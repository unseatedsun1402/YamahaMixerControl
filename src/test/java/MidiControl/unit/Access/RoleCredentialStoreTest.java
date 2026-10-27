package MidiControl.unit.Access;

import MidiControl.Access.Authenticate.Role;
import MidiControl.Access.RoleCredentialStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoleCredentialStoreTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsDefaultFileWithOnlyGuestEnabled() {
        Path credentialsFile = tempDirectory.resolve("roles.json");

        Map<Role, RoleCredentialStore.Credential> credentials =
                RoleCredentialStore.loadOrCreate(credentialsFile);

        assertTrue(Files.exists(credentialsFile));
        assertTrue(credentials.get(Role.GUEST).enabled());
        assertFalse(credentials.get(Role.OPERATOR).enabled());
        assertFalse(credentials.get(Role.ADMIN).enabled());
    }

    @Test
    void loadsEnabledRolePasswordVerifier() throws Exception {
        RoleCredentialStore.Credential operator =
                RoleCredentialStore.createCredential("operator-passphrase".toCharArray());
        Path credentialsFile = tempDirectory.resolve("roles.json");
        Files.writeString(credentialsFile, """
                {
                  "version": 1,
                  "roles": {
                    "guest": {"enabled": true},
                    "operator": %s,
                    "admin": {"enabled": false}
                  }
                }
                """.formatted(RoleCredentialStore.toJson(operator)));

        Map<Role, RoleCredentialStore.Credential> credentials =
                RoleCredentialStore.loadOrCreate(credentialsFile);

        assertTrue(credentials.get(Role.OPERATOR).enabled());
        assertEquals(operator.salt(), credentials.get(Role.OPERATOR).salt());
        assertEquals(operator.passwordHash(), credentials.get(Role.OPERATOR).passwordHash());
        assertFalse(credentials.get(Role.ADMIN).enabled());
    }

    @Test
    void acceptsFourCharacterPasswordsAndRejectsShorterOnes() {
        RoleCredentialStore.Credential credential =
                RoleCredentialStore.createCredential("1234".toCharArray());

        assertTrue(credential.enabled());
        assertThrows(
                IllegalArgumentException.class,
                () -> RoleCredentialStore.createCredential("123".toCharArray()));
    }

    @Test
    void rejectsMalformedCredentialsFileInsteadOfDisablingRolesSilently() throws Exception {
        Path credentialsFile = tempDirectory.resolve("roles.json");
        Files.writeString(credentialsFile, "{ invalid");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> RoleCredentialStore.loadOrCreate(credentialsFile));
        assertTrue(exception.getMessage().contains(credentialsFile.toString()));
    }
}
