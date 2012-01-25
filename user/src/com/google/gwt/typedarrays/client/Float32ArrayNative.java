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
import com.google.gwt.typedarrays.shared.Float32Array;

/**
 * JS native implementation of {@link Float32Array}.
 */
public final class Float32ArrayNative extends ArrayBufferViewNative implements Float32Array {

  /**
   * @param buffer
   * @return a {@link Float32Array} instance
   */
  public static native Float32ArrayNative create(ArrayBuffer buffer) /*-{
    return new Float32Array(buffer);
  }-*/;

  /**
   * @param buffer
   * @param byteOffset
   * @return a {@link Float32Array} instance
   */
  public static native Float32ArrayNative create(ArrayBuffer buffer, int byteOffset) /*-{
    return new Float32Array(buffer, byteOffset);
  }-*/;

  /**
   * @param buffer
   * @param byteOffset
   * @param length
   * @return a {@link Float32Array} instance
   */
  public static native Float32ArrayNative create(ArrayBuffer buffer, int byteOffset,
      int length) /*-{
    return new Float32Array(buffer, byteOffset, length);
  }-*/;

  /**
   * @param length
   * @return a {@link Float32Array} instance
   */
  public static native Float32ArrayNative create(int length) /*-{
    return new Float32Array(length);
  }-*/;

  protected Float32ArrayNative() {
  }

  @Override
  public native float get(int index) /*-{
    return this[index];
  }-*/;

  @Override
  public native int length() /*-{
    return this.length;
  }-*/;

  @Override
  public void set(float[] array) {
    set(array, 0);
  }

  @Override
  public void set(float[] array, int offset) {
    set(JsArrayUtils.readOnlyJsArray(array), offset);
  }

  @Override
  public native void set(Float32Array array) /*-{
    this.set(array);
  }-*/;

  @Override
  public native void set(Float32Array array, int offset) /*-{
    this.set(array, offset);
  }-*/;

  @Override
  public native void set(int index, float value) /*-{
    this[index] = value;
  }-*/;

  @Override
  public native Float32Array subarray(int begin) /*-{
    return this.subarray(begin);
  }-*/;

  @Override
  public native Float32Array subarray(int begin, int end) /*-{
    return this.subarray(begin, end);
  }-*/;

  private native void set(JavaScriptObject array, int offset) /*-{
    this.set(array, offset);
  }-*/;
}
