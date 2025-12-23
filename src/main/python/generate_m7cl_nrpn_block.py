import json

def generate_block(start_hex, canonical_prefix, instances=56):
    """
    start_hex: e.g. '0x28EA'
    canonical_prefix: e.g. 'kInputToMix.kMix1Level'
    Prints each JSON entry on a single line.
    """
    start = int(start_hex, 16)
    entries = []

    for i in range(instances):
        nrpn = start + i
        msb = (nrpn >> 8) & 0xFF
        lsb = nrpn & 0xFF

        entry = {
            "canonical_id": f"{canonical_prefix}.{i+1}",
            "msb": f"0x{msb:02X}",
            "lsb": f"0x{lsb:02X}"
        }

        entries.append(json.dumps(entry, separators=(",", ":")))

    return entries


# Collect all blocks in one list
all_blocks = []

# MIX1–8 LEVEL
all_blocks += generate_block("0x28EA", "kInputToMix.kMix1Level", instances=56)
all_blocks += generate_block("0x292A", "kInputToMix.kMix2Level", instances=56)
all_blocks += generate_block("0x296A", "kInputToMix.kMix3Level", instances=56)
all_blocks += generate_block("0x29AA", "kInputToMix.kMix4Level", instances=56)
all_blocks += generate_block("0x29EA", "kInputToMix.kMix5Level", instances=56)
all_blocks += generate_block("0x2A2A", "kInputToMix.kMix6Level", instances=56)
all_blocks += generate_block("0x2A6A", "kInputToMix.kMix7Level", instances=56)
all_blocks += generate_block("0x2AAA", "kInputToMix.kMix8Level", instances=56)

# MIX9–16 LEVEL
all_blocks += generate_block("0x007E", "kInputToMix.kMix9Level", instances=56)
all_blocks += generate_block("0x00DE", "kInputToMix.kMix10Level", instances=56)
all_blocks += generate_block("0x013E", "kInputToMix.kMix11Level", instances=56)
all_blocks += generate_block("0x019E", "kInputToMix.kMix12Level", instances=56)
all_blocks += generate_block("0x01FE", "kInputToMix.kMix13Level", instances=56)
all_blocks += generate_block("0x025E", "kInputToMix.kMix14Level", instances=56)
all_blocks += generate_block("0x02BE", "kInputToMix.kMix15Level", instances=56)
all_blocks += generate_block("0x031E", "kInputToMix.kMix16Level", instances=56)

# MATRIX1–8 LEVEL
all_blocks += generate_block("0x037E", "kInputToMatrix.kMatrix1Level", instances=56)
all_blocks += generate_block("0x03DE", "kInputToMatrix.kMatrix2Level", instances=56)
all_blocks += generate_block("0x043E", "kInputToMatrix.kMatrix3Level", instances=56)
all_blocks += generate_block("0x049E", "kInputToMatrix.kMatrix4Level", instances=56)
all_blocks += generate_block("0x2AEA", "kInputToMatrix.kMatrix5Level", instances=56)
all_blocks += generate_block("0x2B2A", "kInputToMatrix.kMatrix6Level", instances=56)
all_blocks += generate_block("0x2B6A", "kInputToMatrix.kMatrix7Level", instances=56)
all_blocks += generate_block("0x2BAA", "kInputToMatrix.kMatrix8Level", instances=56)

all_blocks += generate_block("0x00","kInputFader.kFader",instances=56)

all_blocks += generate_block("0x0060", "kMixFader.kFader", instances=16)
all_blocks += generate_block("0x0070", "kMatrixFader.kFader", instances=8)
all_blocks += generate_block("0x0078", "kStereoFader.kFader", instances=5)

with open("m7cl_nrpn_mappings.json","w") as file:
    file.write("[" +(",\n".join(all_blocks)+"]"))
