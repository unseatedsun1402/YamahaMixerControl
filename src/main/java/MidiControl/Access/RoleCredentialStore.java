package MidiControl.Access;

import MidiControl.Access.Authenticate.Role;
import MidiControl.SystemTools.ConfigDirectoryProvider;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

public final class RoleCredentialStore {

    public record Credential(boolean enabled, String salt, String passwordHash, int iterations) {}

    public static final int PASSWORD_HASH_ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 2_000_000;
    private static final String CONFIG_DIRECTORY = "MidiControl";
    private static final String CONFIG_FILE = "roles.json";
    private static final String CONFIG_PATH_PROPERTY = "midicontrol.roles.file";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RoleCredentialStore() {}

    public static Path defaultPath() {
        String override = System.getProperty(CONFIG_PATH_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        return ConfigDirectoryProvider.getConfigDirectory(CONFIG_DIRECTORY)
                .toPath()
                .resolve(CONFIG_FILE);
    }

    public static Map<Role, Credential> loadDefault() {
        return loadOrCreate(defaultPath());
    }

    public static Map<Role, Credential> loadOrCreate(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        try {
            Path parent = normalizedPath.getParent();
            if (parent == null) {
                throw new IOException("Role credentials file has no parent directory.");
            }
            Files.createDirectories(parent);
            try {
                Files.writeString(
                        normalizedPath,
                        defaultConfiguration(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // Another application instance created the default file first.
            }
            return read(normalizedPath);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Could not load role credentials from " + normalizedPath, exception);
        }
    }

    public static Credential createCredential(char[] password) {
        if (password == null || password.length < 4) {
            throw new IllegalArgumentException("Password must contain at least 4 characters.");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = derivePasswordHash(password, salt, PASSWORD_HASH_ITERATIONS);
        try {
            return new Credential(
                    true,
                    encode(salt),
                    encode(hash),
                    PASSWORD_HASH_ITERATIONS);
        } finally {
            java.util.Arrays.fill(hash, (byte) 0);
            java.util.Arrays.fill(salt, (byte) 0);
        }
    }

    public static byte[] derivePasswordHash(char[] password, byte[] salt, int iterations) {
        if (password == null || salt == null || iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("Invalid PBKDF2 input.");
        }
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, HASH_BYTES * Byte.SIZE);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA-256 is unavailable.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    public static byte[] decodeHash(String encoded, int expectedLength) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            if (decoded.length != expectedLength) {
                throw new IllegalArgumentException("Invalid encoded credential length.");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid base64url credential value.", exception);
        }
    }

    public static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static JsonObject toJson(Credential credential) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", credential.enabled());
        if (credential.enabled()) {
            json.addProperty("salt", credential.salt());
            json.addProperty("passwordHash", credential.passwordHash());
            json.addProperty("iterations", credential.iterations());
        }
        return json;
    }

    private static Map<Role, Credential> read(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (!root.has("version") || root.get("version").getAsInt() != 1
                || !root.has("roles") || !root.get("roles").isJsonObject()) {
            throw new IllegalArgumentException("Role credentials file must use schema version 1.");
        }

        JsonObject roles = root.getAsJsonObject("roles");
        EnumMap<Role, Credential> credentials = new EnumMap<>(Role.class);
        credentials.put(Role.GUEST, new Credential(true, "", "", 0));
        credentials.put(Role.OPERATOR, readCredential(roles, "operator"));
        credentials.put(Role.ADMIN, readCredential(roles, "admin"));
        return Map.copyOf(credentials);
    }

    private static Credential readCredential(JsonObject roles, String roleName) {
        if (!roles.has(roleName) || !roles.get(roleName).isJsonObject()) {
            throw new IllegalArgumentException("Role credentials are missing entry: " + roleName);
        }
        JsonObject role = roles.getAsJsonObject(roleName);
        if (!role.has("enabled") || !role.get("enabled").isJsonPrimitive()
                || !role.getAsJsonPrimitive("enabled").isBoolean()) {
            throw new IllegalArgumentException("Role enabled flag is missing or invalid: " + roleName);
        }
        boolean enabled = role.get("enabled").getAsBoolean();
        if (!enabled) {
            return new Credential(false, "", "", 0);
        }
        if (!role.has("salt") || !role.get("salt").isJsonPrimitive()
                || !role.has("passwordHash") || !role.get("passwordHash").isJsonPrimitive()
                || !role.has("iterations") || !role.get("iterations").isJsonPrimitive()) {
            throw new IllegalArgumentException("Enabled role is missing password hash settings: " + roleName);
        }

        String salt = role.get("salt").getAsString();
        String passwordHash = role.get("passwordHash").getAsString();
        int iterations = role.get("iterations").getAsInt();
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("PBKDF2 iteration count is out of range for " + roleName);
        }
        if (decodeHash(salt, SALT_BYTES).length != SALT_BYTES
                || decodeHash(passwordHash, HASH_BYTES).length != HASH_BYTES) {
            throw new IllegalArgumentException("Invalid password hash settings for " + roleName);
        }
        return new Credential(true, salt, passwordHash, iterations);
    }

    private static String defaultConfiguration() {
        return """
                {
                  "version": 1,
                  "roles": {
                    "guest": {
                      "enabled": true
                    },
                    "operator": {
                      "enabled": false
                    },
                    "admin": {
                      "enabled": false
                    }
                  }
                }
                """;
    }
}
