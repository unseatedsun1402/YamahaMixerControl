package MidiControl.UserInterface.DTO;

public class ControlUpdateDTO {
    public String canonicalId;
    public int value;

    public String toJson() {
        return "{\"type\":\"control-update\",\"payload\":{"
            + "\"canonicalId\":\"" + canonicalId + "\","
            + "\"value\":" + value
            + "}}";
    }
}