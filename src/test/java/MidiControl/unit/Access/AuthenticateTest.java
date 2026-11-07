package MidiControl.unit.Access;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import MidiControl.Access.Authenticate;
import MidiControl.Access.CredentialStore;
import jakarta.websocket.Session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
public class AuthenticateTest {
    @Test
    public void testBuildMetadataIncludesClientAndParsedUserAgent() {
        Session session = mock(Session.class);
        Map<String, Object> properties = new HashMap<>();
        properties.put("clientIp", "192.0.2.10");
        properties.put("userAgent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0 Edg/120.0");
        when(session.getId()).thenReturn("session-123");
        when(session.getUserProperties()).thenReturn(properties);

        JsonObject metadata = Authenticate.buildMetadata(session, "test-salt");

        assertEquals("test-salt", metadata.get("salt").getAsString());
        assertEquals("session-123", metadata.get("sessionId").getAsString());
        assertEquals("192.0.2.10", metadata.get("clientIp").getAsString());
        assertEquals("Edge", metadata.get("browser").getAsString());
        assertEquals("Windows", metadata.get("os").getAsString());
    }

    @Test
    public void testBuildMetadataUsesUnknownForUnavailableClientData() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-123");
        when(session.getUserProperties()).thenReturn(Map.of());

        JsonObject metadata = Authenticate.buildMetadata(session, "test-salt");

        assertEquals("unknown", metadata.get("clientIp").getAsString());
        assertEquals("unknown", metadata.get("browser").getAsString());
        assertEquals("unknown", metadata.get("os").getAsString());
    }

    @Test
    public void testCreatedTokenIsStoredWithMetadata() {
        CredentialStore directory = new CredentialStore();
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-123");
        when(session.getUserProperties()).thenReturn(Map.of("clientIp", "192.0.2.10"));

        String token = Authenticate.createCredentialsAndStore(directory, session, Authenticate.Role.GUEST);

        assertTrue(directory.checkTokenExists(token));
        assertEquals(Authenticate.Role.GUEST, directory.getTokenRole(token).orElseThrow());
        assertEquals("session-123", directory.getTokenMetadata(token).orElseThrow()
                .get("sessionId").getAsString());
    }

    @Test
    public void testReturnSuccessfulAuthentication(){
        Authenticate.AuthenticationResult testResult = new Authenticate.AuthenticationResult(
            true,
            Authenticate.Role.GUEST,"testToken",
            "success"
        );

        assertTrue(testResult.successful());
    }

    @Test
    public void testReturnUnsuccessfulAuthentication(){
        Authenticate.AuthenticationResult testResult = new Authenticate.AuthenticationResult(
            false,
            Authenticate.Role.GUEST,"testToken",
            "success"
        );

        assertFalse(testResult.successful());
    }

    @Test
    public void testReturnSuccessfulAuthenticationRole(){
        Authenticate.AuthenticationResult testResult = new Authenticate.AuthenticationResult(
            true,
            Authenticate.Role.GUEST,"testToken",
            "success"
        );

        assertEquals(testResult.role(),Authenticate.Role.GUEST);
    }

    @Test
    public void testReturnSuccessfulAuthenticationMessage(){
        String testMessage = "success";
        Authenticate.AuthenticationResult testResult = new Authenticate.AuthenticationResult(
            true,
            Authenticate.Role.GUEST,"testToken",
            testMessage
        );

        assertEquals(testResult.message(),testMessage);
    }

    @Test
    public void testReturnUnuccessfulAuthenticationMessage(){
        String testMessage = "failure";
        Authenticate.AuthenticationResult testResult = new Authenticate.AuthenticationResult(
            false,
            null, null,
            testMessage
        );

        assertFalse(testResult.successful());
        assertEquals(testMessage, testResult.message());
        assertEquals(null,testResult.role());
    }

    @Test
    public void testGenerateTokenReturnsBytes() throws NoSuchAlgorithmException {
        String baseInput = "";
        Optional<byte[]> testBytes = Authenticate.generateNewToken(baseInput, baseInput);

        var actual = testBytes.orElseThrow();
        byte[] expectedBytes = MessageDigest.getInstance("SHA-256").digest(new byte[0]);

        assertTrue(actual != null);
        assertEquals(32, actual.length);
        assertArrayEquals(expectedBytes, actual);
    }

    @Test
    public void testGenerateSalt() throws NoSuchAlgorithmException {
        String testSalt = "";
        int base64paddedsize = 43;

        testSalt = Authenticate.createSalt();

        assertTrue(testSalt != "");
        assertEquals(testSalt.length(),base64paddedsize);
    }

    @Test
    public void testAuthenticateSuccess() {
        CredentialStore testDirectory = new CredentialStore();
        String testToken = "test token";
        Authenticate.Role testRole = Authenticate.Role.GUEST;
        
        testDirectory.storeBearerToken(testToken,testRole);
        assertTrue(Authenticate.authenticateToken(testDirectory,testToken).successful());
    }

    @Test
    public void testEmptyDirectoryReturnsFailureOnAuthenticate() {
        CredentialStore testDirectory = new CredentialStore();
        String testToken = "test token";
        
        assertFalse(Authenticate.authenticateToken(testDirectory,testToken).successful());
    }
}
