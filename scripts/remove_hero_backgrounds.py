#!/usr/bin/env python3
import glob
import os
import struct
import zlib
from collections import deque

def unfilter_scanlines(raw, width, height, bpp):
    prev_line = [0] * (width * bpp)
    out = []
    pos = 0
    for y in range(height):
        filter_type = raw[pos]
        pos += 1
        line = list(raw[pos:pos+width*bpp])
        pos += width*bpp
        recon = [0] * (width * bpp)
        if filter_type == 0: recon = line
        elif filter_type == 1:
            for x in range(width * bpp):
                a = recon[x - bpp] if x >= bpp else 0
                recon[x] = (line[x] + a) & 0xff
        elif filter_type == 2:
            for x in range(width * bpp):
                b = prev_line[x]
                recon[x] = (line[x] + b) & 0xff
        elif filter_type == 3:
            for x in range(width * bpp):
                a = recon[x - bpp] if x >= bpp else 0
                b = prev_line[x]
                recon[x] = (line[x] + ((a + b) >> 1)) & 0xff
        elif filter_type == 4:
            for x in range(width * bpp):
                a = recon[x - bpp] if x >= bpp else 0
                b = prev_line[x]
                c = prev_line[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                recon[x] = (line[x] + pr) & 0xff
        prev_line = recon
        out.append(recon)
    return out

def write_png(filename, width, height, grid):
    def chunk(chunk_type, data):
        c = chunk_type + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    raw_data = bytearray()
    for row in grid:
        raw_data.append(0) # filter 0
        for pixel in row:
            raw_data.extend(pixel)
    
    compressed = zlib.compress(bytes(raw_data), 9)
    out = bytearray(b'\x89PNG\r\n\x1a\n')
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    out.extend(chunk(b'IHDR', ihdr_data))
    out.extend(chunk(b'IDAT', compressed))
    out.extend(chunk(b'IEND', b''))
    with open(filename, 'wb') as f:
        f.write(out)

def is_bg(r, g, b, a, x, y, w, h):
    if a < 35: return True
    # Frame/crop edge line artifacts on perimeter
    if (x <= 6 or x >= w - 7 or y <= 4 or y >= h - 5):
        if r <= 160 and g <= 155 and b <= 145 and (abs(r - g) < 25 and abs(g - b) < 25):
            return True
        if r <= 28 and g <= 35 and b <= 40:
            return True
    # Cold dark slate/blue canvas background
    if (r <= 26 and g <= 36 and b <= 48 and b >= r + 4):
        return True
    if (r <= 18 and g <= 24 and b <= 32):
        return True
    return False

def clean_hero_asset(path):
    with open(path, 'rb') as f: data = f.read()
    pos = 8
    idats = []
    while pos < len(data):
        length, chunk_type = struct.unpack('>I4s', data[pos:pos+8])
        chunk_data = data[pos+8:pos+8+length]
        pos += 8 + length + 4
        if chunk_type == b'IHDR': width, height = struct.unpack('>II', chunk_data[:8])
        elif chunk_type == b'IDAT': idats.append(chunk_data)
        elif chunk_type == b'IEND': break
    
    rows = unfilter_scanlines(zlib.decompress(b''.join(idats)), width, height, 4)
    grid = [[rows[y][x*4:(x+1)*4] for x in range(width)] for y in range(height)]
    
    visited = set()
    queue = deque()
    for y in range(height):
        for x in [0, width - 1]:
            if is_bg(*grid[y][x], x, y, width, height):
                visited.add((x, y))
                queue.append((x, y))
    for x in range(width):
        for y in [0, height - 1]:
            if (x, y) not in visited and is_bg(*grid[y][x], x, y, width, height):
                visited.add((x, y))
                queue.append((x, y))
    
    while queue:
        cx, cy = queue.popleft()
        grid[cy][cx] = [0, 0, 0, 0]
        for nx, ny in [(cx+1, cy), (cx-1, cy), (cx, cy+1), (cx, cy-1)]:
            if 0 <= nx < width and 0 <= ny < height and (nx, ny) not in visited:
                if is_bg(*grid[ny][nx], nx, ny, width, height):
                    visited.add((nx, ny))
                    queue.append((nx, ny))
    
    # Remove small disconnected components (< 50 pixels)
    comp_visited = set()
    for y in range(height):
        for x in range(width):
            if grid[y][x][3] > 0 and (x, y) not in comp_visited:
                comp = []
                cqueue = deque([(x, y)])
                comp_visited.add((x, y))
                while cqueue:
                    kx, ky = cqueue.popleft()
                    comp.append((kx, ky))
                    for dx, dy in [(1,0),(-1,0),(0,1),(0,-1),(1,1),(1,-1),(-1,1),(-1,-1)]:
                        mx, my = kx + dx, ky + dy
                        if 0 <= mx < width and 0 <= my < height and (mx, my) not in comp_visited:
                            if grid[my][mx][3] > 0:
                                comp_visited.add((mx, my))
                                cqueue.append((mx, my))
                if len(comp) < 50:
                    for px, py in comp:
                        grid[py][px] = [0, 0, 0, 0]
    
    write_png(path, width, height, grid)
    print(f"Successfully cleaned: {os.path.basename(path)}")

if __name__ == "__main__":
    assets = sorted(glob.glob("app/src/main/res/drawable/hero_*.png"))
    for asset in assets:
        clean_hero_asset(asset)
    print(f"All {len(assets)} hero assets cleaned and saved with true transparent background.")
