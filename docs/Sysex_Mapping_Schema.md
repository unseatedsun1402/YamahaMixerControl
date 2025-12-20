# SysEx Mapping JSON Schema

Each JSON file (e.g. 01V96i_sysex_mappings.json, m7cl_sysex_mappings.json) contains an array of mapping objects.
Each mapping represents a unique control type (not per‑channel instance).

## Fields

### Field Definitions

| Field                       | Type              | Description |
|-----------------------------|-------------------|-------------|
| control_group               | string            | Logical grouping of controls (e.g. "kInputFader", "kInputHA"). |
| control_id                  | integer           | Identifier from datasheet (if available). |
| sub_control                 | string            | Specific element within the group (e.g. "kFader", "kHAPhantom"). |
| comment                     | string            | Human‑readable note, often datasheet reference (e.g. "PRM TABLE #05-2"). |
| key                         | long              | Precomputed registry key:<br>(model << 32)<br>(scope << 24)<br>(group << 16)<br>(sub_control << 8)<br>param. Used by the Java registry for resolution. |
| value                       | integer \| null   | Default or fixed value (optional). |
| min_value                   | integer \| null   | Minimum allowed value (optional). |
| max_value                   | integer \| null   | Maximum allowed value (optional). |
| default_value               | integer \| null   | Default value (optional). |
| parameter_change_format     | array             | SysEx byte pattern for parameter change messages. Tokens may include: "1n", "3n", "cc", "dd". |
| parameter_request_format    | array             | SysEx byte pattern for parameter request messages. Same token rules as above. |

---

## Token Semantics

- **"1n"** → Parameter change header byte (0x10 + device number nibble).  
- **"3n"** → Parameter request header byte (0x30 + device number nibble).  

### Channel Index Tokens
- **"cc"** → Channel index placeholder  
  - 01V96i: single "cc" (1 byte)  
  - M7CL: two consecutive "cc" tokens (2 bytes)

### Data Tokens
- **"dd"** → Data byte placeholder  
  - 01V96i: 4 "dd" tokens  
  - M7CL: 5 "dd" tokens

### Fixed SysEx Bytes
- **240** → SysEx start (F0)  
- **67** → Yamaha manufacturer ID (0x43)  
- **247** → SysEx end (F7)

### Other SysEx Bytes
- **@[2]** → Yamaha Model ID

## Example: 01V96i Input Fader

```{
  "control_group": "kInputFader",
  "control_id": 27,
  "sub_control": "kFader",
  "comment": "PRM TABLE #05-2",
  "key": 266287972352,
  "parameter_change_format": [
    240, 67, "1n", 62, 127, 1, 28, 0,
    "cc", "dd", "dd", "dd", "dd", 247
  ],
  "parameter_request_format": [
    240, 67, "3n", 62, 127, 1, 28, 0,
    "cc", 247
  ]
}
```

## Example: M7CL Input Fader

```
{
  "control_group": "kInputFader",
  "control_id": 27,
  "sub_control": "kFader",
  "comment": "PRM TABLE #05-2",
  "key": 120259084288,
  "parameter_change_format": [
    240, 67, "1n", 28, 127, 1, 29, 0,
    "cc", "cc", "dd", "dd", "dd", "dd", "dd", 247
  ],
  "parameter_request_format": [
    240, 67, "3n", 28, 127, 1, 29, 0,
    "cc", "cc", 247
  ]
}
```

## Usage in Registry

Registry consumes key directly for resolution.

Formats are used only for interpretation (knowing where channel index and data bytes go) and message building (constructing outgoing SysEx).

No per‑channel expansion in JSON; channel index and values are runtime concerns.