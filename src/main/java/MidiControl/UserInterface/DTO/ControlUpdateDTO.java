package MidiControl.UserInterface.DTO;

public class ControlUpdateDTO {
    public String canonicalId;
    public int value;
    public int min;
    public int max;

    public String toJson() {
        return "{\"type\":\"control-update\",\"payload\":{"
            + "\"canonicalId\":\"" + canonicalId + "\","
            + "\"value\":" + value + ","
            + "\"min\":" + min + ","
            + "\"max\":" + max
            + "}}";
    }
}