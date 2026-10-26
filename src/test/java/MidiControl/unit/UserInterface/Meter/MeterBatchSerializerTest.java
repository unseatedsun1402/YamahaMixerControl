package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import MidiControl.UserInterface.Meter.MeterBatchSerializer;
import MidiControl.UserInterface.Meter.MeterCategory;
import MidiControl.UserInterface.Meter.MeterDTO;
import MidiControl.UserInterface.Meter.MeterSource;

class MeterBatchSerializerTest {

    private final Gson gson = new Gson();

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

        JsonObject message = gson.fromJson(
                MeterBatchSerializer.toJson(List.of(update, nextUpdate)), JsonObject.class);

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
