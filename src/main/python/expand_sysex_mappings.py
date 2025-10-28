import json

def expand_sysex_mappings(json_path, output_path):
    with open(json_path, "r") as f:
        mappings = json.load(f)

    expanded = []

    for entry in mappings:
        if entry.get("is_group_header"):
            continue

        count = entry.get("element_count", 0)
        default_val = entry.get("default_value")
        default_val = int(default_val) if default_val is not None else 0

        num_channels = count if count > 0 else 1

        for channel in range(num_channels):
            expanded_mapping = {
                "name": entry["name"],
                "control_id": entry["control_id"],
                "channel_index": channel,
                "value": entry["value"],
                "min_value": entry["min_value"],
                "max_value": entry["max_value"],
                "default_value": entry["default_value"],
                "comment": entry["comment"],
                "parameter_change_format": interpolate(entry["parameter_change_format"], channel, default_val),
                "parameter_request_format": interpolate(entry["parameter_request_format"], channel, default_val)
            }
            expanded.append(expanded_mapping)

    with open(output_path, "w") as f:
        json.dump(expanded, f, indent=2)

    print(f"Generated {len(expanded)} expanded mappings into {output_path}")

def interpolate(format_list, channel_index, default_val):
    result = []
    cc_seen = 0
    for item in format_list:
        if item == "cc":
            if cc_seen == 0:
                result.append((channel_index >> 7) & 0x7F)  # MSB
            else:
                result.append(channel_index & 0x7F)          # LSB
            cc_seen += 1
        elif item == "dd":
            result.append(default_val)
        else:
            result.append(item)
    return result

# Example usage
expand_sysex_mappings("sysex_mappings.json", "sysex_expanded.json")
