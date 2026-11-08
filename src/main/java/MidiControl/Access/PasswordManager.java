package MidiControl.Access;

import java.io.IOException;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

import MidiControl.SystemTools.ConfigDirectoryProvider;

public class PasswordManager {
    private static final String SETTINGS_DIR = "MidiControl";
    private static final String PASSWORD_FILE = "system-password.hash";
    private static final Logger logger = Logger.getLogger(PasswordManager.class.getName());

    public enum PasswordCheckResult {
        SUCCESS,
        FAILURE,
        FILE_DOES_NOT_EXIST
    }

    public static void storePassword(String password) throws IOException {
        storePassword(getPasswordFile(), password);
    }

    public static void storePassword(Path passwordFile, String password) throws IOException {
        Objects.requireNonNull(password, "password");
        Path absoluteFile = passwordFile.toAbsolutePath();
        Files.createDirectories(absoluteFile.getParent());
        Files.writeString(passwordFile, hashPassword(password), StandardCharsets.UTF_8);
        logger.info("System password stored at: " + absoluteFile);
    }

    public static PasswordCheckResult verifyPassword(String password) throws IOException {
        return verifyPassword(getPasswordFile(), password);
    }

    public static PasswordCheckResult verifyPassword(Path passwordFile, String password) throws IOException {
        Objects.requireNonNull(password, "password");
        return verifyPasswordHash(passwordFile, hashPassword(password));
    }

    public static PasswordCheckResult verifyPasswordHash(String passwordHash) throws IOException {
        return verifyPasswordHash(getPasswordFile(), passwordHash);
    }

    public static PasswordCheckResult verifyPasswordHash(Path passwordFile, String passwordHash) throws IOException {
        Objects.requireNonNull(passwordHash, "passwordHash");
        String storedHash;
        try {
            storedHash = Files.readString(passwordFile, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return PasswordCheckResult.FILE_DOES_NOT_EXIST;
        }

        return MessageDigest.isEqual(
                passwordHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8))
                ? PasswordCheckResult.SUCCESS
                : PasswordCheckResult.FAILURE;
    }

    public static Optional<String> getStoredPasswordHash() throws IOException {
        String storedHash;
        try {
            storedHash = Files.readString(getPasswordFile(), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
        if (!storedHash.matches("[0-9a-f]{64}")) {
            throw new IOException("Stored system password hash is invalid");
        }
        return Optional.of(storedHash);
    }

    private static Path getPasswordFile() {
        return ConfigDirectoryProvider.getConfigDirectory(SETTINGS_DIR).toPath().resolve(PASSWORD_FILE);
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
