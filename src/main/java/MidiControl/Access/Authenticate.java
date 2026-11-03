package MidiControl.Access;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

public class Authenticate {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final CredentialStore credentialManager = new CredentialStore();

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

    private static String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
