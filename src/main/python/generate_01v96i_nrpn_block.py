import json

def generate_block(start_hex, canonical_prefix, instances=32, value_mode="NRPN_14BIT"):
    """
    start_hex: e.g. '0x28EA'
    canonical_prefix: e.g. 'kInputToMix.kMix1Level'
    value_mode: controls NRPN value encoding behaviour
    """
    start = int(start_hex, 16)
    entries = []

    for i in range(instances):
        nrpn = start + i
        msb = (nrpn >> 8) & 0xFF
        lsb = nrpn & 0xFF

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
all_blocks += generate_block("0x007E", "kInputAUX.kAUX1Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x00DE", "kInputAUX.kAUX2Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x013E", "kInputAUX.kAUX3Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x019E", "kInputAUX.kAUX4Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x01FE", "kInputAUX.kAUX5Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x025E", "kInputAUX.kAUX6Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x02BE", "kInputAUX.kAUX7Level", instances=40, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x031E", "kInputAUX.kAUX8Level", instances=40, value_mode="NRPN_14BIT")


# INPUT FADER (01V96i = 14-bit NRPN)
all_blocks += generate_block("0x00", "kInputFader.kFader", instances=40, value_mode="NRPN_14BIT")

# INPUT PAN
all_blocks += generate_block("0x4116", "kInputPan.kChannelPan")

# INPUT COMPRESSOR THRESHOLD (corrected direction)
all_blocks += generate_block("0x3C30", "kInputDynamics1.kThreshold")

# INPUT ON / MUTE
all_blocks += generate_block("0x0B36", "kInputOn.kChannelOn")


# OUTPUT FADERS
all_blocks += generate_block("0x0060", "kAUXFader.kFader", instances=8, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x0068", "kBusFader.kFader", instances=8, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x0079", "kStereoFader.kFader", instances=5, value_mode="NRPN_14BIT")


with open("01v96i_nrpn_mappings.json", "w") as file:
    file.write("[" + ",\n".join(all_blocks) + "]")