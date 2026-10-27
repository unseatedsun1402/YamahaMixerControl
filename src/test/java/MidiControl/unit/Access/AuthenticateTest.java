package MidiControl.unit.Access;

import MidiControl.Access.Authenticate;
import MidiControl.Access.Authenticate.Role;
import MidiControl.Access.RoleCredentialStore;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.EnumMap;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticateTest {

    private static final String OPERATOR_PASSWORD = "operator-secret";
    private static final String ADMIN_PASSWORD = "admin-secret";

    private static Authenticate.ClientFingerprint fingerprint() {
        return new Authenticate.ClientFingerprint("stable-salt", "192.168.1.20", "Chrome", "Windows");
    }

    private static Authenticate authenticateWithPasswords(String operatorPassword, String adminPassword) {
        EnumMap<Role, RoleCredentialStore.Credential> credentials = new EnumMap<>(Role.class);
        credentials.put(Role.GUEST, new RoleCredentialStore.Credential(true, "", "", 0));
        credentials.put(Role.OPERATOR, credentialFor(operatorPassword));
        credentials.put(Role.ADMIN, credentialFor(adminPassword));
        return new Authenticate(credentials);
    }

    private static RoleCredentialStore.Credential credentialFor(String password) {
        if (password == null || password.isBlank()) {
            return new RoleCredentialStore.Credential(false, "", "", 0);
        }
        return RoleCredentialStore.createCredential(password.toCharArray());
    }

    private static String passwordProof(
            String password,
            Authenticate.PasswordChallengeResult challenge,
            String salt) throws Exception {
        String message = challenge.challengeId() + "\n"
                + challenge.role().name().toLowerCase() + "\n"
                + salt;
        byte[] verifier = RoleCredentialStore.derivePasswordHash(
                password.toCharArray(),
                RoleCredentialStore.decodeHash(challenge.passwordSalt(), 16),
                challenge.iterations());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(verifier, "HmacSHA256"));
        String proof = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        java.util.Arrays.fill(verifier, (byte) 0);
        return proof;
    }

    private static Authenticate.AuthenticationResult authenticatePassword(
            Authenticate authenticate,
            String role,
            String password,
            String sessionId) throws Exception {
        Authenticate.PasswordChallengeResult challenge =
                authenticate.beginPasswordAuthentication(role, fingerprint().salt(), sessionId);
        if (!challenge.successful()) {
            return new Authenticate.AuthenticationResult(false, null, null, challenge.message());
        }
        return authenticate.completePasswordAuthentication(
                challenge.challengeId(),
                sessionId,
                passwordProof(password, challenge, fingerprint().salt()),
                fingerprint());
    }

    @Test
    void guestCanAuthenticateWithoutPasswordAndResumeWithSavedToken() {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);

        Authenticate.AuthenticationResult result =
                authenticate.authenticateGuest(fingerprint());

        assertTrue(result.successful());
        assertEquals(Role.GUEST, authenticate.roleFor(result.token(), fingerprint()).orElseThrow());

        Authenticate.AuthenticationResult resumed = authenticate.authenticateToken(result.token(), fingerprint());
        assertTrue(resumed.successful());
        assertEquals(Role.GUEST, resumed.role());
        assertEquals(result.token(), resumed.token());
        assertFalse(authenticate.authenticateToken("unknown-token", fingerprint()).successful());
    }

    @Test
    void operatorAndAdminCredentialsAreDistinct() throws Exception {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);

        assertFalse(authenticatePassword(authenticate, "operator", ADMIN_PASSWORD, "wrong-operator").successful());
        assertFalse(authenticatePassword(authenticate, "admin", OPERATOR_PASSWORD, "wrong-admin").successful());
        assertTrue(authenticatePassword(authenticate, "operator", OPERATOR_PASSWORD, "correct-operator").successful());
        assertTrue(authenticatePassword(authenticate, "admin", ADMIN_PASSWORD, "correct-admin").successful());
    }

    @Test
    void operatorCanChangeOnlyMixSendLevels() {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);

        assertTrue(authenticate.isAllowed(
                Role.OPERATOR, "set-control-value", "kInputToMix.kMix1Level.10"));
        assertFalse(authenticate.isAllowed(
                Role.OPERATOR, "set-control-value", "kInputToMix.kMix1Pan.10"));
        assertFalse(authenticate.isAllowed(
                Role.OPERATOR, "set-midi-device", null));
    }

    @Test
    void guestIsReadOnlyAndAdminIsUnrestricted() {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);

        assertTrue(authenticate.isAllowed(Role.GUEST, "get-ui-model", null));
        assertFalse(authenticate.isAllowed(
                Role.GUEST, "set-control-value", "kInputToMix.kMix1Level.10"));
        assertFalse(authenticate.isAllowed(Role.GUEST, "set-midi-device", null));
        assertTrue(authenticate.isAllowed(Role.ADMIN, "set-control-value", "any.control.1"));
        assertTrue(authenticate.isAllowed(Role.ADMIN, "save-midi-settings", null));
    }

    @Test
    void authenticatingAgainDoesNotInvalidatePreviouslyIssuedTokens() {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);
        String guestToken = authenticate.authenticateGuest(fingerprint()).token();
        String operatorToken;
        try {
            operatorToken = authenticatePassword(
                    authenticate, "operator", OPERATOR_PASSWORD, "operator-session").token();
        } catch (Exception exception) {
            throw new AssertionError("Could not authenticate test operator", exception);
        }

        assertEquals(Role.GUEST, authenticate.roleFor(guestToken, fingerprint()).orElseThrow());
        assertEquals(Role.OPERATOR, authenticate.roleFor(operatorToken, fingerprint()).orElseThrow());
    }

    @Test
    void privilegedRolesAreDisabledWhenPasswordsAreNotConfigured() {
        Authenticate authenticate = authenticateWithPasswords(null, "");

        assertTrue(authenticate.authenticateGuest(fingerprint()).successful());
        assertFalse(authenticate.beginPasswordAuthentication("operator", fingerprint().salt(), "disabled-operator")
                .successful());
        assertFalse(authenticate.beginPasswordAuthentication("admin", fingerprint().salt(), "disabled-admin")
                .successful());
    }

    @Test
    void oneFingerprintMismatchIsTolerated() {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);
        String token;
        try {
            token = authenticatePassword(
                    authenticate, "operator", OPERATOR_PASSWORD, "one-mismatch-session").token();
        } catch (Exception exception) {
            throw new AssertionError("Could not authenticate test operator", exception);
        }
        Authenticate.ClientFingerprint changedIp = new Authenticate.ClientFingerprint(
                "stable-salt", "192.168.1.21", "Chrome", "Windows");

        assertEquals(Role.OPERATOR, authenticate.roleFor(token, changedIp).orElseThrow());
    }

    @Test
    void twoFingerprintMismatchesInvalidateToken() {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);
        String token;
        try {
            token = authenticatePassword(
                    authenticate, "operator", OPERATOR_PASSWORD, "two-mismatch-session").token();
        } catch (Exception exception) {
            throw new AssertionError("Could not authenticate test operator", exception);
        }
        Authenticate.ClientFingerprint changedIpAndBrowser = new Authenticate.ClientFingerprint(
                "stable-salt", "192.168.1.21", "Firefox", "Windows");

        assertTrue(authenticate.roleFor(token, changedIpAndBrowser).isEmpty());
        assertTrue(authenticate.roleFor(token, fingerprint()).isEmpty());
    }

    @Test
    void passwordChallengeAuthenticatesWithoutSendingPasswordAndCanOnlyBeUsedOnce() throws Exception {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);
        Authenticate.PasswordChallengeResult challenge =
                authenticate.beginPasswordAuthentication("operator", fingerprint().salt(), "session-1");
        assertTrue(challenge.successful());

        String proof = passwordProof(OPERATOR_PASSWORD, challenge, fingerprint().salt());
        Authenticate.AuthenticationResult result = authenticate.completePasswordAuthentication(
                challenge.challengeId(), "session-1", proof, fingerprint());

        assertTrue(result.successful());
        assertEquals(Role.OPERATOR, result.role());
        assertEquals(Role.OPERATOR, authenticate.roleFor(result.token(), fingerprint()).orElseThrow());
        assertFalse(authenticate.completePasswordAuthentication(
                challenge.challengeId(), "session-1", proof, fingerprint()).successful());
    }

    @Test
    void failedPasswordProofConsumesChallenge() throws Exception {
        Authenticate authenticate = authenticateWithPasswords(OPERATOR_PASSWORD, ADMIN_PASSWORD);
        Authenticate.PasswordChallengeResult challenge =
                authenticate.beginPasswordAuthentication("operator", fingerprint().salt(), "session-2");
        String wrongProof = passwordProof(ADMIN_PASSWORD, challenge, fingerprint().salt());

        assertFalse(authenticate.completePasswordAuthentication(
                challenge.challengeId(), "session-2", wrongProof, fingerprint()).successful());
        assertFalse(authenticate.completePasswordAuthentication(
                challenge.challengeId(),
                "session-2",
                passwordProof(OPERATOR_PASSWORD, challenge, fingerprint().salt()),
                fingerprint()).successful());
    }

    @Test
    void missingOrBlankConfiguredPasswordsDisablePrivilegedChallenges() {
        Authenticate authenticate = authenticateWithPasswords(null, "   ");

        assertTrue(authenticate.isRoleEnabled(Role.GUEST));
        assertFalse(authenticate.isRoleEnabled(Role.OPERATOR));
        assertFalse(authenticate.isRoleEnabled(Role.ADMIN));

        Authenticate.PasswordChallengeResult operatorChallenge =
                authenticate.beginPasswordAuthentication("operator", fingerprint().salt(), "session-3");
        Authenticate.PasswordChallengeResult adminChallenge =
                authenticate.beginPasswordAuthentication("admin", fingerprint().salt(), "session-4");

        assertFalse(operatorChallenge.successful());
        assertFalse(adminChallenge.successful());
        assertTrue(operatorChallenge.message().contains("no password hash is configured"));
        assertTrue(adminChallenge.message().contains("no password hash is configured"));
    }

    @Test
    void passwordHashDerivationMatchesStandardPbkdf2ForUtf8Passwords() throws Exception {
        char[] password = "pässword".toCharArray();
        byte[] salt = "test-salt".getBytes(StandardCharsets.UTF_8);
        int iterations = 100_000;
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        byte[] expected = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        byte[] actual = RoleCredentialStore.derivePasswordHash(password, salt, iterations);

        assertArrayEquals(expected, actual);
        spec.clearPassword();
        java.util.Arrays.fill(password, '\0');
        java.util.Arrays.fill(expected, (byte) 0);
        java.util.Arrays.fill(actual, (byte) 0);
    }
}
