#
# png.jl
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#

# A PNG decoder, included into the RStudio module. Julia's plotting packages
# render images as PNG; decoding them here, into the pixels R's nativeRaster
# holds, lets R draw them in the Plots pane without a PNG reader of its own.

import Zlib_jll

const PNG_SIGNATURE = UInt8[0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]

# Samples per pixel, by color type: gray, RGB, palette index, gray and alpha,
# RGBA; and the bit depths each type allows.
const PNG_CHANNELS = Dict(0 => 1, 2 => 3, 3 => 1, 4 => 2, 6 => 4)
const PNG_DEPTHS = Dict(0 => (1, 2, 4, 8, 16), 2 => (8, 16), 3 => (1, 2, 4, 8), 4 => (8, 16), 6 => (8, 16))

"""
    decode_png(bytes) -> (pixels, width, height)

Decode a PNG image into one `Int32` per pixel, row by row from the top, packed
as R's nativeRaster packs them: red in the lowest byte, then green, blue and
alpha. Interlaced images aren't supported; plotting packages don't write them.
"""
function decode_png(bytes::AbstractVector{UInt8})
   if length(bytes) < 8 || view(bytes, 1:8) != PNG_SIGNATURE
      throw(ArgumentError("not a PNG image"))
   end

   header = UInt8[]
   palette = UInt8[]
   transparency = UInt8[]
   compressed = UInt8[]

   # chunks: a length, a type, the data, and a CRC (not checked)
   pos = 9
   while pos + 7 <= length(bytes)
      len = Int(read_be32(bytes, pos))
      type = String(bytes[pos + 4:pos + 7])
      data_end = pos + 7 + len
      if data_end + 4 > length(bytes)
         throw(ArgumentError("truncated PNG image"))
      end

      data = view(bytes, pos + 8:data_end)
      pos = data_end + 5

      if type == "IHDR"
         header = collect(data)
      elseif type == "PLTE"
         palette = collect(data)
      elseif type == "tRNS"
         transparency = collect(data)
      elseif type == "IDAT"
         append!(compressed, data)
      elseif type == "IEND"
         break
      end
   end

   if length(header) != 13
      throw(ArgumentError("PNG image without a valid header"))
   end

   width = Int(read_be32(header, 1))
   height = Int(read_be32(header, 5))
   depth = Int(header[9])
   color = Int(header[10])

   if width == 0 || height == 0
      throw(ArgumentError("empty PNG image"))
   elseif !(depth in get(PNG_DEPTHS, color, ()))
      throw(ArgumentError("invalid PNG color type $color with bit depth $depth"))
   elseif header[11] != 0 || header[12] != 0
      throw(ArgumentError("unknown PNG compression or filter method"))
   elseif header[13] != 0
      throw(ArgumentError("interlaced PNG images aren't supported"))
   end

   bits = PNG_CHANNELS[color] * depth    # per pixel
   stride = cld(width * bits, 8)         # bytes per row, after its filter byte
   step = max(1, bits >> 3)              # bytes from a sample to the last pixel's

   raw = inflate(compressed, height * (stride + 1))
   rows = unfilter(raw, height, stride, step)
   pixels = to_native_raster(rows, width, height, stride, depth, color, palette, transparency)

   return pixels, width, height
end

read_be32(bytes, i) =
   (UInt32(bytes[i]) << 24) | (UInt32(bytes[i + 1]) << 16) | (UInt32(bytes[i + 2]) << 8) | UInt32(bytes[i + 3])

read_be16(bytes, i) = (Int(bytes[i]) << 8) | Int(bytes[i + 1])

# The image data, inflated with the zlib Julia ships; its size is known from
# the header.
function inflate(compressed::Vector{UInt8}, size::Int)
   inflated = Vector{UInt8}(undef, size)
   inflated_size = Ref{Culong}(size)
   status = ccall(
      (:uncompress, Zlib_jll.libz),
      Cint,
      (Ptr{UInt8}, Ref{Culong}, Ptr{UInt8}, Culong),
      inflated,
      inflated_size,
      compressed,
      length(compressed),
   )

   if status != 0 || inflated_size[] != size
      throw(ArgumentError("corrupt PNG image data"))
   end
   return inflated
end

