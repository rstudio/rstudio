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

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Float64Array;
import com.google.gwt.typedarrays.shared.Int16Array;
import com.google.gwt.typedarrays.shared.Int32Array;
import com.google.gwt.typedarrays.shared.Int8Array;
import com.google.gwt.typedarrays.shared.Uint16Array;
import com.google.gwt.typedarrays.shared.Uint32Array;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;

/**
 * JS-specific utility methods, for use in client-side code that has the values in
 * JS objects already.
 */
public class JsUtils {

  /**
   * Creates an {@link ArrayBuffer} from a string, with bytes encoded as
   * individual characters (which means in UTF8-encoded strings, byte
   * values {@code 0x80-0xFF} take two bytes. 
   * 
   * @param str
   * @return an {@link ArrayBuffer} containing the bytes decoded from the
   *     string
   */
  public static native ArrayBuffer arrayBufferFromString(String str) /*-{
    // TODO(jat): more efficient way to do this?
    var len = str.length;
    var buf = new Uint8Array(len);
    for (var i = 0; i < len; ++i) {
       buf[i] = str.charCodeAt(i);
    }
    return buf.buffer;
  }-*/;

  public static native Float32ArrayNative createFloat32Array(JsArrayNumber array) /*-{
    return new Float32Array(array);
  }-*/;

  public static native Float64ArrayNative createFloat64Array(JsArrayNumber array) /*-{
    return new Float64Array(array);
  }-*/;

  public static native Int16ArrayNative createInt16Array(JsArrayInteger array) /*-{
    return new Int16Array(array);
  }-*/;

  public static native Int32ArrayNative createInt32Array(JsArrayInteger array) /*-{
    return new Int32Array(array);
  }-*/;

  public static native Int8ArrayNative createInt8Array(JsArrayInteger array) /*-{
    return new Int8Array(array);
  }-*/;

  public static native Uint16ArrayNative createUint16Array(JsArrayInteger array) /*-{
    return new Uint16Array(array);
  }-*/;

  public static native Uint32ArrayNative createUint32Array(JsArrayNumber array) /*-{
    return new Uint32Array(array);
  }-*/;

  public static native Uint8ArrayNative createUint8Array(JsArrayInteger array) /*-{
    return new Uint8Array(array);
  }-*/;

  public static Uint8ClampedArray createUint8ClampedArray(JsArrayInteger array) {
    if (hasClampedArray()) {
      return Uint8ArrayNative.createClamped(array);
    } else {
      return Uint8ClampedArrayNativeEmul.create(array);
    }
  }

  public static native void set(Float32Array dest, JsArrayNumber array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Float32Array dest, JsArrayNumber array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Float64Array dest, JsArrayNumber array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Float64Array dest, JsArrayNumber array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Int16Array dest, JsArrayInteger array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Int16Array dest, JsArrayInteger array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Int32Array dest, JsArrayInteger array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Int32Array dest, JsArrayInteger array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Int8Array dest, JsArrayInteger array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Int8Array dest, JsArrayInteger array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Uint16Array dest, JsArrayInteger array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Uint16Array dest, JsArrayInteger array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Uint32Array dest, JsArrayNumber array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Uint32Array dest, JsArrayNumber array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static native void set(Uint8Array dest, JsArrayInteger array) /*-{
    dest.set(array);
  }-*/;

  public static native void set(Uint8Array dest, JsArrayInteger array, int offset) /*-{
    dest.set(array, offset);
  }-*/;

  public static void set(Uint8ClampedArray dest, JsArrayInteger array) {
    if (hasClampedArray()) {
      set((Uint8Array) dest, array, 0);
    } else {
      int len = array.length();
      for (int i = 0; i < len; ++i) {
        dest.set(i, array.get(i));
      }
    }
  }

  public static void set(Uint8ClampedArray dest, JsArrayInteger array, int offset) {
    if (hasClampedArray()) {
      set((Uint8Array) dest, array, offset);
    } else {
      int len = array.length();
      for (int i = 0; i < len; ++i) {
        dest.set(i + offset, array.get(i));
      }
    }
  }

  /**
   * Creates a string from an {@link ArrayBuffer}, with bytes encoded as
   * individual characters (which means in UTF8-encoded strings, byte
   * values {@code 0x80-0xFF} take two bytes. 
   * 
   * @param buf
   * @return a string encoding the bytes in the {@link ArrayBuffer}
   */
  public static native String stringFromArrayBuffer(ArrayBuffer buf) /*-{
    // TODO(jat): more efficient way to do this?
    var cc = [];
    var i8 = new Uint8Array(buf);
    for (var i = 0; i < buf.byteLength; ++i) {
      cc.push(i8[i]);
    }
    return String.fromCharCode.apply(null, cc);
  }-*/;

  private static native boolean hasClampedArray() /*-{
    // TODO(jat): this is awkward - should this be deferred bound?
    return !!(window.Uint8ClampedArray);
  }-*/;

  private JsUtils() {
  }
}
