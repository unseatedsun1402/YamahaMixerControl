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

## Logical Controls Summary

Files:

01v96i_sysex_mappings.json: JSON list, 2,791 records.
m7cl_sysex_mappings.json: JSON list, 990 records.
Record structure: control_group, control_id, max_channels, sub_control, value, min_value, max_value, defaults/comments, address/index bytes, parameter formats, and priority.
01V96i mappings
Attenuation:

kInputAttenuator — 2 records, 40 channels; kAtt range -960..120, kAttBitShift -24..2.
kAUXAttenuator, kBusAttenuator — each 2 records, 8 channels; same sub-controls/ranges.
kStereoAttenuator — 2 records, 2 channels; same sub-controls/ranges.
Input routing:

kInputRouting — 11 records, 40 channels:
kRoutingStereo, kRoutingPan, kRoutingDirect, kRoutingBus1 through kRoutingBus8; all ranges 0..1.
kInputGroup — 27 records, one control instance:
kInGroupFader1..8, kInGroupMute1..8, kInGroupDynamics1..4, kInGroupEQ1..4, kInGroupLevel, kInMasterFaderAsgn, kInMasterMuteAsgn.
Ranges are mostly 0..1, with assignment/level fields including 0..8 and 0..1023.
Mix/AUX:

kInputAUX — 24 records, 40 channels:
kAUX1On..kAUX8On, kAUX1Pre..kAUX8Pre, kAUX1Level..kAUX8Level.
On/pre fields are 0..1; level fields are 0..1023.
kAuxSendPrePoint / kPrePoint — 1 record, range 0..1.
kAUXChannelOn and kAUXChannelDSPOn — each 1 record, kChannelOn, range 0..1.
Mute-group assignment:

kSceneInputGroup — kInMuteGroup1..8, kInMuteGrpMasterEnable; 27 total records, range 0..1 for these fields.
kSceneOutputGroup — kOutMuteGroup1..4, kOutMuteGrpMasterEnable; 19 total records, range 0..1.
kInMuteGrpMaster — 8 instances; kMasterMute, range 0..1.
kOutMuteGrpMaster — 4 instances; kMasterMute, range 0..1.
Patching:

kScenePatchLink — kInPatchLinkOn, kOutPatchLinkOn (0..1), and kInPatchLinkNo, kOutPatchLinkNo (0..31).
kChannelInput / kChannelIn — 1 mapping, 40-channel instance count, range 0..150.
M7CL mappings
Attenuation:

kInputAttenuator, kMixAttenuator, kMatrixAttenuator, kStereoAttenuator — each 2 records with kAttGang (0..1) and kAtt (-960..240).
Instance counts are respectively 56, 16, 8, and 3.
Input bus/group routing:

kInputToMix — 51 records, 56-channel instances:
kInputToMixLoc, kFixedMixPan, kFollowPan, and kMix1On..kMix16On, kMix1Pos..kMix16Pos, kMix1Level..kMix16Level.
On/position fields use 0..1; level fields use 0..1023.
kInputToMono / kToMonoOn and kInputToStereo / kToStereoOn — one record each, 0..1.
kInputToMatrix and kMixToMatrix — each 27 records with kToMatrixLoc, kFixedMatrixPan, kFollowPan, and kMatrix1On..8, kMatrix1Pos..8, kMatrix1Level..8; on/position 0..1, levels 0..1023.
kMixToMono / kMixToMonoOn — one record, 0..1.
kMixToStereo — kMixToStereoOn, kMixToStereoPre (0..1), and kMixToStereoPan (-63..63).
Mix on/post:

kMixOn / kChannelOn — 1 record, 16-channel instance count, 0..1.
kMixPostToStereo / kPostToStereo — 1 record, 16-channel instance count, 0..1.
Mute assignment/safety:

kDCAMute / kDCAMuteOn — 1 record, 8-channel instance count, 0..1.
kMuteSafeInput and kMuteSafeMix / kMuteSafeOn — one record each, 0..1.
No literal M7CL kInMuteGroup/kOutMuteGroup assignment group exists in these Sysex mappings; M7CL uses DCA and mute-safe structures.
Channel/bus patching:

Input patch groups: kPatchInInput (56 instances, source range 0..120), kPatchInInsertInput (56), kPatchInInsertMix (16), kPatchInInsertMatrix (8), kPatchInInsertStereo (3).
Cascade patch groups: kPatchInCascadeMix (16), kPatchInCascadeMatrix (8), kPatchInCascadeStereo (3), each range 0..104.
Output patch groups:
kPatchOutOmniOutSrc / kOmniOut — 32 instances, 0..182.
kPatchOutSlotOutSrc / kSlotOut1..16 — 16 records, 3 instances each, 0..182.
kPatchOutRackSrc / kRackIn1..2 — 8 instances each, 0..153.
kPatchOutD2trSrc / kD2trOut — 2 instances, 0..30.
