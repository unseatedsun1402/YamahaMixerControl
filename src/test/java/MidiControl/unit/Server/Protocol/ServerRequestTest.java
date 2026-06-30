package MidiControl.unit.Server.Protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import MidiControl.Server.Protocol.ServerRequest;

public class ServerRequestTest {
    @Test
    void testEmptyPayloadRequest(){
        ServerRequest testRequest = ServerRequest.of("test_type","test_reqId",null);
        assertEquals("test_type",testRequest.type());
        assertEquals("test_reqId",testRequest.requestId());
        assertTrue(testRequest.payload().isJsonObject());
    }

    @Test
    void testEmptyRequestId(){
        ServerRequest testRequest = ServerRequest.of("test_type",null,new JsonObject());
        assertEquals("",testRequest.requestId());
        assertEquals("test_type",testRequest.type());
        assertTrue(testRequest.payload().isJsonObject());
    }

    @Test
    void testEmptyType(){
        ServerRequest testRequest = ServerRequest.of(null,"test_reqId",new JsonObject());
        assertEquals("test_reqId",testRequest.requestId());
        assertEquals("",testRequest.type());
        assertTrue(testRequest.payload().isJsonObject());
    }

    @Test
    void testGoodServerRequestRecord(){
        JsonObject testObject = new JsonObject();
        ServerRequest testRequest = ServerRequest.of("test_type","test_reqId",testObject);
        assertEquals("test_reqId",testRequest.requestId());
        assertEquals("test_type",testRequest.type());
        assertEquals(testObject, testRequest.payload());
    }

    @Test
    void testHasType(){
        ServerRequest testRequest = ServerRequest.of("test_type",null,null);
        assertTrue(testRequest.hasType());
    }

    @Test
    void testHasTypeReturnsFalseForNoType(){
        ServerRequest testRequest = ServerRequest.of(null,null,null);
        assertFalse(testRequest.hasType());
    }
}
