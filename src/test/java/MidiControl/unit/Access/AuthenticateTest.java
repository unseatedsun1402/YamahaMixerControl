package MidiControl.unit.Access;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.Access.Authenticate;

@Tag("unit")
public class AuthenticateTest {
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
}
