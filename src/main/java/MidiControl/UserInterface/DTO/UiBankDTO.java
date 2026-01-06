package MidiControl.UserInterface.DTO;

import java.util.List;
import java.util.Map;

public class UiBankDTO {
    public String contextId;          // e.g. "bank.inputs"
    public List<String> contexts;     // e.g. ["channel.1", "channel.2", ...]
    public Map<String, Object> metadata; // loose-typed metadata
}
