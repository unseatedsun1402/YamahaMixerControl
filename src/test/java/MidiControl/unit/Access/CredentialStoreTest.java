package MidiControl.unit.Access;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Tag;

import MidiControl.Access.Authenticate;
import MidiControl.Access.CredentialStore;

@Tag("unit")
public class CredentialStoreTest {
    @Test
    public void testStoresValidTokenRolePair(){
        String testToken="SomeRandomString";
        Authenticate.Role testRole= Authenticate.Role.GUEST;

        CredentialStore testCredentialStore = new CredentialStore();
        testCredentialStore.storeBearerToken(testToken,testRole);
        assertTrue(testCredentialStore.checkTokenExists(testToken));
    }

    @Test
    public void testValidTokenReturnsRole(){
        String testToken="SomeRandomString";
        Authenticate.Role testRole= Authenticate.Role.GUEST;

        CredentialStore testCredentialStore = new CredentialStore();
        testCredentialStore.storeBearerToken(testToken,testRole);
        assertEquals(testCredentialStore.getTokenRole(testToken).get(),testRole);
    }

    @Test
    public void testCheckTokenMissingReturnsFalse(){
        CredentialStore testCredentialStore = new CredentialStore();
        assertFalse(testCredentialStore.checkTokenExists("thereIsNoToken"));
    }

    @Test
    public void testGetRoleForMissingTokenReturnsEmpty(){
        CredentialStore testCredentialStore = new CredentialStore();
        assertThrows(NoSuchElementException.class,() -> testCredentialStore.getTokenRole("thereIsNoToken").get());
    }
}
