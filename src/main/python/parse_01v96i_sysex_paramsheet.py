import json

import xlrd


def parse_01v96i_sysex(xls_path, output_json):
    workbook = xlrd.open_workbook(xls_path)
    sheet = workbook.sheet_by_index(1)

    mappings = []

    for row_idx in range(2, sheet.nrows):
        row = sheet.row_values(row_idx)

        if all(str(cell).strip() == "" for cell in row):
            continue

        # Update retained values if present
        if str(row[1]).strip():
            control_group = str(row[1]).strip()
        if str(row[2]).strip():
            control_id = safe_int(row[2])
        if str(row[1]).strip():
            max_channels = safe_int(row[3])

        sub_control = str(row[4]).strip()
        if not sub_control:
            continue  # skip group header rows

        value = safe_int(row[5])
        min_value = safe_int(row[6])
        max_value = safe_int(row[7])
        default_value = safe_int(row[8])
        comment = str(row[9]).strip() or "N/A"

        change_fmt = parse_bytes(row[10:23])  # K–AE
        request_fmt = parse_bytes(row[24:34])  # AF–AH

        if change_fmt and change_fmt[-1] != 247:
            change_fmt.append(247)
        if request_fmt and request_fmt[-1] != 247:
            request_fmt.append(247)

        # --- Precompute key from bytes 3–7 of the format ---
        # Use change_fmt if available, else request_fmt
        fmt = change_fmt if change_fmt else request_fmt
        if not fmt or len(fmt) < 8:
            continue

        model = safe_int(fmt[4])
        group = safe_int(fmt[5])
        addressA = safe_int(fmt[6])
        addressB = safe_int(fmt[7])

        key = (
            (model << 24)   |
            (group << 16)   |
            (addressA << 8) |
            addressB
        )
        
        priority = compute_priority(sub_control)

        mapping = {
            "control_group": control_group or "UnknownElement",
            "control_id": control_id if control_id is not None else None,
            "max_channels": (max_channels+1) if max_channels is not None else 1,
            "sub_control": sub_control,
            "value": value if value is not None else None,
            "min_value": min_value if min_value is not None else None,
            "max_value": max_value if max_value is not None else None,
            "default_value": default_value if default_value is not None else None,
            "comment": comment,
            "key": key,  # precomputed long key
            "address_bytes": [4,5,6,7],
            "index_bytes": [8],
            "parameter_change_format": change_fmt,
            "parameter_request_format": request_fmt,
            "priority":priority
        }
        mappings.append(mapping)

    with open(output_json, "w") as f:
        json.dump(mappings, f, indent=2)

    print(f"Parsed {len(mappings)} mappings (including synthetic meter blocks) into {output_json}")

def compute_priority(sub_control: str) -> int:
    sc = sub_control.lower()

    # Priority 1: faders and channel on/off
    if "kfader" in sc or "kchannelon" in sc:
        return 1

    # Priority 2: level/gain suffixes
    if sc.endswith("level") or sc.endswith("gain"):
        return 2

    # Default: priority 3
    return 3


def parse_bytes(cells):
    tokens = {"1n", "3n", "cc", "dd"}
    result = []
    for c in cells:
        c_str = str(c).strip()
        if not c_str:
            continue
        if c_str in tokens:
            result.append(c_str)
        else:
            try:
                result.append(int(c_str, 16))
            except ValueError:
                try:
                    result.append(int(c_str))
                except ValueError:
                    result.append("N/A")
    return result


def safe_int(val):
    try:
        return int(val)
    except:
        return None


# Example usage
parse_01v96i_sysex("01v96iParamChangeList.xls", "01v96i_sysex_mappings.json")
