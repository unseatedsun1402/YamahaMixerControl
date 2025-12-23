package MidiControl.UserInterface.DTO;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ViewControl;

import java.util.List;
import java.util.stream.Collectors;

public class DTOMapper {

    public UiModelDTO toDto(Context ctx, List<ViewControl> controls) {
        UiModelDTO dto = new UiModelDTO();
        dto.contextId = ctx.getId();
        dto.controls = controls.stream()
                               .map(this::toDto)
                               .collect(Collectors.toList());
        return dto;
    }

    private ViewControlDTO toDto(ViewControl vc) {
        ViewControlDTO dto = new ViewControlDTO();

        // ---------------------------------------------------------------------
        // UI Identity
        // ---------------------------------------------------------------------
        dto.logicId = vc.logicId;        // e.g. "PAN", "SEND_MIX1"
        dto.uiGroup = vc.uiGroup;        // e.g. "kInputPan"
        dto.label = vc.label;            // e.g. "Pan"
        dto.type = vc.type.name();       // e.g. "SLIDER_HORIZONTAL"
        dto.index = vc.index;            // ordering within group

        // ---------------------------------------------------------------------
        // Hardware Identity (canonical_id)
        // ---------------------------------------------------------------------
        dto.hwGroup = vc.hwGroup;            // e.g. "kInputPan"
        dto.hwSubcontrol = vc.hwSubcontrol;  // e.g. "kChannelPan"
        dto.hwInstance = vc.hwInstance;      // e.g. 1

        // ---------------------------------------------------------------------
        // Value Range
        // ---------------------------------------------------------------------
        dto.value = vc.value;
        dto.min = vc.min;
        dto.max = vc.max;

        // ---------------------------------------------------------------------
        // UI Hints
        // ---------------------------------------------------------------------
        dto.bipolar = vc.bipolar;
        dto.stepped = vc.stepped;
        dto.readOnly = vc.readOnly;
        dto.unit = vc.unit;

        return dto;
    }
}