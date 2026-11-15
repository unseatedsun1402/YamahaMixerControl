import argparse
import csv
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SYSEX = ROOT / "src" / "main" / "resources" / "MidiControl" / "01v96i_sysex_mappings.json"
REFERENCE_BLOCKS = ROOT / "nrpn_m7cl" / "all_nrpn.csv"

# M7CL reference names differ from the 01V96i SysEx model for these controls.
ALIASES = {
    f"kInputEQ.kEQ{number}{parameter}": f"kInputEQ.kEQ{name}{parameter}"
    for number, name in ((1, "Low"), (2, "LowMid"), (3, "HiMid"), (4, "Hi"))
    for parameter in ("Q", "F", "G")
}

def generate_block(start_hex, canonical_prefix,
                   instances=32,
                   value_mode="NRPN_14BIT"):
    """
    start_hex: Yamaha NRPN address as a 14-bit hex string

    Example:
        "007E"
        "00DE"
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
    ("00DE", "kInputAUX.kAUX2Level", 40),
    ("013E", "kInputAUX.kAUX3Level", 40),
    ("019E", "kInputAUX.kAUX4Level", 40),
    ("01FE", "kInputAUX.kAUX5Level", 40),
    ("025E", "kInputAUX.kAUX6Level", 40),
    ("02BE", "kInputAUX.kAUX7Level", 40),
    ("031E", "kInputAUX.kAUX8Level", 40),

    # INPUT FADER

    ("0000", "kInputFader.kFader", 40),

    # INPUT PAN

    ("2096", "kInputChannelPan.kChannelPan", 40),

    # INPUT COMP THRESHOLD

    ("3C30", "kInputDynamics1.kThreshold", 40),

    # INPUT ON

    ("1636", "kInputOn.kChannelOn", 40),

    # OUTPUT FADERS

    ("0060", "kAUXFader.kFader", 8),
    ("0068", "kBusFader.kFader", 8),
    ("0079", "kStereoFader.kFader", 5),
]


def reference_names():
    names = {}
    with REFERENCE_BLOCKS.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            key = (int(row["hex_from"], 16), int(row["hex_to"], 16))
            names.setdefault(key, []).append(row["canonical_id"])
    return names


def sysex_controls():
    with SYSEX.open(encoding="utf-8") as file:
        controls = json.load(file)
    result = {}
    for control in controls:
        name = f'{control["control_group"]}.{control["sub_control"]}'
        if name in result:
            raise ValueError(f"Duplicate SysEx control: {name}")
        result[name] = control["max_channels"]
    return result


def proposed_addresses(control, max_channels):
    observations = [
        observation
        for capture in control["learned"]
        for observation in capture["observations"]
    ]
    channels = {observation["midiChannel"] for observation in observations}
    channels.update(
        test["midiChannel"]
        for test in control["tests"]
        if test["status"] == "confirmed_by_user"
    )
    if len(channels) > 1:
        raise ValueError("evidence spans multiple MIDI channels")

    addresses = {}
    if observations:
        observed = sorted({observation["address"] for observation in observations})
        if any(not isinstance(address, int) or not 0 <= address <= 0x3FFF for address in observed):
            raise ValueError("observed address outside the 14-bit NRPN range")
        table_start = control["tableStart"]
        if table_start <= observed[0] and observed[-1] < table_start + max_channels:
            start = table_start
            source = "table start" if start != observed[0] else "observed start, assumed first instance"
        else:
            start = observed[0]
            source = "lowest observed address (assumed first instance)"
        count = observed[-1] - start + 1
        if count > max_channels:
            raise ValueError(f"inferred {count} instances but SysEx supports only {max_channels}")
        addresses.update(enumerate(range(start, observed[-1] + 1)))
        print(f"  Observed addresses: {', '.join(f'{address:04X}' for address in observed)}")
        print(f"  Proposed {start:04X}-{observed[-1]:04X} ({count} instances; {source}; "
              "intermediate addresses inferred)")
        if any(not control["tableStart"] <= address <= control["tableEnd"] for address in observed):
            print("  WARNING: observations fall outside the selected M7CL table range.")
        for observation in observations:
            print(f'  {observation["address"]:04X}: '
                  f'CC6 {observation.get("minCc6")}..{observation.get("maxCc6")}, '
                  f'CC38 {observation.get("minCc38")}..{observation.get("maxCc38")}')

    for test in control["tests"]:
        if test["status"] != "confirmed_by_user":
            continue
        instance = test["tableInstance"] - 1
        address = test["address"]
        if not 0 <= instance < max_channels or not 0 <= address <= 0x3FFF:
            raise ValueError("confirmed probe outside the supported instance or address range")
        if instance in addresses and addresses[instance] != address:
            raise ValueError(f"confirmed instance {instance + 1} conflicts with learned range")
        addresses[instance] = address
        print(f"  Confirmed probe: instance {instance + 1} at {address:04X}")
    return addresses


def mappings_from_report(report, choose=input):
    if report["deskModel"].casefold() != "01v96i":
        raise ValueError("report desk model must be 01v96i")
    references = reference_names()
    sysex = sysex_controls()
    mappings = []
    seen_ids = set()
    seen_addresses = set()

    for control in report["controls"]:
        key = (control["tableStart"], control["tableEnd"])
        candidates = [
            ALIASES.get(name, name)
            for name in references.get(key, [])
            if ALIASES.get(name, name) in sysex
        ]
        print(f'\n{control["label"]} ({key[0]:04X}-{key[1]:04X})')
        if len(candidates) != 1:
            print("  Skipped: no unique reviewed reference-to-SysEx match.")
            continue
        canonical = candidates[0]
        print(f"  SysEx control: {canonical}")
        try:
            addresses = proposed_addresses(control, sysex[canonical])
        except ValueError as error:
            print(f"  Skipped: {error}")
            continue
        if not addresses:
            print("  Skipped: no observations or user-confirmed probes.")
            continue
        print(f"  Proposing {len(addresses)} instances. Unobserved addresses are inferred; "
              "observations do not verify instance identity or value mode.")
        modes = {"1": "NRPN_14BIT", "2": "CC6_ONLY", "3": "NRPN_RAW_SPLIT"}
        while True:
            choice = choose("  Value mode [1=NRPN_14BIT, 2=CC6_ONLY, 3=NRPN_RAW_SPLIT, s=skip]: ").strip().lower()
            if choice in modes or choice == "s":
                break
            print("  Enter 1, 2, 3 or s.")
        if choice == "s":
            continue
        for instance, address in sorted(addresses.items()):
            canonical_id = f"{canonical}.{instance}"
            if canonical_id in seen_ids or address in seen_addresses:
                raise ValueError(f"duplicate mapping for {canonical_id} or address {address:04X}")
            seen_ids.add(canonical_id)
            seen_addresses.add(address)
            mappings.append({
                "canonical_id": canonical_id,
                "msb": f"0x{address >> 7:02X}",
                "lsb": f"0x{address & 0x7F:02X}",
                "value_mode": modes[choice],
            })
    return mappings


def main():
    parser = argparse.ArgumentParser(description="Generate 01V96i NRPN mappings.")
    parser.add_argument("--report", type=Path, help="learned NRPN report to consume")
    parser.add_argument("--output", type=Path, help="new output file for report-based mappings")
    args = parser.parse_args()
    if args.output and not args.report:
        parser.error("--output requires --report")
    if args.report:
        output = args.output or Path("01v96i_nrpn_learned_mappings.json")
        if output.exists():
            parser.error(f"output already exists: {output}")
        try:
            with args.report.open(encoding="utf-8") as file:
                report = json.load(file)
            mappings = mappings_from_report(report)
            if not mappings:
                parser.error("no mappings generated; no output written")
            with output.open("x", encoding="utf-8") as file:
                json.dump(mappings, file, indent=2, sort_keys=True)
                file.write("\n")
        except (OSError, ValueError, KeyError, TypeError, EOFError) as error:
            parser.error(str(error))
        print(f"Written {len(mappings)} learned mappings to {output}")
        return

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


if __name__ == "__main__":
    main()
