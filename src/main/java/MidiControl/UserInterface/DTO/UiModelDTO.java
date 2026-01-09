package MidiControl.UserInterface.DTO;

import java.util.List;
import java.util.Map;

public class UiModelDTO {
    public String contextId;
    public List<ViewControlDTO> controls;
    public Map<String, Object> metadata;

    public UiModelDTO(){}
}