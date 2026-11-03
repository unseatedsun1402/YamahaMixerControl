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
}
