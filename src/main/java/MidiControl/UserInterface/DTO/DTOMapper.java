package MidiControl.UserInterface.DTO;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ViewControl;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DTOMapper {

    public UiModelDTO toDto(Context ctx, List<ViewControl> controls) {
        UiModelDTO dto = new UiModelDTO();
        dto.contextId = ctx.getId();
        dto.controls = controls.stream()
                               .map(this::toDto)
                               .collect(Collectors.toList());
        dto.metadata = new HashMap<>();
        dto.metadata.put("label", ctx.getLabel());
        dto.metadata.put("contextType", ctx.getContextType().name());
        dto.metadata.put("rolesAllowed", ctx.getRolesAllowed());
        dto.metadata.put("filters", ctx.getFilters());
        return dto;
    }

    private ViewControlDTO toDto(ViewControl vc) {
        ViewControlDTO dto = new ViewControlDTO();

        dto.logicId = vc.logicId;        // e.g. "PAN", "SEND_MIX1"
        dto.uiGroup = vc.uiGroup;        // e.g. "kInputPan"
        dto.label = vc.label;            // e.g. "Pan"
        dto.type = vc.type.name();       // e.g. "SLIDER_HORIZONTAL"
        dto.index = vc.index;            // ordering within group

        dto.hwGroup = vc.hwGroup;            // e.g. "kInputPan"
        dto.hwSubcontrol = vc.hwSubcontrol;  // e.g. "kChannelPan"
        dto.hwInstance = vc.hwInstance;      // e.g. 1
        dto.canonicalId = (vc.hwGroup+"."+vc.hwSubcontrol+"."+vc.hwInstance);

        dto.value = vc.value;
        dto.min = vc.min;
        dto.max = vc.max;

        dto.bipolar = vc.bipolar;
        dto.stepped = vc.stepped;
        dto.readOnly = vc.readOnly;
        dto.unit = vc.unit;

        return dto;
    }
}