import { canonicalIdFor } from "./canonical.js";

export function classifyControl(control) {

    const id = canonicalIdFor(control);
    const logicId = control.logicId || "";
    const uiGroup = control.uiGroup || "";
    const controlRole = control.controlRole || "";

    if (control.type === "FADER") {
        if (
            controlRole === "INPUT_SEND_LEVEL" ||
            uiGroup === "input.send.sof" ||
            logicId.startsWith("SEND_")
        ) return "fader-role-sof";
        if (
            controlRole === "BUS_FADER" &&
            id.includes("StereoFader")
        ) return "fader-role-master";
        if (controlRole === "BUS_FADER" || uiGroup === "bus.fader")
            return "fader-role-output";
        if (id.includes("InputFader")) return "fader-role-input";
        if (id.includes("OutFader")) return "fader-role-output";
        if (id.includes("DCAFader")) return "fader-role-dca";
        if (id.includes("GroupFader")) return "fader-role-group";
        return "";
    }

    if (control.type !== "KNOB") return "";

    if (
        controlRole === "INPUT_SEND_LEVEL" ||
        uiGroup.startsWith("input.send") ||
        logicId.startsWith("SEND_") ||
        /k(?:AUX|Mix)\d+Level/i.test(id) ||
        id.includes(".mix.")
    ) return "knob-role-send";
    if (id.includes(".matrix.")) return "knob-role-matrix";
    if (id.includes(".eq.")) return "knob-role-eq";
    if (id.includes(".dyn.")) return "knob-role-dyn";
    if (id.includes(".preamp.") || id.includes(".headamp.") || id.includes(".trim"))
        return "knob-role-gain";

    // Default fallback
    return "";
}
