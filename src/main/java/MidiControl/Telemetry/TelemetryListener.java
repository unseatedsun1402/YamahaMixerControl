package MidiControl.Telemetry;

import com.google.gson.JsonObject;

public interface TelemetryListener {
    void onTelemetry(String jsonString);
    void onJson(JsonObject json);

    
    TelemetryListener NO_OP = new TelemetryListener() {
        @Override public void onTelemetry(String json) {}
        @Override public void onJson(JsonObject json) {}
    };

}