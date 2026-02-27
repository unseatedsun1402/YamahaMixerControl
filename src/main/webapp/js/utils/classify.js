import { canonicalIdFor } from "./canonical.js";

export function classifyControl(control) {

    const id = canonicalIdFor(control);

    // FADERS --------------------------------------------------
    if (id.includes("InputFader")) return "fader-role-input";
    if (id.includes("OutFader")) return "fader-role-output";
    if (id.includes("DCAFader")) return "fader-role-dca";
    if (id.includes("GroupFader")) return "fader-role-group";

    // KNOBS ----------------------------------------------------
    if (id.includes(".mix.")) return "knob-role-send";         // Aux / Mix send
    if (id.includes(".matrix.")) return "knob-role-matrix";    // Matrix send
    if (id.includes(".eq.")) return "knob-role-eq";            // EQ band
    if (id.includes(".dyn.")) return "knob-role-dyn";          // Compressor / gate
    if (id.includes(".preamp.") || id.includes(".headamp.") || id.includes(".trim"))
        return "knob-role-gain";                              // Input gain / HA

    // Default fallback
    return "";
}
