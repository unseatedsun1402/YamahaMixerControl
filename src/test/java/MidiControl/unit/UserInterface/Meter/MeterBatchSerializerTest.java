package MidiControl.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class MeterBatchSerializerTest {

    @Test
    void serializesMeterUpdatesAsOneBatchEnvelope() {
        MeterDTO update = new MeterDTO(
                127,
                15,
                -9300,
                (byte) 0x1A,
                MeterCategory.INPUT,
                MeterSource.CHANNEL,
                true,
                123456789L);

        MeterDTO nextUpdate = new MeterDTO(
                64,
                16,
                -4000,
                (byte) 0x1A,
                MeterCategory.INPUT,
                MeterSource.CHANNEL,
                true,
                123456790L);

        var message = JsonParser.parseString(
                MeterBatchSerializer.toJson(List.of(update, nextUpdate))).getAsJsonObject();

        assertEquals("meter-batch", message.get("type").getAsString());
        var updates = message.getAsJsonObject("payload").getAsJsonArray("updates");
        assertEquals(2, updates.size());
        assertEquals("INPUT", updates.get(0).getAsJsonObject().get("category").getAsString());
        assertEquals(15, updates.get(0).getAsJsonObject().get("offset").getAsInt());
        assertEquals("true", updates.get(0).getAsJsonObject().get("is14Bit").getAsString());
        assertEquals(123456789L, updates.get(0).getAsJsonObject().get("ts").getAsLong());
        assertEquals(16, updates.get(1).getAsJsonObject().get("offset").getAsInt());
        assertEquals("-93.00", updates.get(0).getAsJsonObject().get("dB").getAsString());
    }
}
