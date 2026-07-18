import csv
import json
from collections import Counter, defaultdict


def generate_block(start_hex, canonical_prefix, instances, value_mode):
    """
    start_hex: Yamaha NRPN address

    Example:
        28EA
        2096
        1304
    """

    start = int(start_hex, 16)

    for i in range(instances):

        nrpn = start + i

        yield {
            "canonical_id": f"{canonical_prefix}.{i}",
            "block": canonical_prefix,
            "instance": i,
            "msb": f"0x{((nrpn >> 7) & 0x7F):02X}",
            "lsb": f"0x{(nrpn & 0x7F):02X}",
            "value_mode": value_mode,
        }


def load_blocks(csv_file):
    with open(csv_file, newline="") as f:
        reader = csv.DictReader(f)

        blocks = []

        for row in reader:
            blocks.append(
                (
                    row["hex_from"],
                    row["canonical_id"],
                    int(row["count"]),
                    row.get("value_mode", "NRPN_14BIT"),
                )
            )

        return blocks


def build_mapping(csv_file):
    mappings = []

    blocks = load_blocks(csv_file)

    print()
    print("Blocks Loaded")
    print("=============")

    for start_hex, canonical_id, count, value_mode in blocks:

        print(
            f"{canonical_id:<45}"
            f"{count:>4} instances "
            f"@ {start_hex}"
        )

        mappings.extend(
            generate_block(
                start_hex,
                canonical_id,
                count,
                value_mode,
            )
        )

    return mappings


def validate_duplicates(mappings):

    counts = Counter(
        item["canonical_id"]
        for item in mappings
    )

    duplicates = {
        key: count
        for key, count in counts.items()
        if count > 1
    }

    print()
    print("Duplicate Canonical IDs")
    print("=======================")

    if not duplicates:
        print("None found")
        return

    for key, count in sorted(duplicates.items()):
        print(f"{key} ({count})")


def validate_overlapping_addresses(mappings):

    address_map = defaultdict(list)

    for item in mappings:

        nrpn = (
            (int(item["msb"], 16) << 8)
            | int(item["lsb"], 16)
        )

        address_map[nrpn].append(
            item["canonical_id"]
        )

    overlaps = {
        addr: ids
        for addr, ids in address_map.items()
        if len(ids) > 1
    }

    print()
    print("Address Overlap Check")
    print("=====================")

    if not overlaps:
        print("No overlapping NRPN addresses")
        return

    for addr, ids in sorted(overlaps.items()):

        print(
            f"0x{addr:04X}: "
            + ", ".join(ids)
        )


def validate_instance_ranges(mappings):

    block_instances = defaultdict(set)

    for item in mappings:

        block_instances[item["block"]].add(
            item["instance"]
        )

    print()
    print("Instance Range Check")
    print("====================")

    for block, instances in sorted(block_instances.items()):

        expected = set(
            range(max(instances) + 1)
        )

        missing = expected - instances

        if missing:
            print(
                f"{block} missing "
                f"{sorted(missing)}"
            )


def validate_against_sysex(mappings, sysex_file):

    with open(sysex_file) as f:
        sysex = json.load(f)

    nrpn_blocks = {
        item["block"]
        for item in mappings
    }

    sysex_blocks = {
        item["control_group"]
        + "."
        + item["sub_control"]
        for item in sysex
    }

    orphaned_nrpn = sorted(
        nrpn_blocks - sysex_blocks
    )

    print()
    print("Canonical Name Validation")
    print("=========================")

    print(f"NRPN blocks generated : {len(nrpn_blocks)}")

    if orphaned_nrpn:

        print(
            f"Unknown NRPN canonical names : "
            f"{len(orphaned_nrpn)}"
        )

        for each in orphaned_nrpn:
            print(each)

        print()

    else:
        print(
            "All NRPN block names exist in SysEx mappings."
        )


def print_block_statistics(mappings):

    counts = Counter(
        item["block"]
        for item in mappings
    )

    print()
    print("NRPN Block Statistics")
    print("=====================")

    for block, count in sorted(counts.items()):

        print(
            f"{count:>4}  {block}"
        )


if __name__ == "__main__":

    mappings = build_mapping(
        "nrpn_m7cl/all_nrpn.csv"
    )

    print()
    print("Summary")
    print("=======")

    print(
        f"Generated {len(mappings)} NRPN mappings"
    )

    unique_ids = len(
        {
            item["canonical_id"]
            for item in mappings
        }
    )

    print(
        f"Unique canonical IDs: {unique_ids}"
    )

    print_block_statistics(mappings)

    validate_duplicates(mappings)

    validate_overlapping_addresses(mappings)

    validate_instance_ranges(mappings)

    validate_against_sysex(
        mappings,
        "src/main/resources/MidiControl/m7cl_sysex_mappings.json"
    )

    with open(
        "m7cl_nrpn_mappings.json",
        "w"
    ) as f:

        json.dump(
            mappings,
            f,
            indent=2,
            sort_keys=True
        )

    print()
    print(
        "Wrote m7cl_nrpn_mappings.json"
    )