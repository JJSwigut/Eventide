#!/usr/bin/env python3
import struct
import sys
import zlib


def read_chunks(data):
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG file")
    offset = 8
    while offset < len(data):
        length = struct.unpack(">I", data[offset : offset + 4])[0]
        chunk_type = data[offset + 4 : offset + 8]
        payload = data[offset + 8 : offset + 8 + length]
        yield chunk_type, payload
        offset += 12 + length


def paeth(left, up, up_left):
    predictor = left + up - up_left
    left_distance = abs(predictor - left)
    up_distance = abs(predictor - up)
    up_left_distance = abs(predictor - up_left)
    if left_distance <= up_distance and left_distance <= up_left_distance:
        return left
    if up_distance <= up_left_distance:
        return up
    return up_left


def unfilter_scanline(filter_type, scanline, previous, bytes_per_pixel):
    result = bytearray(scanline)
    for index, value in enumerate(scanline):
        left = result[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
        up = previous[index] if previous else 0
        up_left = previous[index - bytes_per_pixel] if previous and index >= bytes_per_pixel else 0
        if filter_type == 0:
            result[index] = value
        elif filter_type == 1:
            result[index] = (value + left) & 0xFF
        elif filter_type == 2:
            result[index] = (value + up) & 0xFF
        elif filter_type == 3:
            result[index] = (value + ((left + up) // 2)) & 0xFF
        elif filter_type == 4:
            result[index] = (value + paeth(left, up, up_left)) & 0xFF
        else:
            raise ValueError(f"unsupported PNG filter: {filter_type}")
    return bytes(result)


def decode_png(path):
    data = open(path, "rb").read()
    width = height = color_type = bit_depth = None
    compressed = bytearray()
    for chunk_type, payload in read_chunks(data):
        if chunk_type == b"IHDR":
            width, height, bit_depth, color_type = struct.unpack(">IIBB", payload[:10])
        elif chunk_type == b"IDAT":
            compressed.extend(payload)
    if bit_depth != 8 or color_type not in (0, 2, 4, 6):
        raise ValueError(f"unsupported PNG format: bit_depth={bit_depth}, color_type={color_type}")

    bytes_per_pixel = {0: 1, 2: 3, 4: 2, 6: 4}[color_type]
    row_bytes = width * bytes_per_pixel
    raw = zlib.decompress(bytes(compressed))
    rows = []
    previous = None
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        scanline = raw[offset : offset + row_bytes]
        offset += row_bytes
        row = unfilter_scanline(filter_type, scanline, previous, bytes_per_pixel)
        rows.append(row)
        previous = row
    return width, height, color_type, bytes_per_pixel, rows


def main(path):
    width, height, color_type, bytes_per_pixel, rows = decode_png(path)
    total = width * height
    step = max(1, total // 10000)
    sampled = 0
    non_dark = 0
    colors = set()

    linear_index = 0
    for row in rows:
        for offset in range(0, len(row), bytes_per_pixel):
            if linear_index % step == 0:
                if color_type == 0:
                    red = green = blue = row[offset]
                else:
                    red, green, blue = row[offset], row[offset + 1], row[offset + 2]
                luminance = (0.2126 * red) + (0.7152 * green) + (0.0722 * blue)
                if luminance > 24:
                    non_dark += 1
                colors.add((red, green, blue))
                sampled += 1
            linear_index += 1

    non_dark_ratio = non_dark / max(1, sampled)
    if width < 100 or height < 100:
        raise ValueError(f"screenshot is unexpectedly small: {width}x{height}")
    if non_dark_ratio < 0.05:
        raise ValueError(f"screenshot is mostly dark or blank: non_dark_ratio={non_dark_ratio:.3f}")
    if len(colors) < 8:
        raise ValueError(f"screenshot has too little visual variation: colors={len(colors)}")

    print(
        f"Screenshot check passed: {width}x{height}, "
        f"non_dark_ratio={non_dark_ratio:.3f}, sampled_colors={len(colors)}"
    )


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: eventide_assert_screenshot.py <screenshot.png>", file=sys.stderr)
        sys.exit(2)
    try:
        main(sys.argv[1])
    except Exception as error:
        print(f"Screenshot check failed: {error}", file=sys.stderr)
        sys.exit(1)
