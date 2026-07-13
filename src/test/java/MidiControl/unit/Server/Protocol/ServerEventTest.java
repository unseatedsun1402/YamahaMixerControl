package MidiControl.unit.Server.Protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import MidiControl.Server.Protocol.ServerEvent;
import MidiControl.Server.Protocol.ServerEventLevel;

public class ServerEventTest {
    @Test
    public void CreateInfoServerEventTest(){
        String testMessage = "Hello world";
        String testCategory= "TestCat";
        ServerEvent actual = ServerEvent.info(testCategory, testMessage);
        assertEquals(testCategory, actual.category());
        assertEquals(testMessage, actual.message());
        assertEquals(ServerEventLevel.INFO, actual.level());
    }

    @Test
    public void CreateErrorServerEventTest(){
        String testMessage = "Hello world";
        String testCategory= "TestCat";
        ServerEvent actual = ServerEvent.error(testCategory, testMessage);
        assertEquals(testCategory, actual.category());
        assertEquals(testMessage, actual.message());
        assertEquals(ServerEventLevel.ERROR, actual.level());
    }

    @Test
    public void CreateWarningServerEventTest(){
        String testMessage = "Hello world";
        String testCategory= "TestCat";
        ServerEvent actual = ServerEvent.warning(testCategory, testMessage);
        assertEquals(testCategory, actual.category());
        assertEquals(testMessage, actual.message());
        assertEquals(ServerEventLevel.WARN, actual.level());
    }

    @Test
    public void CreateInfoServerEventGdJson(){
        String testMessage = "Hello world";
        String testCategory= "TestCat";
        JsonObject testDetailsJson= new Gson().fromJson("{\"type\":\"test\"}",JsonObject.class);
        ServerEvent actual = ServerEvent.info(testCategory, testMessage, testDetailsJson);
        assertEquals(testCategory, actual.category());
        assertEquals(testMessage, actual.message());
        assertEquals(ServerEventLevel.INFO, actual.level());
        assertEquals(testDetailsJson, actual.details());
    }

    @Test
    public void CreateErrorServerEventGdJson(){
        String testMessage = "Hello world";
        String testCategory= "TestCat";
        JsonObject testDetailsJson= new Gson().fromJson("{\"type\":\"test\"}",JsonObject.class);
        ServerEvent actual = ServerEvent.error(testCategory, testMessage, testDetailsJson);
        assertEquals(testCategory, actual.category());
        assertEquals(testMessage, actual.message());
        assertEquals(ServerEventLevel.ERROR, actual.level());
        assertEquals(testDetailsJson, actual.details());
    }

    @Test
    public void CreateWarningServerEventGdJson(){
        String testMessage = "Hello world";
        String testCategory= "TestCat";
        JsonObject testDetailsJson= new Gson().fromJson("{\"type\":\"test\"}",JsonObject.class);
        ServerEvent actual = ServerEvent.warning(testCategory, testMessage, testDetailsJson);
        assertEquals(testCategory, actual.category());
        assertEquals(testMessage, actual.message());
        assertEquals(ServerEventLevel.WARN, actual.level());
        assertEquals(testDetailsJson, actual.details());
    }
}
