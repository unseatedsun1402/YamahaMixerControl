import json
from collections import defaultdict


def summarize_sysex(json_path):
    with open(json_path, "r") as f:
        data = json.load(f)

    total_controls = 0
    group_counts = defaultdict(int)
    subcontrol_counts = defaultdict(int)

    missing_defaults = 0
    missing_ranges = 0
    missing_change_format = 0
    missing_request_format = 0

    default_values = []

    for entry in data:
        total_controls += 1

        group_name = entry.get("control_group", "Unknown")
        sub_control = entry.get("sub_control", "Unknown")

        group_counts[group_name] += 1
        subcontrol_counts[(group_name, sub_control)] += 1

        # Collect numeric stats
        if entry.get("default_value") is not None:
            default_values.append(entry["default_value"])
        else:
            missing_defaults += 1

        if entry.get("min_value") is None or entry.get("max_value") is None:
            missing_ranges += 1

        if not entry.get("parameter_change_format"):
            missing_change_format += 1
        if not entry.get("parameter_request_format"):
            missing_request_format += 1

    print("=== Sysex Mapping Summary ===")
    print(f"Total controls: {total_controls}")
    print(f"Unique control groups: {len(group_counts)}")

    print("\nTop 5 largest groups:")
    for group, count in sorted(group_counts.items(), key=lambda x: -x[1])[:5]:
        print(f"  {group}: {count} controls")

    print("\nTop 5 largest sub-controls:")
    for (group, sub), count in sorted(subcontrol_counts.items(), key=lambda x: -x[1])[
        :5
    ]:
        print(f"  {group}/{sub}: {count} channels")

    print("\nMissing values:")
    print(f"  Controls missing default_value: {missing_defaults}")
    print(f"  Controls missing min/max range: {missing_ranges}")
    print(f"  Controls missing parameter_change_format: {missing_change_format}")
    print(f"  Controls missing parameter_request_format: {missing_request_format}")

    if default_values:
        print("\nDefault value range:")
        print(f"  Min: {min(default_values)}")
        print(f"  Max: {max(default_values)}")

    # --- Optional sanity check: print samples from top groups ---
    top_groups = sorted(group_counts.items(), key=lambda x: -x[1])[:5]
    top_group_names = {g for g, _ in top_groups}

    for entry in data:
        if entry["control_group"] in top_group_names:
            print(f"\nSample entry for group {entry['control_group']}:")
            print(json.dumps(entry, indent=2))
            top_group_names.remove(entry["control_group"])
            if not top_group_names:
                break


# Example usage
summarize_sysex("m7cl_sysex_mappings.json")
