package MidiControl.UserInterface;

import MidiControl.UserInterface.DTO.UiModelDTO;

public interface UiModelService {
    UiModelDTO buildUiModel(String contextId, String uiType);
}