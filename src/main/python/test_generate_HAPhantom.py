import json


def build_mapping(
    name,
    control_id,
    channel_index,
    comment,
    model_id=62,
    addr_bytes=[17, 1, 0, 41],
    min_value=0,
    max_value=1,
    default_value=0,
):
    """
    Build a JSON mapping entry for a given control and channel.
    - name: parameter group (e.g. "kInputHA")
    - control_id: control name (e.g. "kHAPhantom")
    - channel_index: integer channel index
    - comment: string description of values
    - model_id: defaults to 62 (0x3E)
    - addr_bytes: list of address bytes for the control
    """

    # Fixed prefix up to channel index
    prefix = [240, 43, "1n", model_id] + addr_bytes + [0, 0, 0, channel_index]

    # Parameter change format: prefix + 4 data tokens + terminator
    parameter_change_format = prefix + ["dd", "dd", "dd", "dd", 247]

    # Parameter request format: same prefix but with "3n" token and no data bytes
    parameter_request_format = prefix.copy()
    parameter_request_format[2] = "3n"
    parameter_request_format.append(247)

    return {
        "name": name,
        "control_id": control_id,
        "channel_index": channel_index,
        "min_value": min_value,
        "max_value": max_value,
        "default_value": default_value,
        "comment": comment,
        "parameter_change_format": parameter_change_format,
        "parameter_request_format": parameter_request_format,
    }


def generate_all_channels(
    name,
    control_id,
    comment,
    model_id=62,
    addr_bytes=[17, 1, 0, 41],
    min_value=0,
    max_value=1,
    default_value=0,
    max_channel=56,
):
    """
    Generate mappings for all channels from 1 to max_channel.
    """
    entries = []
    for ch in range(1, max_channel + 1):
        entry = build_mapping(
            name=name,
            control_id=control_id,
            channel_index=ch,
            comment=comment,
            model_id=model_id,
            addr_bytes=addr_bytes,
            min_value=min_value,
            max_value=max_value,
            default_value=default_value,
        )
        entries.append(entry)
    return entries


# Example: generate Phantom Power mappings for channels 1–64
phantom_entries = generate_all_channels(
    name="kInputHA", control_id="kHAPhantom", comment="Off, On", max_channel=64
)

# Save to JSON file
with open("phantom_power_mappings.json", "w") as f:
    json.dump(phantom_entries, f, indent=2)
