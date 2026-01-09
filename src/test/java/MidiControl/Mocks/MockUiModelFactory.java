package MidiControl.Mocks;

import MidiControl.UserInterface.UiModelFactory;
import MidiControl.UserInterface.DTO.UiModelDTO;

public class MockUiModelFactory extends UiModelFactory {

    public UiModelDTO dto = new UiModelDTO();

    public MockUiModelFactory() {
        super(null, null, null); // safe because we override buildUiModel
    }

    @Override
    public UiModelDTO buildUiModel(String contextId) {
        return dto;
    }
}

