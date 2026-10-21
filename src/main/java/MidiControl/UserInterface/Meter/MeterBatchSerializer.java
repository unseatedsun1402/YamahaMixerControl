package MidiControl.UserInterface.Meter;

import java.util.List;

final class MeterBatchSerializer {

    private MeterBatchSerializer() {}

    static String toJson(List<MeterDTO> updates) {
        StringBuilder json = new StringBuilder(32 + updates.size() * 96);
        json.append("{\"type\":\"meter-batch\",\"payload\":{\"updates\":[");

        for (int i = 0; i < updates.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            updates.get(i).appendPayloadJson(json);
        }

        return json.append("]}}").toString();
    }
}
