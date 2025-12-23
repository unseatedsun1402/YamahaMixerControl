# 1. Hardware / Protocol Layer
- Raw device‑specific control surfaces
- Yamaha: SysEx, NRPN, parameter change messages
- X32: OSC
- TouchOSC: arbitrary user‑defined controls
- Each desk exposes its own groups, subcontrols, instances, quirks, and naming

Output: Raw vendor‑specific control data  
Identity: **canonical_id = (groupName, subcontrolName, instanceIndex)**

# 2. Discovery Layer (Registry)
- Parses the vendor protocol
- Discovers:
  - ControlGroups (kInputEQ, kMixEQ, kMatrixEQ, kStereoEQ…)
  - SubControls (kEQ1G, kEQ1F, kMix1Level…)
  - Instances per context (channel.1, mix.3, matrix.2…)
- No filtering
- No interpretation
- No UI logic

Output: A complete, unfiltered map of everything the desk exposes  
Identity preserved: **canonical_id**

# 3. Canonical Mapping Layer (View Builders)
Transforms vendor‑specific discovery into a clean, desk‑agnostic UI model.

Each context type gets its own builder:
- InputChannelStripViewBuilder
- MixBusViewBuilder
- MatrixBusViewBuilder
- GEQViewBuilder
- DynamicsViewBuilder
- etc.

Responsibilities:
- Select only relevant groups (e.g., kInputEQ, not kStereoEQ)
- Normalize control types (bypass → toggle, send level → knob)
- Normalize UI logic IDs (kEQ1G → EQ1_GAIN)
- Normalize UI group names (kInputEQ → canonical kInputEQ)
- Normalize ON/MUTE and EQ ON/BYPASS into single toggles
- Preserve hardware canonical_id for I/O
- Order controls in canonical Yamaha‑style strip order

Output: **CanonicalUiModel**  
Each ViewControl contains:
- **logic_id** (UI identity, e.g. EQ1_GAIN)
- **canonical_id** (hardware identity, e.g. kInputEQ.kEQ1G.22)

**Output:** CanonicalUiModel  
Each ViewControl now has:
- **logic_id** (UI identity, e.g. `EQ1_GAIN`)
- **canonical_id** (hardware identity, e.g. `kInputEQ.kEQ1G.22`)


# 4. UI Model Layer (CanonicalUiModel)
A pure, clean, desk‑agnostic representation of a channel strip.

Groups:
- kInputHA
- kInputEQ
- kInputToMix
- kInputPan
- kInputControl
- kInputFader

Controls (logic_id):
- EQ1_GAIN, EQ1_FREQ, EQ1_Q, EQ1_TYPE, EQ1_ON
- CHANNEL_ON
- SEND_MIX1
- FADER
- etc.

Each control also carries:
- canonical_id → (hwGroup, hwSubcontrol, hwInstance)

Output: A stable, predictable model the UI can render

# 5. UI Renderer Layer
- Renders the canonical model into HTML/CSS/JS
- No desk‑specific logic
- No protocol knowledge
- No discovery logic

Just:
- Render groups
- Render controls
- Apply hybrid EQ behavior
- Apply rotary knobs
- Apply faders, toggles, selects

Input: logic_id + UI metadata  
Output: Interactive UI

# 6. Input Handler Layer (UI → Server)
- User interacts with UI controls
- Renderer sends control changes using **logic_id**:
    sendControlChange("EQ1_GAIN", 5.0)
    sendControlChange("CHANNEL_ON", 1)
    sendControlChange("FADER", -12.0)

- Server maps:
    logic_id → canonical_id → vendor protocol

Examples:
    EQ1_GAIN   → kInputEQ.kEQ1G.22 → SysEx
    CHANNEL_ON → kOn.22           → SysEx
    FADER      → kFader.22        → SysEx

Output: Vendor‑specific control messages back to the desk
