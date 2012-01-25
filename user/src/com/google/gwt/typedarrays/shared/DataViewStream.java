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
package com.google.gwt.typedarrays.shared;

/**
 * A stream of heterogeneous values on top of a {@link DataViewStream}.
 */
public class DataViewStream {
  // TODO: add methods to pack/unpack strings in various formats

  private final DataView dataView;
  private int offset;

  public DataViewStream(DataView dataView) {
    this.dataView = dataView;
    offset = 0;
  }

  public float getFloat32() {
    return getFloat32(false);
  }

  public float getFloat32(boolean littleEndian) {
    float val = dataView.getFloat32(offset, littleEndian);
    offset += 4;
    return val;
  }

  public double getFloat64() {
    return getFloat64(false);
  }

  public double getFloat64(boolean littleEndian) {
    double val = dataView.getFloat64(offset, littleEndian);
    offset += 8;
    return val;
  }

  public short getInt16() {
    return getInt16(false);
  }

  public short getInt16(boolean littleEndian) {
    short val = dataView.getInt16(offset, littleEndian);
    offset += 2;
    return val;
  }

  public int getInt32() {
    return getInt32(false);
  }

  public int getInt32(boolean littleEndian) {
    int val = dataView.getInt32(offset, littleEndian);
    offset += 4;
    return val;
  }

  public byte getInt8() {
    return dataView.getInt8(offset++);
  }

  public int getUint16() {
    return getUint16(false);
  }

  public int getUint16(boolean littleEndian) {
    int val = dataView.getUint16(offset, littleEndian);
    offset += 2;
    return val;
  }

  public long getUint32() {
    return getUint32(false);
  }

  public long getUint32(boolean littleEndian) {
    long val = dataView.getUint32(offset, littleEndian);
    offset += 4;
    return val;
  }

  /**
   * @return unsigned 32-bit int as a double
   */
  public double getUint32AsDouble() {
    return getUint32AsDouble(false);
  }

  /**
   * @param littleEndian
   * @return unsigned 32-bit int as a double
   */
  public double getUint32AsDouble(boolean littleEndian) {
    double val = dataView.getUint32AsDouble(offset, littleEndian);
    offset += 4;
    return val;
  }

  public short getUint8() {
    return dataView.getUint8(offset++);
  }

  public int position() {
    return offset;
  }

  public void putFloat32(float value) {
    putFloat32(value, false);
  }

  public void putFloat32(float value, boolean littleEndian) {
    dataView.setFloat32(offset, value, littleEndian);
    offset += 4;
  }

  public void putFloat64(double value) {
    putFloat64(value, false);
  }

  public void putFloat64(double value, boolean littleEndian) {
    dataView.setFloat64(offset, value, littleEndian);
    offset += 8;
  }

  public void putInt16(int value) {
    putInt16(value, false);
  }

  public void putInt16(int value, boolean littleEndian) {
    dataView.setInt16(offset, value, littleEndian);
    offset += 2;
  }

  public void putInt32(int value) {
    putInt32(value, false);
  }

  public void putInt32(int value, boolean littleEndian) {
    dataView.setInt32(offset, value, littleEndian);
    offset += 4;
  }

  public void putInt8(int value) {
    dataView.setInt8(offset++, value);
  }

  public void putUint16(int value) {
    putUint16(value, false);
  }

  public void putUint16(int value, boolean littleEndian) {
    dataView.setUint16(offset, value, littleEndian);
    offset += 2;
  }

  public void putUint32(long value) {
    putUint32(value, false);
  }

  public void putUint32(long value, boolean littleEndian) {
    dataView.setUint32(offset, value, littleEndian);
    offset += 4;
  }

  /**
   * @param value
   */
  public void putUint32FromDouble(double value) {
    putUint32FromDouble(value, false);
  }

  /**
   * @param value
   * @param littleEndian
   */
  public void putUint32FromDouble(double value, boolean littleEndian) {
    dataView.setUint32FromDouble(offset, value, littleEndian);
    offset += 4;
  }

  public void putUint8(int value) {
    dataView.setUint8(offset++, value);
  }

  public void rewind() {
    offset = 0;
  }

  public void setPosition(int position) {
    if (position < 0 || position >= dataView.byteLength()) {
      throw new IndexOutOfBoundsException();
    }
    offset = position;
  }
}
