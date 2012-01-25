/*
 * Copyright 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.typedarrays.client;

import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.DataView;
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Float64Array;
import com.google.gwt.typedarrays.shared.Int16Array;
import com.google.gwt.typedarrays.shared.Int32Array;
import com.google.gwt.typedarrays.shared.Uint16Array;
import com.google.gwt.typedarrays.shared.Uint32Array;

/**
 * JS native implementation of {@link DataView} for platforms with typed array
 * support but missing DataView (ie, Firefox).
 */
public class DataViewNativeEmul implements DataView {

  private static final boolean nativeLittleEndian;

  static {
    nativeLittleEndian = isNativeLittleEndian();
  }

  /**
   * @param buffer
   * @param byteOffset
   * @param byteLength
   * @return a {@link DataView} instance
   */
  public static DataView create(ArrayBuffer buffer, int byteOffset,
      int byteLength) {
    return new DataViewNativeEmul(buffer, byteOffset, byteLength);
  }

  /**
   * @return true if this platform's typed arrays are little-endian
   */
  private static native boolean isNativeLittleEndian() /*-{
    var i8 = new Int8Array(4);
    i8[0] = 1;
    i8[1] = 2;
    i8[2] = 3;
    i8[3] = 4;
    var i32 = new Int32Array(i8.buffer);
    // TODO(jat): do we need to check for other endianness beside BE/LE?
    return i32[0] == 0x04030201;
  }-*/;

  protected final ArrayBuffer buffer;
  protected final int bufferByteOffset;
  protected final int byteLength;
  
  /**
   * A temporary buffer used for reversing bytes.
   */
  protected final Uint8ArrayNative tempBuffer;

  /**
   * A view of the underlying buffer as bytes.
   */
  protected final Uint8ArrayNative uint8Array;

  protected DataViewNativeEmul(ArrayBuffer buffer, int byteOffset, int byteLength) {
    this.buffer = buffer;
    this.bufferByteOffset = byteOffset;
    this.byteLength = byteLength;
    tempBuffer = Uint8ArrayNative.create(ArrayBufferNative.create(8), 0, 8);
    uint8Array = Uint8ArrayNative.create(buffer, byteOffset, byteLength);
  }

  @Override
  public ArrayBuffer buffer() {
    return buffer;
  }

  @Override
  public int byteLength() {
    return byteLength;
  }

  @Override
  public int byteOffset() {
    return bufferByteOffset;
  }

  @Override
  public float getFloat32(int byteOffset) {
    return getFloat32(byteOffset, false);
  }

  @Override
  public float getFloat32(int byteOffset, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int len = Float32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(uint8Array, ofs, len, tempBuffer, 0);
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    return Float32ArrayNative.create(buf, ofs, 1).get(0);
  }

  @Override
  public double getFloat64(int byteOffset) {
    return getFloat64(byteOffset, false);
  }

  @Override
  public double getFloat64(int byteOffset, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int len = Float64Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(uint8Array, ofs, len, tempBuffer, 0);
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    return Float64ArrayNative.create(buf, ofs, 1).get(0);
  }

  @Override
  public short getInt16(int byteOffset) {
    return getInt16(byteOffset, false);
  }

  @Override
  public short getInt16(int byteOffset, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int len = Int16Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(uint8Array, ofs, len, tempBuffer, 0);
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    return Int16ArrayNative.create(buf, ofs, 1).get(0);
  }

  @Override
  public int getInt32(int byteOffset) {
    return getInt32(byteOffset, false);
  }

  @Override
  public int getInt32(int byteOffset, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int len = Int32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(uint8Array, ofs, len, tempBuffer, 0);
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    return Int32ArrayNative.create(buf, ofs, 1).get(0);
  }

  @Override
  public byte getInt8(int byteOffset) {
    return Int8ArrayNative.create(buffer, byteOffset, 1).get(0);
  }

  @Override
  public int getUint16(int byteOffset) {
    return getUint16(byteOffset, false);
  }

  @Override
  public int getUint16(int byteOffset, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int len = Uint16Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(uint8Array, ofs, len, tempBuffer, 0);
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    return Uint16ArrayNative.create(buf, ofs, 1).get(0);
  }

  @Override
  public long getUint32(int byteOffset) {
    return getUint32(byteOffset, false);
  }

  @Override
  public long getUint32(int byteOffset, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int len = Uint32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(uint8Array, ofs, len, tempBuffer, 0);
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    return Uint32ArrayNative.create(buf, ofs, 1).get(0);
  }

  @Override
  public double getUint32AsDouble(int byteOffset) {
    return getUint32(byteOffset, false);
  }

  @Override
  public double getUint32AsDouble(int byteOffset, boolean littleEndian) {
    return getUint32(byteOffset, littleEndian);
  }

  @Override
  public short getUint8(int byteOffset) {
    return Uint8ArrayNative.create(buffer, byteOffset, 1).get(0);
  }

  @Override
  public void setFloat32(int byteOffset, float value) {
    setFloat32(byteOffset, value, false);
  }

  @Override
  public void setFloat32(int byteOffset, float value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Float32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Float32ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setFloat64(int byteOffset, double value) {
    setFloat64(byteOffset, value, false);
  }

  @Override
  public void setFloat64(int byteOffset, double value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Float64Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Float64ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setInt16(int byteOffset, int value) {
    setInt16(byteOffset, value, false);
  }

  @Override
  public void setInt16(int byteOffset, int value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Int16Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Int16ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setInt32(int byteOffset, int value) {
    setInt32(byteOffset, value, false);
  }

  @Override
  public void setInt32(int byteOffset, int value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Int32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Int32ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setInt8(int byteOffset, int value) {
    Int8ArrayNative.create(buffer, byteOffset, 1).set(0, value);
  }

  @Override
  public void setUint16(int byteOffset, int value) {
    setUint16(byteOffset, value, false);
  }

  @Override
  public void setUint16(int byteOffset, int value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Uint16Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Uint16ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setUint32(int byteOffset, long value) {
    setUint32(byteOffset, value, false);
  }

  @Override
  public void setUint32(int byteOffset, long value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Uint32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Uint32ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setUint32FromDouble(int byteOffset, double value) {
    setUint32FromDouble(byteOffset, value, false);
  }

  @Override
  public void setUint32FromDouble(int byteOffset, double value, boolean littleEndian) {
    ArrayBuffer buf = buffer;
    int ofs = bufferByteOffset + byteOffset;
    int finalOfs = ofs;
    int len = Uint32Array.BYTES_PER_ELEMENT;
    if (littleEndian != nativeLittleEndian) {
      buf = tempBuffer.buffer();
      ofs = 0;
    }
    Uint32ArrayNative.create(buf, ofs, 1).set(0, value);
    if (littleEndian != nativeLittleEndian) {
      reverseBytes(tempBuffer, 0, len, uint8Array, finalOfs);
    }
  }

  @Override
  public void setUint8(int byteOffset, int value) {
    Uint8ArrayNative.create(buffer, byteOffset, 1).set(0, value);
  }

  /**
   * Copy bytes from the underlying buffer to a temporary buffer, reversing them
   * in the process.
   * 
   * @param src
   * @param srcOfs offset into {@code buffer}
   * @param len number of bytes to copy
   * @param dest
   * @param destOfs
   */
  protected final void reverseBytes(Uint8ArrayNative src, int srcOfs, int len,
      Uint8ArrayNative dest, int destOfs) {
    for (int i = 0; i < len; ++i) {
      dest.set(i + destOfs, src.get(srcOfs + len - 1 - i));
    }
  }
}
