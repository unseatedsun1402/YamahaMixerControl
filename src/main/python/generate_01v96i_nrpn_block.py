import json

def generate_block(start_hex, canonical_prefix,
                   instances=32,
                   value_mode="NRPN_14BIT"):
    """
    start_hex: Yamaha NRPN address as a 14-bit hex string

    Example:
        "007E"
        "015E"
        "3C30"
    """

    start_nrpn = int(start_hex, 16)

    for i in range(instances):

        nrpn = start_nrpn + i

        msb = (nrpn >> 7) & 0x7F
        lsb = nrpn & 0x7F

        yield {
            "canonical_id": f"{canonical_prefix}.{i}",
            "msb": f"0x{msb:02X}",
            "lsb": f"0x{lsb:02X}",
            "value_mode": value_mode
        }

blocks = [

    # AUX LEVELS

    ("007E", "kInputAUX.kAUX1Level", 40),
    ("015E", "kInputAUX.kAUX2Level", 40),
    ("023E", "kInputAUX.kAUX3Level", 40),
    ("031E", "kInputAUX.kAUX4Level", 40),
    ("03FE", "kInputAUX.kAUX5Level", 40),
    ("045E", "kInputAUX.kAUX6Level", 40),
    ("053E", "kInputAUX.kAUX7Level", 40),
    ("061E", "kInputAUX.kAUX8Level", 40),

    # INPUT FADER

    ("0000", "kInputFader.kFader", 40),

    # INPUT PAN

    ("0216", "kInputPan.kChannelPan", 40),

    # INPUT COMP THRESHOLD

    ("3C30", "kInputDynamics1.kThreshold", 40),

    # INPUT ON

    ("1636", "kInputOn.kChannelOn", 40),

    # OUTPUT FADERS

    ("0060", "kAUXFader.kFader", 8),
    ("0068", "kBusFader.kFader", 8),
    ("0079", "kStereoFader.kFader", 5),
]


all_blocks = []

for start_hex, canonical_id, count in blocks:

    all_blocks.extend(
        generate_block(
            start_hex,
            canonical_id,
            instances=count,
            value_mode="NRPN_14BIT"
        )
    )


with open("01v96i_nrpn_mappings.json", "w") as f:
    json.dump(
        all_blocks,
        f,
        indent=2,
        sort_keys=True
    )

    print("Written to 01v96i_nrpn_mappings.json")
