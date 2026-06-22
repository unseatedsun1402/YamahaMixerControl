import json

def generate_block(start_hex, canonical_prefix, instances=56, value_mode="CC6_ONLY"):
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


# INPUT TO MIX 1–8 LEVEL
all_blocks += generate_block("0x045E", "kInputToMix.kMix1Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x049E", "kInputToMix.kMix2Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x04DE", "kInputToMix.kMix3Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x051E", "kInputToMix.kMix4Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x055E", "kInputToMix.kMix5Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x059E", "kInputToMix.kMix6Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x05DE", "kInputToMix.kMix7Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x061E", "kInputToMix.kMix8Level", instances=56, value_mode="NRPN_14BIT")


# INPUT TO MIX 9–16 LEVEL
all_blocks += generate_block("0x536A", "kInputToMix.kMix9Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x53AA", "kInputToMix.kMix10Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x53EA", "kInputToMix.kMix11Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x542A", "kInputToMix.kMix12Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x546A", "kInputToMix.kMix13Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x54AA", "kInputToMix.kMix14Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x54EA", "kInputToMix.kMix15Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x552A", "kInputToMix.kMix16Level", instances=56, value_mode="NRPN_14BIT")


# MATRIX1–8 LEVEL
all_blocks += generate_block("0x037E", "kInputToMatrix.kMatrix1Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x03DE", "kInputToMatrix.kMatrix2Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x043E", "kInputToMatrix.kMatrix3Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x049E", "kInputToMatrix.kMatrix4Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x2AEA", "kInputToMatrix.kMatrix5Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x2B2A", "kInputToMatrix.kMatrix6Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x2B6A", "kInputToMatrix.kMatrix7Level", instances=56, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x2BAA", "kInputToMatrix.kMatrix8Level", instances=56, value_mode="NRPN_14BIT")


# INPUT FADER
all_blocks += generate_block("0x00", "kInputFader.kFader", instances=56, value_mode="NRPN_14BIT")


# INPUT PAN
all_blocks += generate_block("0x4116", "kInputPan.kChannelPan", instances=56, value_mode="NRPN_14BIT")

# INPUT DYNAMICS 1 THRESHOLD
all_blocks += generate_block("0x3C20", "kInputDynamics2.kThreshold", instances=56, value_mode="NRPN_14BIT")

# INPUT ON / MUTE
all_blocks += generate_block("0x0B36", "kInputOn.kChannelOn", instances=56, value_mode="NRPN_14BIT")


# OUTPUT FADERS
all_blocks += generate_block("0x0060", "kMixFader.kFader", instances=16, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x0070", "kMatrixFader.kFader", instances=8, value_mode="NRPN_14BIT")
all_blocks += generate_block("0x0078", "kStereoFader.kFader", instances=5, value_mode="NRPN_14BIT")


with open("m7cl_nrpn_mappings.json", "w") as file:
    file.write("[" + ",\n".join(all_blocks) + "]")