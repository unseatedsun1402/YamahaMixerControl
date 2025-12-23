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

# AUX1-8 LEVEL
all_blocks += generate_block("0x007E", "kInputToAux.kAux1Level", instances=40)
all_blocks += generate_block("0x00DE", "kInputToAux.kAux2Level", instances=40)
all_blocks += generate_block("0x013E", "kInputToAux.kAux3Level", instances=40)
all_blocks += generate_block("0x019E", "kInputToAux.kAux4Level", instances=40)
all_blocks += generate_block("0x01FE", "kInputToAux.kAux5Level", instances=40)
all_blocks += generate_block("0x025E", "kInputToAux.kAux6Level", instances=40)
all_blocks += generate_block("0x02BE", "kInputToAux.kAux7Level", instances=40)
all_blocks += generate_block("0x031E", "kInputToAux.kAux8Level", instances=40)

# INPUT FADER
all_blocks += generate_block("0x00","kInputFader.kFader",instances=40)

# OUTPUT FADER
all_blocks += generate_block("0x0060", "kBusFader.kFader", instances=8)
all_blocks += generate_block("0x0068", "kAuxFader.kFader", instances=8)
all_blocks += generate_block("0x0079", "kStereoFader.kFader", instances=5)

with open("01v96_nrpn_mappings.json","w") as file:
    file.write("[" +(",\n".join(all_blocks)+"]"))