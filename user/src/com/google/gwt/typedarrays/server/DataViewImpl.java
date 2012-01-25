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
package com.google.gwt.typedarrays.server;

import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.DataView;

/**
 * Pure Java implementation of {@link DataView}.
 */
public class DataViewImpl extends ArrayBufferViewImpl implements DataView {

  /**
   * @param buffer
   * @param byteOffset
   * @param byteLength
   */
  public DataViewImpl(ArrayBuffer buffer, int byteOffset, int byteLength) {
    super(buffer, byteOffset, byteLength);
  }

  @Override
  public float getFloat32(int byteOffset) {
    return getFloat32(byteOffset, false);
  }

  @Override
  public float getFloat32(int byteOffset, boolean littleEndian) {
    return arrayBuf.getFloat32(byteOffset, littleEndian);
  }

  @Override
  public double getFloat64(int byteOffset) {
    return getFloat64(byteOffset, false);
  }

  @Override
  public double getFloat64(int byteOffset, boolean littleEndian) {
    return arrayBuf.getFloat64(byteOffset, littleEndian);
  }

  @Override
  public short getInt16(int byteOffset) {
    return getInt16(byteOffset, false);
  }

  @Override
  public short getInt16(int byteOffset, boolean littleEndian) {
    return arrayBuf.getInt16(byteOffset, littleEndian);
  }

  @Override
  public int getInt32(int byteOffset) {
    return getInt32(byteOffset, false);
  }

  @Override
  public int getInt32(int byteOffset, boolean littleEndian) {
    return arrayBuf.getInt32(byteOffset, littleEndian);
  }

  @Override
  public byte getInt8(int byteOffset) {
    return arrayBuf.getInt8(byteOffset);
  }

  @Override
  public int getUint16(int byteOffset) {
    return getUint16(byteOffset, false);
  }

  @Override
  public int getUint16(int byteOffset, boolean littleEndian) {
    int val = arrayBuf.getInt16(byteOffset, littleEndian);
    if (val < 0) {
      val += 0x10000;
    }
    return val;
  }

  @Override
  public long getUint32(int byteOffset) {
    return getUint32(byteOffset, false);
  }

  @Override
  public long getUint32(int byteOffset, boolean littleEndian) {
    long val = arrayBuf.getInt32(byteOffset, littleEndian);
    if (val < 0) {
      val += 0x100000000L;
    }
    return val;
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
    short val = getInt8(byteOffset);
    if (val < 0) {
      val += 256;
    }
    return val;
  }

  @Override
  public void setFloat32(int byteOffset, float value) {
    setFloat32(byteOffset, value, false);
  }

  @Override
  public void setFloat32(int byteOffset, float value, boolean littleEndian) {
    arrayBuf.setFloat32(byteOffset, value, littleEndian);
  }

  @Override
  public void setFloat64(int byteOffset, double value) {
    setFloat64(byteOffset, value, false);
  }

  @Override
  public void setFloat64(int byteOffset, double value, boolean littleEndian) {
    arrayBuf.setFloat64(byteOffset, value, littleEndian);
  }

  @Override
  public void setInt16(int byteOffset, int value) {
    setInt16(byteOffset, value, false);
  }

  @Override
  public void setInt16(int byteOffset, int value, boolean littleEndian) {
    arrayBuf.setInt16(byteOffset, (short) (value & 0xFFFF), littleEndian);
  }

  @Override
  public void setInt32(int byteOffset, int value) {
    setInt32(byteOffset, value, false);
  }

  @Override
  public void setInt32(int byteOffset, int value, boolean littleEndian) {
    arrayBuf.setInt32(byteOffset, value, littleEndian);
  }

  @Override
  public void setInt8(int byteOffset, int value) {
    arrayBuf.setInt8(byteOffset, (byte) (value & 255));
  }

  @Override
  public void setUint16(int byteOffset, int value) {
    setUint16(byteOffset, value, false);
  }

  @Override
  public void setUint16(int byteOffset, int value, boolean littleEndian) {
    arrayBuf.setInt16(byteOffset, (short) (value & 0xFFFF), littleEndian);
  }

  @Override
  public void setUint32(int byteOffset, long value) {
    setUint32(byteOffset, value, false);
  }

  @Override
  public void setUint32(int byteOffset, long value, boolean littleEndian) {
    arrayBuf.setInt32(byteOffset, (int) (value & 0xFFFFFFFF), littleEndian);
  }

  @Override
  public void setUint32FromDouble(int byteOffset, double value) {
    setUint32(byteOffset, (long) value);
  }

  @Override
  public void setUint32FromDouble(int byteOffset, double value, boolean littleEndian) {
    setUint32(byteOffset, (long) value, littleEndian);
  }

  @Override
  public void setUint8(int byteOffset, int value) {
    arrayBuf.setInt8(byteOffset, (byte) (value & 255));
  }
}
