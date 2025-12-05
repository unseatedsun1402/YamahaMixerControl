import xlrd
import json

def parse_sysex_table(xls_path, output_json):
    workbook = xlrd.open_workbook(xls_path)
    sheet = workbook.sheet_by_index(1)

    mappings = []

    control_group = ""
    control_id = None
    max_channel = None

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
        if str(row[3]).strip():
            max_channel = safe_int(row[3])

        sub_control = str(row[4]).strip()
        if not sub_control:
            continue  # skip group header rows

        value = safe_int(row[5])
        min_value = safe_int(row[6])
        max_value = safe_int(row[7])
        default_value = safe_int(row[8])
        comment = str(row[9]).strip()

        # Parse formats
        change_fmt = parse_bytes(row[10:28])
        request_fmt = parse_bytes(row[28:41])

        # Replace trailing zeros in change format with "dd" tokens
        # Typically last 4 data bytes before F7
        if len(change_fmt) > 6 and change_fmt[-5] == 0:
            change_fmt[-5:] = ["dd", "dd", "dd", "dd", 247]

        # Ensure request format ends with F7
        if request_fmt and request_fmt[-1] != 247:
            request_fmt.append(247)

        # Expand per-channel entries if element_count is set
        channel_count = max_channel if max_channel else 1
        for ch in range(1, channel_count + 1):
            # Replace channel index placeholder "cc" with actual channel number
            param_change_fmt = expand_format(change_fmt, ch)
            param_request_fmt = expand_format(request_fmt, ch)



            mapping = {
                "control_group": control_group,
                "control_id": control_id,
                "sub_control": sub_control,
                "channel_index": ch if channel_count > 1 else None,
                "value": value,
                "min_value": min_value,
                "max_value": max_value,
                "default_value": default_value,
                "comment": comment,
                "parameter_change_format": param_change_fmt,
                "parameter_request_format": param_request_fmt
            }
            mappings.append(mapping)

    with open(output_json, "w") as f:
        json.dump(mappings, f, indent=2)

    print(f"Parsed {len(mappings)} mappings into {output_json}")


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
                # interpret as hex string, convert to decimal
                result.append(int(c_str, 16))
            except ValueError:
                try:
                    # fallback: interpret as decimal if not valid hex
                    result.append(int(c_str))
                except ValueError:
                    print(f"Warning: Unrecognized token '{c_str}'")
                    result.append(c_str)
    return result

def expand_channel_index(n):
    return list(n.to_bytes(2, byteorder='big'))

def expand_format(fmt, ch):
    result = []
    i = 0
    while i < len(fmt):
        tok = fmt[i]
        if tok == "cc":
            # collapse consecutive "cc" tokens into one 2-byte expansion
            if i+1 < len(fmt) and fmt[i+1] == "cc":
                result.extend(ch.to_bytes(2, "big"))
                i += 2
                continue
            else:
                # single cc (shouldn't happen in datasheet, but handle gracefully)
                result.extend(ch.to_bytes(2, "big"))
        else:
            result.append(tok)
        i += 1
    return result

def safe_int(val):
    try:
        return int(val)
    except:
        return None


# Example usage
parse_sysex_table("ParamChangeList_V350_M7CL.xls", "sysex_mappings.json")