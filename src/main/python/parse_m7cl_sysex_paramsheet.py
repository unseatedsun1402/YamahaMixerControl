import xlrd
import re
import json

def parse_sysex_table(xls_path, output_json):
    workbook = xlrd.open_workbook(xls_path)
    sheet = workbook.sheet_by_index(1)

    mappings = []

    control_group = ""
    control_id = None
    max_channels = None

    for row_idx in range(2, sheet.nrows):
        row = sheet.row_values(row_idx)

        # Skip empty rows
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

        # --- GHOST FILTER ---
        is_ghost, reason = is_ghost_mapping(control_group, sub_control)
        if is_ghost:
            print(f"[DROP] {sub_control} ({control_group}) -> {reason}")
            continue

        value = safe_int(row[5])
        min_value = safe_int(row[6])
        max_value_val = safe_int(row[7])
        default_value = safe_int(row[8])
        comment = str(row[9]).strip() or "N/A"

        is_used = re.search(r"not used",comment.lower(),)
        if is_used:
            print ("[DROP] "+control_group+"."+sub_control)
            continue

        # Parse formats
        change_fmt = parse_bytes(row[10:28])
        request_fmt = parse_bytes(row[28:41])

        # Replace trailing zeros in change format with "dd" tokens
        if len(change_fmt) > 6 and change_fmt[-6] == 0:
            change_fmt[-6:] = ["66", "dd", "dd", "dd", "dd", 247]

        # Ensure request format ends with F7
        if request_fmt and request_fmt[-1] != 247:
            request_fmt.append(247)

        # --- Precompute key from bytes 4–9 of the format (M7CL: 6-byte identity) ---
        fmt = change_fmt if change_fmt else request_fmt
        if not fmt or len(fmt) < 10:
            # Need at least indices 0..9 to safely read fmt[9]
            continue

        model    = safe_int(fmt[4])
        group    = safe_int(fmt[5])
        addressA = safe_int(fmt[6])
        addressB = safe_int(fmt[7])
        addressC = safe_int(fmt[8])
        addressD = safe_int(fmt[9])

        # Skip rows with invalid identity bytes
        if None in (model, group, addressA, addressB, addressC, addressD):
            print(f"Skipping row due to invalid identity bytes: {fmt}")
            continue

        key = (
            (model    << 40) |
            (group    << 32) |
            (addressA << 24) |
            (addressB << 16) |
            (addressC << 8)  |
            addressD
        )

        priority = compute_priority(sub_control)

        mapping = {
            "control_group": control_group or "UnknownElement",
            "control_id": control_id,
            "max_channels": (max_channels + 1) if max_channels is not None else 1,
            "sub_control": sub_control,
            "value": value,
            "min_value": min_value,
            "max_value": max_value_val,
            "default_value": default_value,
            "comment": comment,
            "key": key,
            "address_bytes": [4,5,6,7,8,9],
            "index_bytes": [10,11],
            "parameter_change_format": change_fmt,
            "parameter_request_format": request_fmt,
            "priority": priority,
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
    if sc.endswith("level") or sc.endswith("gain") or "nameshort" in sc:
        return 2

    # Default: priority 3
    return 3


DROP_IF_CONTAINS = [
    "aux",          # nukes entire AUX domain
    "surr",     # no surround engine
]

DROP_IF_REGEX = [
    r"mix(1[7-9]|[2-9][0-9])",   # MIX > 16
    r"matrix(9|1[0-6])",         # Matrix > 8
    r"fx([5-9]|1[0-6])",         # FX > 4
]

CONDITIONAL_DROP = [
    ("automix", "aux"),
    ("cascade", "aux"),
    ("monitor", "aux"),
    ("hui", "aux"),
    ("nuendo", "aux"),
]


def is_ghost_mapping(control_group: str, sub_control: str):
    cg = (control_group or "").lower()
    sc = (sub_control or "").lower()

    # --- 1. HARD substring kills ---
    for token in DROP_IF_CONTAINS:
        if token in cg or token in sc:
            return True, f"contains '{token}'"

    # --- 2. REGEX-based limits (mix/matrix/fx bounds) ---
    for pattern in DROP_IF_REGEX:
        if re.search(pattern, sc):
            return True, f"regex '{pattern}'"

    # --- 3. CONDITIONAL rules (scope-aware) ---
    for domain, trigger in CONDITIONAL_DROP:
        if domain in cg or domain in sc:
            if trigger in cg or trigger in sc:
                return True, f"{domain} with {trigger}"

    return False, None

def parse_bytes(cells):
    tokens = {"1n", "3n", "cc", "dd"}
    result = []
    for c in cells:
        c_str = str(c).strip()
        if not c_str:
            continue
        if c_str in tokens:
            result.append(c_str)
            continue

        # Try hex
        try:
            result.append(int(c_str, 16))
            continue
        except Exception:
            pass

        # Try float → int (handles "13.0", 13.0, etc.)
        try:
            result.append(int(float(c_str)))
            continue
        except Exception:
            pass

        print(f"Warning: Unrecognized token '{c_str}'")
        result.append(None)
    return result


def safe_int(val):
    if val is None:
        return None
    try:
        if isinstance(val, float):
            return int(val)
        if isinstance(val, str):
            s = val.strip()
            # Handle "13.0" style strings
            try:
                return int(float(s))
            except Exception:
                return None
        return int(val)
    except Exception:
        return None

# Example usage
parse_sysex_table("ParamChangeList_V350_M7CL.xls", "m7cl_sysex_mappings.json")