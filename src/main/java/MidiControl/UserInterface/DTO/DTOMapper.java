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

        dto.logicId = vc.logicId;
        dto.uiGroup = vc.uiGroup;
        dto.label = vc.label;
        dto.type = vc.type.name();
        dto.index = vc.index;

        dto.hwGroup = vc.hwGroup;
        dto.hwSubcontrol = vc.hwSubcontrol;
        dto.hwInstance = vc.hwInstance;

        dto.canonicalId = vc.canonicalId;

        dto.value = vc.value;
        dto.min = vc.min;
        dto.max = vc.max;

        dto.bipolar = vc.bipolar;
        dto.stepped = vc.stepped;
        dto.readOnly = vc.readOnly;
        dto.unit = vc.unit;
        
        dto.controlRole = vc.controlRole;
        dto.sendIndex = vc.sendIndex;
        dto.channelIndex = vc.channelIndex;
        dto.viewType = vc.viewType;
        dto.viewSuffix = vc.viewSuffix;

        return dto;
    }
}