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
 * A view representing an {@link ArrayBuffer} as heterogeneous values.
 * 
 * {@link "http://www.khronos.org/registry/typedarray/specs/latest/#8"}
 */
public interface DataView extends ArrayBufferView {

  float getFloat32(int byteOffset);

  float getFloat32(int byteOffset, boolean littleEndian);

  double getFloat64(int byteOffset);

  double getFloat64(int byteOffset, boolean littleEndian);

  short getInt16(int byteOffset);

  short getInt16(int byteOffset, boolean littleEndian);

  int getInt32(int byteOffset);

  int getInt32(int byteOffset, boolean littleEndian);

  byte getInt8(int byteOffset);

  int getUint16(int byteOffset);

  int getUint16(int byteOffset, boolean littleEndian);

  long getUint32(int byteOffset);

  long getUint32(int byteOffset, boolean littleEndian);

  /**
   * @param byteOffset
   * @return unsigned 32-bit int as a double
   */
  double getUint32AsDouble(int byteOffset);

  /**
   * @param byteOffset
   * @param littleEndian
   * @return unsigned 32-bit int as a double
   */
  double getUint32AsDouble(int byteOffset, boolean littleEndian);

  short getUint8(int byteOffset);

  void setFloat32(int byteOffset, float value);

  void setFloat32(int byteOffset, float value, boolean littleEndian);

  void setFloat64(int byteOffset, double value);

  void setFloat64(int byteOffset, double value, boolean littleEndian);

  void setInt16(int byteOffset, int value);

  void setInt16(int byteOffset, int value, boolean littleEndian);

  void setInt32(int byteOffset, int value);

  void setInt32(int byteOffset, int value, boolean littleEndian);

  void setInt8(int byteOffset, int value);

  void setUint16(int byteOffset, int value);

  void setUint16(int byteOffset, int value, boolean littleEndian);

  void setUint32(int byteOffset, long value);

  void setUint32(int byteOffset, long value, boolean littleEndian);

  /**
   * @param byteOffset
   * @param value
   */
  void setUint32FromDouble(int byteOffset, double value);

  /**
   * @param byteOffset
   * @param value
   * @param littleEndian
   */
  void setUint32FromDouble(int byteOffset, double value, boolean littleEndian);

  void setUint8(int byteOffset, int i);
}
