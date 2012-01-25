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

/**
 * JS native implementation of {@link DataView}.
 */
public final class DataViewNative extends ArrayBufferViewNative implements DataView {

  /**
   * @param buffer
   * @return a {@link DataView} instance
   */
  public static native DataView create(ArrayBuffer buffer) /*-{
    return new DataView(buffer);
  }-*/;

  /**
   * @param buffer
   * @param byteOffset
   * @return a {@link DataView} instance
   */
  public static native DataView create(ArrayBuffer buffer, int byteOffset) /*-{
    return new DataView(buffer, byteOffset);
  }-*/;

  /**
   * @param buffer
   * @param byteOffset
   * @param byteLength
   * @return a {@link DataView} instance
   */
  public static native DataView create(ArrayBuffer buffer, int byteOffset,
      int byteLength) /*-{
    return new DataView(buffer, byteOffset, byteLength);
  }-*/;

  protected DataViewNative() {
  }

  @Override
  public native float getFloat32(int byteOffset) /*-{
    return this.getFloat32(byteOffset);
  }-*/;

  @Override
  public native float getFloat32(int byteOffset, boolean littleEndian) /*-{
    return this.getFloat32(byteOffset, littleEndian);
  }-*/;

  @Override
  public native double getFloat64(int byteOffset) /*-{
    return this.getFloat64(byteOffset);
  }-*/;

  @Override
  public native double getFloat64(int byteOffset, boolean littleEndian) /*-{
    return this.getFloat64(byteOffset, littleEndian);
  }-*/;

  @Override
  public native short getInt16(int byteOffset) /*-{
    return this.getInt16(byteOffset);
  }-*/;

  @Override
  public native short getInt16(int byteOffset, boolean littleEndian) /*-{
    return this.getInt16(byteOffset, littleEndian);
  }-*/;

  @Override
  public native int getInt32(int byteOffset) /*-{
    return this.getInt32(byteOffset);
  }-*/;

  @Override
  public native int getInt32(int byteOffset, boolean littleEndian) /*-{
    return this.getInt32(byteOffset, littleEndian);
  }-*/;

  @Override
  public native byte getInt8(int byteOffset) /*-{
    return this.getInt8(byteOffset);
  }-*/;

  @Override
  public native int getUint16(int byteOffset) /*-{
    return this.getUint16(byteOffset);
  }-*/;

  @Override
  public native int getUint16(int byteOffset, boolean littleEndian) /*-{
    return this.getUint16(byteOffset, littleEndian);
  }-*/;

  @Override
  public long getUint32(int byteOffset) {
    return (long) getUint32AsDouble(byteOffset);
  }

  @Override
  public long getUint32(int byteOffset, boolean littleEndian) {
    return (long) getUint32AsDouble(byteOffset, littleEndian);
  }

  @Override
  public native double getUint32AsDouble(int byteOffset) /*-{
    return this.getUint32(byteOffset);
  }-*/;

  @Override
  public native double getUint32AsDouble(int byteOffset, boolean littleEndian) /*-{
    return this.getUint32(byteOffset, littleEndian);
  }-*/;

  @Override
  public native short getUint8(int byteOffset) /*-{
    return this.getUint8(byteOffset);
  }-*/;

  @Override
  public native void setFloat32(int byteOffset, float value) /*-{
    this.setFloat32(byteOffset, value);
  }-*/;

  @Override
  public native void setFloat32(int byteOffset, float value, boolean littleEndian) /*-{
    this.setFloat32(byteOffset, value, littleEndian);
  }-*/;

  @Override
  public native void setFloat64(int byteOffset, double value) /*-{
    this.setFloat64(byteOffset, value);
  }-*/;

  @Override
  public native void setFloat64(int byteOffset, double value, boolean littleEndian) /*-{
    this.setFloat64(byteOffset, value, littleEndian);
  }-*/;

  @Override
  public native void setInt16(int byteOffset, int value) /*-{
    this.setInt16(byteOffset, value);
  }-*/;

  @Override
  public native void setInt16(int byteOffset, int value, boolean littleEndian) /*-{
    this.setInt16(byteOffset, value, littleEndian);
  }-*/;

  @Override
  public native void setInt32(int byteOffset, int value) /*-{
    this.setInt32(byteOffset, value);
  }-*/;

  @Override
  public native void setInt32(int byteOffset, int value, boolean littleEndian) /*-{
    this.setInt32(byteOffset, value, littleEndian);
  }-*/;

  @Override
  public native void setInt8(int byteOffset, int value) /*-{
    this.setInt8(byteOffset, value);
  }-*/;

  @Override
  public native void setUint16(int byteOffset, int value) /*-{
    this.setUint16(byteOffset, value);
  }-*/;

  @Override
  public native void setUint16(int byteOffset, int value, boolean littleEndian) /*-{
    this.setUint16(byteOffset, value, littleEndian);
  }-*/;

  @Override
  public void setUint32(int byteOffset, long value) {
    setUint32FromDouble(byteOffset, value);
  }

  @Override
  public void setUint32(int byteOffset, long value, boolean littleEndian) {
    setUint32FromDouble(byteOffset, value, littleEndian);
  }

  @Override
  public native void setUint32FromDouble(int byteOffset, double value) /*-{
    this.setUint32(byteOffset, value);
  }-*/;

  @Override
  public native void setUint32FromDouble(int byteOffset, double value, boolean littleEndian) /*-{
    this.setUint32(byteOffset, value, littleEndian);
  }-*/;

  @Override
  public native void setUint8(int byteOffset, int value) /*-{
    this.setUint8(byteOffset, value);
  }-*/;
}
