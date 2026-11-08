package MidiControl.unit.Access;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import MidiControl.Access.PasswordManager;
import MidiControl.Access.PasswordManager.PasswordCheckResult;

class PasswordManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsMissingFileWhenNoPasswordHasBeenStored() throws IOException {
        Path passwordFile = temporaryDirectory.resolve("system-password.hash");

        assertEquals(PasswordCheckResult.FILE_DOES_NOT_EXIST,
                PasswordManager.verifyPasswordHash(passwordFile, "some-hash"));
    }

    @Test
    void storesOnlyThePasswordHashAndComparesHashes() throws IOException, NoSuchAlgorithmException {
        Path passwordFile = temporaryDirectory.resolve("system-password.hash");
        String password = "correct horse battery staple";
        String passwordHash = sha256(password);

        PasswordManager.storePassword(passwordFile, password);

        assertEquals(passwordHash, Files.readString(passwordFile, StandardCharsets.UTF_8));
        assertEquals(PasswordCheckResult.SUCCESS,
                PasswordManager.verifyPasswordHash(passwordFile, passwordHash));
        assertEquals(PasswordCheckResult.FAILURE,
                PasswordManager.verifyPasswordHash(passwordFile, sha256("wrong password")));
    }

    @Test
    void verifiesPasswordAgainstStoredHash() throws IOException {
        Path passwordFile = temporaryDirectory.resolve("system-password.hash");
        PasswordManager.storePassword(passwordFile, "stored password");

        assertEquals(PasswordCheckResult.SUCCESS,
                PasswordManager.verifyPassword(passwordFile, "stored password"));
    }

    private static String sha256(String password) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte value : hash) {
            hexString.append(String.format("%02x", value & 0xff));
        }
        return hexString.toString();
    }
}