# Each byte of a row is stored as its difference from a prediction, made from
# the bytes a pixel to its left, above it, and above that one; the row's first
# byte says which prediction.
function unfilter(raw::Vector{UInt8}, height::Int, stride::Int, step::Int)
   rows = Vector{UInt8}(undef, height * stride)
   for y in 0:height - 1
      input = y * (stride + 1) + 1    # the row's filter byte
      output = y * stride             # the row's start, less one
      kind = raw[input]
      if kind > 4
         throw(ArgumentError("unknown PNG filter type $kind"))
      end

      for i in 1:stride
         left = i > step ? rows[output + i - step] : 0x00
         up = y > 0 ? rows[output + i - stride] : 0x00
         upleft = i > step && y > 0 ? rows[output + i - step - stride] : 0x00

         byte = raw[input + i]
         if kind == 1
            byte += left
         elseif kind == 2
            byte += up
         elseif kind == 3
            byte += UInt8((UInt16(left) + UInt16(up)) >> 1)
         elseif kind == 4
            byte += paeth(left, up, upleft)
         end
         rows[output + i] = byte
      end
   end
   return rows
end

# Whichever of the neighbours is closest to left + up - upleft.
function paeth(left::UInt8, up::UInt8, upleft::UInt8)
   p = Int(left) + Int(up) - Int(upleft)
   pl, pu, pul = abs(p - Int(left)), abs(p - Int(up)), abs(p - Int(upleft))
   if pl <= pu && pl <= pul
      return left
   elseif pu <= pul
      return up
   else
      return upleft
   end
end

# The `i`th sample (counting from 0) of the row that follows byte `offset`.
@inline function read_sample(rows::Vector{UInt8}, offset::Int, i::Int, depth::Int)
   if depth == 8
      return Int(rows[offset + i + 1])
   elseif depth == 16
      return (Int(rows[offset + 2i + 1]) << 8) | Int(rows[offset + 2i + 2])
   else
      bit = i * depth
      byte = rows[offset + (bit >> 3) + 1]
      return Int(byte >> (8 - depth - (bit & 7))) & ((1 << depth) - 1)
   end
end

# A sample in 8 bits: 16-bit samples keep their high byte, and smaller ones
# are stretched over 0 to 255.
@inline function to_8bit(value::Int, depth::Int)
   if depth == 16
      return value >> 8
   elseif depth == 8
      return value
   else
      return div(value * 255, (1 << depth) - 1)
   end
end

@inline pack_rgba(r::Int, g::Int, b::Int, a::Int) =
   reinterpret(Int32, UInt32(r) | (UInt32(g) << 8) | (UInt32(b) << 16) | (UInt32(a) << 24))

function to_native_raster(rows, width, height, stride, depth, color, palette, transparency)
   # a gray or RGB image's tRNS chunk names one color as transparent, in
   # 16-bit values whatever the depth
   key = if color == 0 && length(transparency) >= 2
      (read_be16(transparency, 1), -1, -1)
   elseif color == 2 && length(transparency) >= 6
      (read_be16(transparency, 1), read_be16(transparency, 3), read_be16(transparency, 5))
   else
      (-1, -1, -1)
   end

   pixels = Vector{Int32}(undef, width * height)
   for y in 0:height - 1
      offset = y * stride
      for x in 0:width - 1
         if color == 0
            v = read_sample(rows, offset, x, depth)
            r = g = b = to_8bit(v, depth)
            a = v == key[1] ? 0 : 255
         elseif color == 2
            vr = read_sample(rows, offset, 3x, depth)
            vg = read_sample(rows, offset, 3x + 1, depth)
            vb = read_sample(rows, offset, 3x + 2, depth)
            r, g, b = to_8bit(vr, depth), to_8bit(vg, depth), to_8bit(vb, depth)
            a = (vr, vg, vb) == key ? 0 : 255
         elseif color == 3
            index = read_sample(rows, offset, x, depth)
            if 3index + 3 > length(palette)
               throw(ArgumentError("PNG palette index out of range"))
            end
            r, g, b = Int(palette[3index + 1]), Int(palette[3index + 2]), Int(palette[3index + 3])
            a = index < length(transparency) ? Int(transparency[index + 1]) : 255
         elseif color == 4
            r = g = b = to_8bit(read_sample(rows, offset, 2x, depth), depth)
            a = to_8bit(read_sample(rows, offset, 2x + 1, depth), depth)
         else
            r = to_8bit(read_sample(rows, offset, 4x, depth), depth)
            g = to_8bit(read_sample(rows, offset, 4x + 1, depth), depth)
            b = to_8bit(read_sample(rows, offset, 4x + 2, depth), depth)
            a = to_8bit(read_sample(rows, offset, 4x + 3, depth), depth)
         end
         pixels[y * width + x + 1] = pack_rgba(r, g, b, a)
      end
   end
   return pixels
end
