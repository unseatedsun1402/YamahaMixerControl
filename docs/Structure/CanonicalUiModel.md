# Canonical UI Model — Input Channel
This defines the complete set of UI‑canonical groups and controls for an input
channel. It includes both the **compact view** (minimal live‑mixing controls)
and the **full view** (all processing blocks).

---

# 1. Canonical UI Groups
These groups may appear in an input channel strip:

- **kInputControl** — Channel On, Mute (if used)
- **kInputPan** — Pan controls
- **kInputToMix** — Sends to Mix buses
- **kInputFader** — Main channel fader
- **kInputHA** — Head amp / preamp (full view)
- **kInputEQ** — 4‑band EQ (full view)
- **kInputDynamics** — Gate/Comp (full view)
- **kInputToMatrix** — Sends to Matrix (full view)

The **compact view** uses only:
- kInputControl  
- kInputPan  
- kInputToMix  
- kInputFader  

---

# 2. Canonical Controls (logic_id)

## Compact View Controls
These are the controls included in the compact input channel strip.

### kInputControl
- `CHANNEL_ON` — TOGGLE — “On”

### kInputPan
- `PAN` — SLIDER_HORIZONTAL — “Pan”

### kInputToMix
For each mix bus `n`:
- `SEND_MIX{n}` — KNOB — “Mix {n}”

### kInputFader
- `FADER` — FADER — “Fader”

---

## Full View Controls
These controls are not in the compact strip but are part of the full canonical model.

### kInputHA
- `HA_GAIN` — KNOB — “Gain”
- `HPF_FREQ` — KNOB — “HPF Freq”
- `HPF_ON` — TOGGLE — “HPF”

### kInputEQ (4‑band parametric EQ)
For each band `b = 1..4`:
- `EQ{b}_GAIN` — KNOB — “Gain”
- `EQ{b}_FREQ` — KNOB — “Frequency”
- `EQ{b}_Q` — KNOB — “Q”
- `EQ{b}_TYPE` — SELECT — “Type”

Global EQ toggle:
- `EQ_ON` — TOGGLE — “EQ On”

### kInputDynamics (optional)
- `GATE_ON` — TOGGLE
- `GATE_THRESH` — KNOB
- `COMP_ON` — TOGGLE
- `COMP_THRESH` — KNOB

### kInputToMatrix (optional)
For each matrix bus `x`:
- `SEND_MATRIX{x}` — KNOB — “Matrix {x}”

### kInputControl (extended)
- `INPUT_MUTE` — TOGGLE — “Mute Group Assign” (if implemented)

---

# 3. Ordering Rules

## Compact View Order
1. kInputControl  
2. kInputPan  
3. kInputToMix  
4. kInputFader  

## Full View Order
1. kInputHA  
2. kInputEQ  
3. kInputDynamics  
4. kInputToMix  
5. kInputToMatrix  
6. kInputPan  
7. kInputControl  
8. kInputFader  

---

# 4. Identity Model
Each ViewControl contains:

### UI identity (logic_id)
- Stable, desk‑agnostic identifier (e.g., `SEND_MIX1`, `EQ1_GAIN`)

### Hardware identity (canonical_id)
- `hwGroup` — e.g., `"kInputToMix"`
- `hwSubcontrol` — e.g., `"kMix1Level"`
- `hwInstance` — context index

This dual‑identity model ensures:
- UI is clean and desk‑agnostic  
- Hardware mapping remains precise and lossless  

---

# 5. Compact vs Full Feature Matrix

| Feature          | Compact | Full |
|------------------|---------|------|
| Channel On       | ✔       | ✔    |
| Pan              | ✔       | ✔    |
| Mix Sends        | ✔       | ✔    |
| Fader            | ✔       | ✔    |
| HA Gain          | ✖       | ✔    |
| HPF              | ✖       | ✔    |
| EQ               | ✖       | ✔    |
| Dynamics         | ✖       | ✔    |
| Matrix Sends     | ✖       | ✔    |
| Input Mute       | ✖       | ✔    |
