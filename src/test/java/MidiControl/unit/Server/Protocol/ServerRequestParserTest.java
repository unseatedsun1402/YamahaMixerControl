package MidiControl.unit.Server.Protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import MidiControl.Server.Protocol.ServerRequest;
import MidiControl.Server.Protocol.ServerRequestParser;

public class ServerRequestParserTest {
    
    @Test
    public void testServerRequestParser(){
        String testType = "\"testType\"";
        String testPayload = "{\"testPayload\":\"testPayload\"}";

        String testJsonString = "{\"type\":"+testType+",\"payload\":"+testPayload+"}";

        ServerRequest result = new ServerRequestParser(new Gson()).parse(testJsonString);
        String wrappedResType = "\""+result.type()+"\"";
        String wrappedResPayload = ""+result.payload();
        assertEquals(testType, wrappedResType);
        assertEquals(testPayload, wrappedResPayload);
    }

    @Test
    public void testServerRequestParserRejectsBadType(){ // this should not return but throw an exception perhaps or take a callback
        String testType = "\"testType\"";
        String testPayload = "{}";

        String testJsonString = "{\"typo\":"+testType+",\"payload\":\""+testPayload+"\"}";

        ServerRequest result = new ServerRequestParser(new Gson()).parse(testJsonString);
        assertEquals("", result.type());
    }
}
