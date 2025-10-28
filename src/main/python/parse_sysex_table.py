import xlrd
import json

def parse_sysex_table(xls_path, output_json):
    workbook = xlrd.open_workbook(xls_path)
    sheet = workbook.sheet_by_index(1)

    mappings = []

    current_name = ""
    current_enum = None
    current_count = None

    for row_idx in range(2, sheet.nrows):
        row = sheet.row_values(row_idx)

        # Skip empty rows
        if all(str(cell).strip() == "" for cell in row):
            continue

        

        # Update retained values if present
        if str(row[1]).strip():
            current_name = str(row[1]).strip()
        if str(row[2]).strip():
            current_enum = safe_int(row[2])
        if str(row[3]).strip():
            current_count = safe_int(row[3])
        
        # if not str(row[4]).strip():
        #     continue  # skip group header rows

        mapping = {
            "name": current_name,
            "element_enum": current_enum,
            "element_count": current_count,
            "control_id": str(row[4]).strip(),
            "value": safe_int(row[5]),
            "min_value": safe_int(row[6]),
            "max_value": safe_int(row[7]),
            "default_value": safe_int(row[8]),
            "comment": str(row[9]).strip(),
            "parameter_change_format": parse_bytes(row[10:28]),
            "parameter_request_format": parse_bytes(row[28:41]),
            "is_group_header": not bool(str(row[4]).strip())
        }

        mappings.append(mapping)


    with open(output_json, "w") as f:
        json.dump(mappings, f, indent=2)

    print(f"✅ Parsed {len(mappings)} rows into {output_json}")

def parse_bytes(cells):
    tokens = {"1n", "3n", "cc", "dd"}
    result = []
    for c in cells:
        c_str = str(c).strip()
        if c_str == "":
            continue
        if c_str in tokens:
            result.append(c_str)
        else:
            try:
                # Try decimal first
                result.append(int(float(c_str)))
            except ValueError:
                try:
                    # Try hex (e.g. "F0" → 240)
                    result.append(int(c_str, 16))
                except ValueError:
                    print(f"Warning: Unrecognized token '{c_str}'")
                    result.append(c_str)  # fallback
    return result

def safe_int(val):
    try:
        return int(val)
    except:
        return 

def safe_float(val):
    try:
        return float(val)
    except:
        return None



# Example usage
parse_sysex_table("ParamChangeList_V350_M7CL.xls", "sysex_mappings.json")

