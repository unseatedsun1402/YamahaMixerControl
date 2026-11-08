package MidiControl.Access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonObject;

import jakarta.websocket.Session;

public class Authenticate {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    public static String createSalt() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static Optional<byte[]> generateNewToken(String salt, String base_input) {
        String hashInput = String.format("%s%s", salt == null ? "" : salt, base_input == null ? "" : base_input);

        try {
            MessageDigest hashMethod = MessageDigest.getInstance("SHA-256");
            return Optional.of(hashMethod.digest(hashInput.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return Optional.empty();
        }
    }

    public static String generateChallengeToken(String salt, String passwordHash, String sessionId) {
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(sessionId, "sessionId");

        byte[] tokenBytes = generateNewToken(salt, passwordHash + sessionId)
                .orElseThrow(() -> new IllegalStateException("SHA-256 is unavailable"));
        StringBuilder token = new StringBuilder(tokenBytes.length * 2);
        for (byte value : tokenBytes) {
            token.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        }
        return token.toString();
    }

    public static boolean verifyChallengeToken(
            String salt,
            String passwordHash,
            String sessionId,
            String suppliedToken) {
        if (suppliedToken == null || !suppliedToken.matches("[0-9a-f]{64}")) {
            return false;
        }
        String expectedToken = generateChallengeToken(salt, passwordHash, sessionId);
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.US_ASCII),
                suppliedToken.getBytes(StandardCharsets.US_ASCII));
    }

    public static AuthenticationResult authenticateToken(CredentialStore directory, String token){
        Optional<Authenticate.Role> maybeToken = directory.getTokenRole(token);
        if(maybeToken.isPresent()){
            return AuthenticationResult.success(maybeToken.get(),token);
        }
        return AuthenticationResult.failure("Token validation failed: reason unknown");
    }

    public static String createCredentialsAndStore(CredentialStore directory, Session session, Role role) {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(role, "role");

        JsonObject metadata = buildMetadata(session, createSalt());
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        directory.storeBearerToken(token, role, metadata);
        return token;
    }

    public static JsonObject buildMetadata(Session session, String salt) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(salt, "salt");

        JsonObject metadata = new JsonObject();
        Map<String, Object> properties = session.getUserProperties();
        String userAgent = readProperty(properties, "userAgent");

        metadata.addProperty("salt", salt);
        metadata.addProperty("sessionId", session.getId());
        metadata.addProperty("clientIp", readProperty(properties, "clientIp"));
        metadata.addProperty("browser", detectBrowser(userAgent));
        metadata.addProperty("os", detectOperatingSystem(userAgent));

        return metadata;
    }

    private static String readProperty(Map<String, Object> properties, String key) {
        Object value = properties == null ? null : properties.get(key);
        if (value == null) {
            return "unknown";
        }

        String text = value.toString().trim();
        return text.isEmpty() ? "unknown" : text;
    }

    private static String detectBrowser(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("edg/")) {
            return "Edge";
        }
        if (normalized.contains("opr/")) {
            return "Opera";
        }
        if (normalized.contains("samsungbrowser/")) {
            return "Samsung Internet";
        }
        if (normalized.contains("vivaldi/")) {
            return "Vivaldi";
        }
        if (normalized.contains("chrome/") || normalized.contains("crios/")) {
            return "Chrome";
        }
        if (normalized.contains("firefox/") || normalized.contains("fxios/")) {
            return "Firefox";
        }
        if (normalized.contains("safari/")) {
            return "Safari";
        }
        return "unknown";
    }

    private static String detectOperatingSystem(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("cros ")) {
            return "ChromeOS";
        }
        if (normalized.contains("android")) {
            return "Android";
        }
        if (normalized.contains("iphone") || normalized.contains("ipad")
                || normalized.contains("ipod") || normalized.contains("cpu iphone")) {
            return "iOS";
        }
        if (normalized.contains("windows")) {
            return "Windows";
        }
        if (normalized.contains("mac os") || normalized.contains("macintosh")) {
            return "macOS";
        }
        if (normalized.contains("linux")) {
            return "Linux";
        }
        return "unknown";
    }

    private static boolean checkTrusted(JsonObject metadata, Session session) {
        String tokenbrowser = metadata.has("browser") ? metadata.get("browser").getAsString() : "unknown";
        String tokenos = metadata.has("os") ? metadata.get("os").getAsString() : "unknown";
        String tokenIpAddress = metadata.has("clientIp") ? metadata.get("clientIp").getAsString() : "unknown";
        String tokensessionId = metadata.has("sessionId") ? metadata.get("sessionId").getAsString() : "unknown";

        Map<String, Object> properties = session.getUserProperties();
        String userAgent = readProperty(properties, "userAgent");

        String sessionIpAddress = readProperty(properties, "clientIp");
        String sessionos = detectOperatingSystem(userAgent);
        String sessionbrowser = detectBrowser(userAgent);

        int score = 0;

        score += isKnownMatch(session.getId(), tokensessionId) ? 10 : 0;
        score += isKnownMatch(sessionIpAddress, tokenIpAddress) ? 10 : 0;
        score += isKnownMatch(sessionos, tokenos) ? 10 : 0;
        score += isKnownMatch(sessionbrowser, tokenbrowser) ? 10 : 0;

        return score >= 20;
    }

    private static boolean isKnownMatch(String sessionValue, String tokenValue) {
        return !"unknown".equals(sessionValue)
                && !"unknown".equals(tokenValue)
                && Objects.equals(sessionValue, tokenValue);
    }
}
