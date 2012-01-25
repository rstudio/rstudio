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
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;

/**
 * JS native implementation of {@link Uint8Array} and, where natively supported,
 * {@link Uint8ClampedArray}.
 * <p>
 * This should generally not be referenced directly -- see
 * {@link com.google.gwt.typedarrays.shared.TypedArrays} and
 * {@link com.google.gwt.typedarrays.client.JsUtils}.
 */
public final class Uint8ArrayNative extends ArrayBufferViewNative implements Uint8ClampedArray {

  /**
   * Create a {@link Uint8Array} instance.
   * 
   * @param buffer
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative create(ArrayBuffer buffer) /*-{
    return new Uint8Array(buffer);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance.
   * 
   * @param buffer
   * @param byteOffset
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative create(ArrayBuffer buffer, int byteOffset) /*-{
    return new Uint8Array(buffer, byteOffset);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance.
   * 
   * @param buffer
   * @param byteOffset
   * @param length
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative create(ArrayBuffer buffer, int byteOffset,
      int length) /*-{
    return new Uint8Array(buffer, byteOffset, length);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance.
   * 
   * @param length
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative create(int length) /*-{
    return new Uint8Array(length);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance from an array.
   * 
   * @param array an array of initial values
   * @return a {@link Uint8Array} instance
   */
  public static Uint8ArrayNative create(int[] array) {
    return create(JsArrayUtils.readOnlyJsArray(array));
  }

  /**
   * Create a {@link Uint8Array} instance from a JavaScript array-like object.
   * 
   * @param array a JS array or array-like object
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative create(JavaScriptObject array) /*-{
    return new Uint8Array(array);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance from an array.
   * 
   * @param array an array of initial values
   * @return a {@link Uint8Array} instance
   */
  public static Uint8ArrayNative create(short[] array) {
    return create(JsArrayUtils.readOnlyJsArray(array));
  }

  /**
   * Create a {@link Uint8ClampedArray} instance. Must only be called if the
   * environment natively supports clamped arrays -- otherwise
   * {@link Uint8ClampedArrayNativeEmul} should be used instead.
   * 
   * @param buffer
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative createClamped(ArrayBuffer buffer) /*-{
    return new Uint8ClampedArray(buffer);
  }-*/;

  /**
   * Create a {@link Uint8ClampedArray} instance. Must only be called if the
   * environment natively supports clamped arrays -- otherwise
   * {@link Uint8ClampedArrayNativeEmul} should be used instead.
   * 
   * @param buffer
   * @param byteOffset
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative createClamped(ArrayBuffer buffer,
      int byteOffset) /*-{
    return new Uint8ClampedArray(buffer, byteOffset);
  }-*/;

  /**
   * Create a {@link Uint8ClampedArray} instance. Must only be called if the
   * environment natively supports clamped arrays -- otherwise
   * {@link Uint8ClampedArrayNativeEmul} should be used instead.
   * 
   * @param buffer
   * @param byteOffset
   * @param length
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative createClamped(ArrayBuffer buffer,
      int byteOffset, int length) /*-{
    return new Uint8ClampedArray(buffer, byteOffset, length);
  }-*/;

  /**
   * Create a {@link Uint8ClampedArray} instance. Must only be called if the
   * environment natively supports clamped arrays -- otherwise
   * {@link Uint8ClampedArrayNativeEmul} should be used instead.
   * 
   * @param length
   * @return a {@link Uint8Array} instance
   */
  public static native Uint8ArrayNative createClamped(int length) /*-{
    return new Uint8ClampedArray(length);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance from an array. Must only be called if
   * the environment natively supports clamped arrays -- otherwise
   * {@link Uint8ClampedArrayNativeEmul} should be used instead.
   * 
   * @param array an array of initial values
   * @return a {@link Uint8Array} instance
   */
  public static Uint8ArrayNative createClamped(int[] array) {
    return createClamped(JsArrayUtils.readOnlyJsArray(array));
  }

  /**
   * Create a {@link Uint8ClampedArray} instance from a JavaScript array-like
   * object. Must only be called if the environment natively supports clamped
   * arrays -- otherwise {@link Uint8ClampedArrayNativeEmul} should be used
   * instead.
   * 
   * @param array a JS array or array-like object
   * @return a {@link Uint8ClampedArray} instance
   */
  public static native Uint8ArrayNative createClamped(JavaScriptObject array) /*-{
    return new Uint8ClampedArray(array);
  }-*/;

  /**
   * Create a {@link Uint8Array} instance from an array. Must only be called if
   * the environment natively supports clamped arrays -- otherwise
   * {@link Uint8ClampedArrayNativeEmul} should be used instead.
   * 
   * @param array an array of initial values
   * @return a {@link Uint8Array} instance
   */
  public static Uint8ArrayNative createClamped(short[] array) {
    return createClamped(JsArrayUtils.readOnlyJsArray(array));
  }

  protected Uint8ArrayNative() {
  }

  @Override
  public native short get(int index) /*-{
    return this[index];
  }-*/;

  @Override
  public native int length() /*-{
    return this.length;
  }-*/;

  @Override
  public native void set(int index, int value) /*-{
    this[index] = value;
  }-*/;

  @Override
  public void set(int[] array) {
    set(array, 0);
  }

  @Override
  public void set(int[] array, int offset) {
    set(JsArrayUtils.readOnlyJsArray(array), offset);
  }

  @Override
  public void set(short[] array) {
    set(array, 0);
  }

  @Override
  public void set(short[] array, int offset) {
    set(JsArrayUtils.readOnlyJsArray(array), offset);
  }

  @Override
  public void set(Uint8Array array) {
    set(array, 0);
  }

  @Override
  public void set(Uint8Array array, int offset) {
    if (array instanceof Uint8ArrayNative) {
      // Note that any JSO would pass this check, but since only one JSO can
      // implement a given interface it has to be this one, so any other
      // implementations must be Java emulations.
      setNative(array, offset);
      return;
    }
    int len = array.length();
    for (int i = 0; i < len; ++i) {
      set(offset++, array.get(i));
    }
  }

  @Override
  public native Uint8ArrayNative subarray(int begin) /*-{
    return this.subarray(begin);
  }-*/;

  @Override
  public native Uint8ArrayNative subarray(int begin, int end) /*-{
    return this.subarray(begin, end);
  }-*/;

  private native void set(JavaScriptObject array, int offset) /*-{
    this.set(array, offset);
  }-*/;

  /**
   * Called when {@code array} is known to be a native {@link Uint8Array}
   * implementation.
   * 
   * @param array
   * @param offset
   */
  private native void setNative(Uint8Array array, int offset) /*-{
    this.set(array, offset);
  }-*/;
}
