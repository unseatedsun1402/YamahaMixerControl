import json
from collections import defaultdict

def summarize_sysex(json_path):
    with open(json_path, "r") as f:
        data = json.load(f)

    total_controls = 0
    group_counts = defaultdict(int)
    missing_defaults = 0
    missing_ranges = 0
    missing_change_format = 0
    missing_request_format = 0

    default_values = []
    element_enums = []
    element_counts = []

    for entry in data:
        if entry.get("is_group_header"):
            continue

        total_controls += 1
        group_name = entry.get("name", "Unknown")
        group_counts[group_name] += 1

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

        if entry.get("element_enum") is not None:
            element_enums.append(entry["element_enum"])
        if entry.get("element_count") is not None:
            element_counts.append(entry["element_count"])

    print("=== Sysex Mapping Summary ===")
    print(f"Total controls: {total_controls}")
    print(f"Unique control groups: {len(group_counts)}")
    print("\nTop 5 largest groups:")
    for group, count in sorted(group_counts.items(), key=lambda x: -x[1])[:5]:
        print(f"  {group}: {count} controls")

    print("\nMissing values:")
    print(f"  Controls missing default_value: {missing_defaults}")
    print(f"  Controls missing min/max range: {missing_ranges}")
    print(f"  Controls missing parameter_change_format: {missing_change_format}")
    print(f"  Controls missing parameter_request_format: {missing_request_format}")

    if default_values:
        print("\nDefault value range:")
        print(f"  Min: {min(default_values)}")
        print(f"  Max: {max(default_values)}")

    if element_enums:
        print("\nElement enum range:")
        print(f"  Min: {min(element_enums)}")
        print(f"  Max: {max(element_enums)}")

    if element_counts:
        print("\nElement count range:")
        print(f"  Min: {min(element_counts)}")
        print(f"  Max: {max(element_counts)}")

# Example usage
summarize_sysex("sysex_mappings.json")
