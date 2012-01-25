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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayUtils;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.Uint32Array;

/**
 * JS native implementation of {@link Uint32Array}.
 */
public final class Uint32ArrayNative extends ArrayBufferViewNative implements Uint32Array {

  /**
   * @param buffer
   * @return a {@link Uint32Array} instance
   */
  public static native Uint32ArrayNative create(ArrayBuffer buffer) /*-{
    return new Uint32Array(buffer);
  }-*/;

  /**
   * @param buffer
   * @param byteOffset
   * @return a {@link Uint32Array} instance
   */
  public static native Uint32ArrayNative create(ArrayBuffer buffer, int byteOffset) /*-{
    return new Uint32Array(buffer, byteOffset);
  }-*/;

  /**
   * @param buffer
   * @param byteOffset
   * @param length
   * @return a {@link Uint32Array} instance
   */
  public static native Uint32ArrayNative create(ArrayBuffer buffer, int byteOffset,
      int length) /*-{
    return new Uint32Array(buffer, byteOffset, length);
  }-*/;

  /**
   * @param length
   * @return a {@link Uint32Array} instance
   */
  public static native Uint32ArrayNative create(int length) /*-{
    return new Uint32Array(length);
  }-*/;

  protected Uint32ArrayNative() {
  }

  @Override
  public long get(int index) {
    return (long) getAsDouble(index);
  }

  @Override
  public native double getAsDouble(int index) /*-{
    return this[index];
  }-*/;

  @Override
  public native int length() /*-{
    return this.length;
  }-*/;

  @Override
  public void set(double[] array) {
    set(array, 0);
  }

  @Override
  public void set(double[] array, int offset) {
    set(JsArrayUtils.readOnlyJsArray(array), offset);
  }

  @Override
  public native void set(int index, double value) /*-{
    this[index] = value;
  }-*/;

  @Override
  public void set(int index, long value) {
    set(index, (double) value);
  }

  @Override
  public void set(long[] array) {
    set(array, 0);
  }

  @Override
  public void set(long[] array, int offset) {
    int len = array.length;
    double[] temp = new double[len];
    for (int i = 0; i < len; ++i) {
      temp[i] = array[i];
    }
    set(temp, offset);
  }

  @Override
  public native void set(Uint32Array array) /*-{
    this.set(array);
  }-*/;

  @Override
  public native void set(Uint32Array array, int offset) /*-{
    this.set(array, offset);
  }-*/;

  @Override
  public native Uint32Array subarray(int begin) /*-{
    return this.subarray(begin);
  }-*/;

  @Override
  public native Uint32Array subarray(int begin, int end) /*-{
    return this.subarray(begin, end);
  }-*/;

  private native void set(JavaScriptObject array, int offset) /*-{
    this.set(array, offset);
  }-*/;
}
