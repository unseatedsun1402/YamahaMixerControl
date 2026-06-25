#!/usr/bin/env python3

# ------------------------------------------------------------
# M7CL name short chunks:
#
# 5 MIDI-safe 7-bit bytes encode 4 ASCII bytes.
#
# Encoding:
#   n = c0 c1 c2 c3 as a 32-bit big-endian integer
#
#   dd0 = (n >> 28) & 0x7F
#   dd1 = (n >> 21) & 0x7F
#   dd2 = (n >> 14) & 0x7F
#   dd3 = (n >>  7) & 0x7F
#   dd4 =  n        & 0x7F
#
# Decoding:
#   n = dd0<<28 | dd1<<21 | dd2<<14 | dd3<<7 | dd4
#   chars = n.to_bytes(4, "big")
# ------------------------------------------------------------


def hex_to_bytes(s):
    return [int(x, 16) for x in s.replace(",", " ").split()]


def bytes_to_hex(values):
    return " ".join("{:02X}".format(v & 0xFF) for v in values)


def tail5_from_sysex_hex(s):
    b = hex_to_bytes(s)

    if len(b) >= 2 and b[0] == 0xF0 and b[-1] == 0xF7:
        b = b[1:-1]

    return b[-5:]


def decode_m7cl_chunk(dd):
    if len(dd) != 5:
        raise ValueError("M7CL chunk must be exactly 5 bytes")

    n = 0

    for b in dd:
        n = (n << 7) | (b & 0x7F)

    raw = n.to_bytes(4, "big")

    # Yamaha appears to use NUL padding in some responses.
    # Convert NUL to spaces for display.
    text = "".join(chr(x) if x != 0 else " " for x in raw)

    return text


def encode_m7cl_chunk(text):
    if len(text) > 4:
        raise ValueError("One M7CL chunk can contain at most 4 characters")

    padded = text.ljust(4, "\x00")
    raw = padded.encode("ascii", errors="replace")

    n = int.from_bytes(raw, "big")

    return [
        (n >> 28) & 0x7F,
        (n >> 21) & 0x7F,
        (n >> 14) & 0x7F,
        (n >> 7) & 0x7F,
        n & 0x7F,
    ]


def decode_m7cl_name(short1_tail, short2_tail):
    part1 = decode_m7cl_chunk(short1_tail)
    part2 = decode_m7cl_chunk(short2_tail)

    return (part1 + part2).rstrip(" \x00")


def test_known_examples():
    short1 = [0x07, 0x23, 0x15, 0x66, 0x74]
    short2 = [0x03, 0x08, 0x00, 0x00, 0x00]

    print("Known test1 example")
    print("short1:", bytes_to_hex(short1), "=>", repr(decode_m7cl_chunk(short1)))
    print("short2:", bytes_to_hex(short2), "=>", repr(decode_m7cl_chunk(short2)))
    print("name:  ", repr(decode_m7cl_name(short1, short2)))
    print()

    for text in ["test", "1", "aaaa", "bbbb", "AAAA", "BBBB", "1111", "0000", "....", "----"]:
        encoded = encode_m7cl_chunk(text)
        decoded = decode_m7cl_chunk(encoded)

        print(
            repr(text),
            "=>",
            bytes_to_hex(encoded),
            "=>",
            repr(decoded)
        )


def decode_sysex_pair(short1_hex, short2_hex):
    short1_tail = tail5_from_sysex_hex(short1_hex)
    short2_tail = tail5_from_sysex_hex(short2_hex)

    print("short1 tail:", bytes_to_hex(short1_tail))
    print("short1 text:", repr(decode_m7cl_chunk(short1_tail)))

    print("short2 tail:", bytes_to_hex(short2_tail))
    print("short2 text:", repr(decode_m7cl_chunk(short2_tail)))

    print("full name:  ", repr(decode_m7cl_name(short1_tail, short2_tail)))


def main():
    test_known_examples()

    print()
    print("Decode your captured test1 SysEx pair")
    print()

    short1_hex = "F0 43 10 3E 11 01 01 19 00 00 00 01 05 1A 41 30 00 F7"
    short2_hex = "F0 43 10 3E 11 01 01 19 00 00 00 03 04 6A 25 08 49 F7"

    decode_sysex_pair(short1_hex, short2_hex)


if __name__ == "__main__":
    main()