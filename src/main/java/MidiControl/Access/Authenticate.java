package MidiControl.Access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Authenticate {

    private static final Logger logger = Logger.getLogger(Authenticate.class.getName());

    public record ClientFingerprint(String salt, String ipAddress, String browser, String operatingSystem) {
        public boolean isComplete() {
            return hasText(salt) && hasText(ipAddress) && hasText(browser) && hasText(operatingSystem);
        }
    }

    public enum Role {
        GUEST,
        OPERATOR,
        ADMIN;

        public static Optional<Role> parse(String value) {
            if (value == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(Role.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }
    }

    public record AuthenticationResult(boolean successful, Role role, String token, String message) {
        private static AuthenticationResult success(Role role, String token) {
            return new AuthenticationResult(true, role, token, "");
        }

        private static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, null, null, message);
        }
    }

    public record PasswordChallengeResult(
            boolean successful,
            Role role,
            String challengeId,
            String passwordSalt,
            int iterations,
            String message) {
        private static PasswordChallengeResult challenge(
                Role role,
                String challengeId,
                String passwordSalt,
                int iterations) {
            return new PasswordChallengeResult(true, role, challengeId, passwordSalt, iterations, "");
        }

        private static PasswordChallengeResult failure(String message) {
            return new PasswordChallengeResult(false, null, null, null, 0, message);
        }
    }

    private record PendingPasswordChallenge(
            String challengeId,
            Role role,
            String fingerprintSalt,
            RoleCredentialStore.Credential credential,
            long expiresAt) {}

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long PASSWORD_CHALLENGE_LIFETIME_MS = 60_000;
    private static final Pattern MIX_SEND_ID_PATTERN =
            Pattern.compile("^kInputToMix\\.kMix\\d+Level\\.\\d+$");

    private final Map<Role, RoleCredentialStore.Credential> credentials;
    private record AuthenticatedClient(Role role, ClientFingerprint fingerprint) {}

    private final Map<String, AuthenticatedClient> clientsByToken = new ConcurrentHashMap<>();
    private final Map<String, PendingPasswordChallenge> passwordChallengesBySession =
            new ConcurrentHashMap<>();

    public Authenticate() {
        this(RoleCredentialStore.loadDefault());
    }

    public Authenticate(Map<Role, RoleCredentialStore.Credential> credentials) {
        this.credentials = Map.copyOf(credentials);
    }

    public AuthenticationResult authenticateGuest(ClientFingerprint fingerprint) {
        if (fingerprint == null || !fingerprint.isComplete()) {
            return AuthenticationResult.failure("Required client metadata is unavailable.");
        }

        String token = createToken();
        clientsByToken.put(token, new AuthenticatedClient(Role.GUEST, fingerprint));
        return AuthenticationResult.success(Role.GUEST, token);
    }

    public PasswordChallengeResult beginPasswordAuthentication(
            String roleName,
            String fingerprintSalt,
            String sessionId) {
        Optional<Role> requestedRole = Role.parse(roleName);
        if (requestedRole.isEmpty() || requestedRole.get() == Role.GUEST) {
            return PasswordChallengeResult.failure("Select a valid password-protected role.");
        }
        if (!hasText(fingerprintSalt) || !hasText(sessionId)) {
            return PasswordChallengeResult.failure("Required client metadata is unavailable.");
        }

        Role role = requestedRole.get();
        RoleCredentialStore.Credential credential = credentials.get(role);
        if (credential == null || !credential.enabled()) {
            return PasswordChallengeResult.failure(
                    "This role is disabled because no password hash is configured.");
        }

        String challengeId = createToken();
        long now = System.currentTimeMillis();
        passwordChallengesBySession.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        PendingPasswordChallenge challenge = new PendingPasswordChallenge(
                challengeId,
                role,
                fingerprintSalt,
                credential,
                now + PASSWORD_CHALLENGE_LIFETIME_MS);
        passwordChallengesBySession.put(sessionId, challenge);
        return PasswordChallengeResult.challenge(
                role, challengeId, credential.salt(), credential.iterations());
    }

    public boolean isRoleEnabled(Role role) {
        if (role == Role.GUEST) {
            return true;
        }
        RoleCredentialStore.Credential credential = credentials.get(role);
        return credential != null && credential.enabled();
    }

    public AuthenticationResult completePasswordAuthentication(
            String challengeId,
            String sessionId,
            String proof,
            ClientFingerprint fingerprint) {
        if (!hasText(sessionId)) {
            return AuthenticationResult.failure("The role or password is invalid.");
        }
        PendingPasswordChallenge challenge = passwordChallengesBySession.remove(sessionId);
        if (challenge == null
                || !challenge.challengeId().equals(challengeId)
                || System.currentTimeMillis() > challenge.expiresAt()
                || fingerprint == null
                || !fingerprint.isComplete()
                || !challenge.fingerprintSalt().equals(fingerprint.salt())) {
            return AuthenticationResult.failure("The role or password is invalid.");
        }

        if (!matchesPasswordProof(challenge.credential(), challenge, proof)) {
            return AuthenticationResult.failure("The role or password is invalid.");
        }

        String token = createToken();
        clientsByToken.put(token, new AuthenticatedClient(challenge.role(), fingerprint));
        return AuthenticationResult.success(challenge.role(), token);
    }

    public AuthenticationResult authenticateToken(String token, ClientFingerprint fingerprint) {
        Optional<Role> role = roleFor(token, fingerprint);
        if (role.isEmpty()) {
            return AuthenticationResult.failure("The saved authentication token is invalid.");
        }
        return AuthenticationResult.success(role.get(), token);
    }

    public Optional<Role> roleFor(String token, ClientFingerprint fingerprint) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        AuthenticatedClient client = clientsByToken.get(token);
        if (client == null) {
            return Optional.empty();
        }

        int failedChecks = countFingerprintMismatches(client.fingerprint(), fingerprint);
        if (failedChecks >= 2) {
            clientsByToken.remove(token, client);
            logger.warning("Authentication token invalidated after multiple client metadata checks failed");
            return Optional.empty();
        }
        return Optional.of(client.role());
    }

    public boolean isAllowed(Role role, String messageType, String canonicalId) {
        if (role == Role.ADMIN || isReadOnlyRequest(messageType)) {
            return true;
        }
        return role == Role.OPERATOR
                && "set-control-value".equals(messageType)
                && canonicalId != null
                && MIX_SEND_ID_PATTERN.matcher(canonicalId).matches();
    }

    private static boolean isReadOnlyRequest(String messageType) {
        return switch (messageType) {
            case "get-ui-model",
                    "subscribe-context",
                    "unsubscribe-context",
                    "list-midi-devices",
                    "meter-keep-alive",
                    "request-channel-names",
                    "register-session" -> true;
            default -> false;
        };
    }

    private static int countFingerprintMismatches(
            ClientFingerprint expected,
            ClientFingerprint supplied) {
        if (supplied == null) {
            return 4;
        }

        int failures = 0;
        if (!constantTimeEquals(expected.salt(), supplied.salt())) failures++;
        if (!expected.ipAddress().equals(supplied.ipAddress())) failures++;
        if (!expected.browser().equals(supplied.browser())) failures++;
        if (!expected.operatingSystem().equals(supplied.operatingSystem())) failures++;
        return failures;
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        if (expected == null || supplied == null) {
            return expected == supplied;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean matchesPasswordProof(
            RoleCredentialStore.Credential credential,
            PendingPasswordChallenge challenge,
            String suppliedProof) {
        if (!hasText(suppliedProof)) {
            return false;
        }
        byte[] verifier = null;
        byte[] expected = null;
        byte[] supplied = null;
        try {
            verifier = RoleCredentialStore.decodeHash(credential.passwordHash(), 32);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(verifier, "HmacSHA256"));
            expected = mac.doFinal(passwordChallengeMessage(challenge));
            supplied = Base64.getUrlDecoder().decode(suppliedProof);
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            return false;
        } finally {
            if (verifier != null) java.util.Arrays.fill(verifier, (byte) 0);
            if (expected != null) java.util.Arrays.fill(expected, (byte) 0);
            if (supplied != null) java.util.Arrays.fill(supplied, (byte) 0);
        }
    }

    private static byte[] passwordChallengeMessage(PendingPasswordChallenge challenge) {
        String role = challenge.role().name().toLowerCase(Locale.ROOT);
        String message = challenge.challengeId() + "\n"
                + role + "\n"
                + challenge.fingerprintSalt();
        return message.getBytes(StandardCharsets.UTF_8);
    }

    private static String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
