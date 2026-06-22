import json

def generate_block(start_msb=int, start_lsb=int,canonical_prefix=str, instances=32, value_mode="NRPN_14BIT"):

    if start_msb > 0x7F or start_lsb > 0x7F:
        raise ValueError("Invalid NRPN start bytes")

    start_nrpn = (start_msb << 7) | start_lsb

    entries = []

    for i in range(instances):
        nrpn = start_nrpn + i

        msb = (nrpn >> 7) & 0x7F
        lsb = nrpn & 0x7F

        entry = {
            "canonical_id": f"{canonical_prefix}.{i}",
            "msb": f"0x{msb:02X}",
            "lsb": f"0x{lsb:02X}",
            "value_mode": value_mode
        }

        entries.append(json.dumps(entry, separators=(",", ":")))

    return entries

# Collect all blocks in one list
all_blocks = []


# AUX1–8 LEVEL (01V96i = 14-bit NRPN)
all_blocks += generate_block(0x00, 0x7E, "kInputAUX.kAUX1Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x01, 0x5E, "kInputAUX.kAUX2Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x02, 0x3E, "kInputAUX.kAUX3Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x03, 0x1E, "kInputAUX.kAUX4Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x03, 0x7E, "kInputAUX.kAUX5Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x04, 0x5E, "kInputAUX.kAUX6Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x05, 0x3E, "kInputAUX.kAUX7Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x06, 0x1E, "kInputAUX.kAUX8Level", instances=40, value_mode="NRPN_14BIT")


# INPUT FADER (01V96i = 14-bit NRPN)
all_blocks += generate_block(0x00, 0x00, "kInputFader.kFader", instances=40, value_mode="NRPN_14BIT")

# INPUT PAN
all_blocks += generate_block(0x02, 0x16, "kInputPan.kChannelPan", instances=40, value_mode="NRPN_14BIT")

# INPUT COMPRESSOR THRESHOLD (corrected direction)
all_blocks += generate_block(0x3C,0x30, "kInputDynamics1.kThreshold")

# INPUT ON / MUTE
all_blocks += generate_block(0x16, 0x36, "kInputOn.kChannelOn", instances=40, value_mode="NRPN_14BIT")


# OUTPUT FADERS
all_blocks += generate_block(0x00, 0x60, "kAUXFader.kFader", instances=8, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x00,0x68, "kBusFader.kFader", instances=8, value_mode="NRPN_14BIT")
all_blocks += generate_block(0x00,0x79, "kStereoFader.kFader", instances=5, value_mode="NRPN_14BIT")


with open("01v96i_nrpn_mappings.json", "w") as file:
    file.write("[" + ",\n".join(all_blocks) + "]")