package MidiControl.Access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

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

    public static AuthenticationResult authenticateToken(CredentialStore directory, String token){
        Optional<Authenticate.Role> maybeToken = directory.getTokenRole(token);
        if(maybeToken.isPresent()){
            return AuthenticationResult.success(maybeToken.get(),token);
        }
        return AuthenticationResult.failure("Token validation failed: reason unknown");
    }
}
