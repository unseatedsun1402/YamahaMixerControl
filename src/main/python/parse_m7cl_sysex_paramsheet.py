import xlrd
import json

def parse_sysex_table(xls_path, output_json):
    workbook = xlrd.open_workbook(xls_path)
    sheet = workbook.sheet_by_index(1)

    mappings = []

    control_group = ""
    control_id = None

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

        value = safe_int(row[5])
        min_value = safe_int(row[6])
        max_value = safe_int(row[7])
        default_value = safe_int(row[8])
        comment = str(row[9]).strip() or "N/A"

        # Parse formats
        change_fmt = parse_bytes(row[10:28])
        request_fmt = parse_bytes(row[28:41])

        # Replace trailing zeros in change format with "dd" tokens
        if len(change_fmt) > 6 and change_fmt[-6] == 0:
            change_fmt[-6:] = ["66","dd", "dd", "dd", "dd", 247]

        # Ensure request format ends with F7
        if request_fmt and request_fmt[-1] != 247:
            request_fmt.append(247)

        # --- Precompute key from bytes 3–7 of the format ---
        fmt = change_fmt if change_fmt else request_fmt
        if not fmt or len(fmt) < 8:
            continue

        model         = safe_int(fmt[3])
        system_scope  = safe_int(fmt[4])
        control_group_val = safe_int(fmt[5])
        sub_control_val   = safe_int(fmt[6])
        param         = safe_int(fmt[7])

        key = ((model << 32) |
               (system_scope << 24) |
               (control_group_val << 16) |
               (sub_control_val << 8) |
               param)

        mapping = {
            "control_group": control_group or "UnknownElement",
            "control_id": control_id,
            "max_channels": (max_channels+1) if max_channels is not None else 1,
            "sub_control": sub_control,
            "value": value,
            "min_value": min_value,
            "max_value": max_value,
            "default_value": default_value,
            "comment": comment,
            "key": key,  # precomputed long key
            "parameter_change_format": change_fmt,
            "parameter_request_format": request_fmt
        }
        mappings.append(mapping)

    with open(output_json, "w") as f:
        json.dump(mappings, f, indent=2)

    print(f"Parsed {len(mappings)} unique control type mappings into {output_json}")


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
                    print(f"Warning: Unrecognized token '{c_str}'")
                    result.append(c_str)
    return result

def safe_int(val):
    try:
        return int(val)
    except:
        return None

# Example usage
parse_sysex_table("ParamChangeList_V350_M7CL.xls", "m7cl_sysex_mappings.json")