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

import com.google.gwt.core.client.JsArrayUtils;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.DataView;
import com.google.gwt.typedarrays.shared.TypedArrays.Impl;
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;

/**
 * The default implementation class, which assumes that Typed Arrays might be
 * supported and does runtime checks where necessary, substituting emulated
 * implementations of DataView and Uint8ClampedArray where they are missing.
 * <p>
 * This can be replaced with a version which avoids runtime checks where
 * possible for efficiency.
 */
public class NativeImpl extends Impl {

  @Override
  public ArrayBuffer createArrayBuffer(int length) {
    return ArrayBufferNative.create(length);
  }

  @Override
  public DataView createDataView(ArrayBuffer buffer) {
    if (checkDataViewSupport()) {
      return DataViewNative.create(buffer);
    } else {
      return DataViewNativeEmul.create(buffer, 0, buffer.byteLength());
    }
  }

  @Override
  public DataView createDataView(ArrayBuffer buffer, int byteOffset) {
    if (checkDataViewSupport()) {
      return DataViewNative.create(buffer, byteOffset);
    } else {
      return DataViewNativeEmul.create(buffer, byteOffset, buffer.byteLength() - byteOffset);
    }
  }

  @Override
  public DataView createDataView(ArrayBuffer buffer, int byteOffset,
      int byteLength) {
    if (checkDataViewSupport()) {
      return DataViewNative.create(buffer, byteOffset, byteLength);
    } else {
      return DataViewNativeEmul.create(buffer, byteOffset, byteLength);
    }
  }

  @Override
  public Float32ArrayNative createFloat32Array(ArrayBuffer buffer) {
    return Float32ArrayNative.create(buffer);
  }

  @Override
  public Float32ArrayNative createFloat32Array(ArrayBuffer buffer, int byteOffset) {
    return Float32ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Float32ArrayNative createFloat32Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Float32ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Float32ArrayNative createFloat32Array(float[] array) {
    return JsUtils.createFloat32Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Float32ArrayNative createFloat32Array(int length) {
    return Float32ArrayNative.create(length);
  }

  @Override
  public Float64ArrayNative createFloat64Array(ArrayBuffer buffer) {
    return Float64ArrayNative.create(buffer);
  }

  @Override
  public Float64ArrayNative createFloat64Array(ArrayBuffer buffer, int byteOffset) {
    return Float64ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Float64ArrayNative createFloat64Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Float64ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Float64ArrayNative createFloat64Array(double[] array) {
    return JsUtils.createFloat64Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Float64ArrayNative createFloat64Array(int length) {
    return Float64ArrayNative.create(length);
  }

  @Override
  public Int16ArrayNative createInt16Array(ArrayBuffer buffer) {
    return Int16ArrayNative.create(buffer);
  }

  @Override
  public Int16ArrayNative createInt16Array(ArrayBuffer buffer, int byteOffset) {
    return Int16ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Int16ArrayNative createInt16Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Int16ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Int16ArrayNative createInt16Array(int length) {
    return Int16ArrayNative.create(length);
  }

  @Override
  public Int16ArrayNative createInt16Array(short[] array) {
    return JsUtils.createInt16Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Int32ArrayNative createInt32Array(ArrayBuffer buffer) {
    return Int32ArrayNative.create(buffer);
  }

  @Override
  public Int32ArrayNative createInt32Array(ArrayBuffer buffer, int byteOffset) {
    return Int32ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Int32ArrayNative createInt32Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Int32ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Int32ArrayNative createInt32Array(int length) {
    return Int32ArrayNative.create(length);
  }

  @Override
  public Int32ArrayNative createInt32Array(int[] array) {
    return JsUtils.createInt32Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Int8ArrayNative createInt8Array(ArrayBuffer buffer) {
    return Int8ArrayNative.create(buffer);
  }

  @Override
  public Int8ArrayNative createInt8Array(ArrayBuffer buffer, int byteOffset) {
    return Int8ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Int8ArrayNative createInt8Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Int8ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Int8ArrayNative createInt8Array(byte[] array) {
    return JsUtils.createInt8Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Int8ArrayNative createInt8Array(int length) {
    return Int8ArrayNative.create(length);
  }

  @Override
  public Uint16ArrayNative createUint16Array(ArrayBuffer buffer) {
    return Uint16ArrayNative.create(buffer);
  }

  @Override
  public Uint16ArrayNative createUint16Array(ArrayBuffer buffer, int byteOffset) {
    return Uint16ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Uint16ArrayNative createUint16Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Uint16ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Uint16ArrayNative createUint16Array(int length) {
    return Uint16ArrayNative.create(length);
  }

  @Override
  public Uint16ArrayNative createUint16Array(int[] array) {
    return JsUtils.createUint16Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Uint32ArrayNative createUint32Array(ArrayBuffer buffer) {
    return Uint32ArrayNative.create(buffer);
  }

  @Override
  public Uint32ArrayNative createUint32Array(ArrayBuffer buffer, int byteOffset) {
    return Uint32ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Uint32ArrayNative createUint32Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Uint32ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Uint32ArrayNative createUint32Array(double[] array) {
    return JsUtils.createUint32Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Uint32ArrayNative createUint32Array(int length) {
    return Uint32ArrayNative.create(length);
  }

  @Override
  public Uint32ArrayNative createUint32Array(long[] array) {
    int len = array.length;
    double[] temp = new double[len];
    for (int i = 0; i < len; ++i) {
      temp[i] = array[i];
    }
    return JsUtils.createUint32Array(JsArrayUtils.readOnlyJsArray(temp));
  }

  @Override
  public Uint8ArrayNative createUint8Array(ArrayBuffer buffer) {
    return Uint8ArrayNative.create(buffer);
  }

  @Override
  public Uint8ArrayNative createUint8Array(ArrayBuffer buffer, int byteOffset) {
    return Uint8ArrayNative.create(buffer, byteOffset);
  }

  @Override
  public Uint8ArrayNative createUint8Array(ArrayBuffer buffer, int byteOffset, int length) {
    return Uint8ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public Uint8ArrayNative createUint8Array(int length) {
    return Uint8ArrayNative.create(length);
  }

  @Override
  public Uint8ArrayNative createUint8Array(short[] array) {
    return JsUtils.createUint8Array(JsArrayUtils.readOnlyJsArray(array));
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(ArrayBuffer buffer) {
    if (checkUint8ClampedArraySupport()) {
      return Uint8ArrayNative.createClamped(buffer);
    } else {
      return Uint8ClampedArrayNativeEmul.create(buffer, 0, buffer.byteLength());
    }
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(ArrayBuffer buffer, int byteOffset) {
    if (checkUint8ClampedArraySupport()) {
      return Uint8ArrayNative.createClamped(buffer, byteOffset);
    } else {
      return Uint8ClampedArrayNativeEmul.create(buffer, byteOffset,
          buffer.byteLength() - byteOffset);
    }
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(ArrayBuffer buffer,
      int byteOffset, int length) {
    if (checkUint8ClampedArraySupport()) {
      return Uint8ArrayNative.createClamped(buffer, byteOffset, length);
    } else {
      return Uint8ClampedArrayNativeEmul.create(buffer, byteOffset, length);
    }
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(int length) {
    if (checkUint8ClampedArraySupport()) {
      return Uint8ArrayNative.createClamped(length);
    } else {
      return Uint8ClampedArrayNativeEmul.create(createArrayBuffer(length), 0, length);
    }
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(short[] array) {
    if (checkUint8ClampedArraySupport()) {
      return Uint8ArrayNative.createClamped(array);
    } else {
      return Uint8ClampedArrayNativeEmul.create(array);
    }
  }

  protected native boolean checkDataViewSupport() /*-{
    return !!(window.DataView);
  }-*/;

  protected native boolean checkUint8ClampedArraySupport() /*-{
    return !!(window.Uint8ClampedArray);
  }-*/;

  @Override
  protected native boolean runtimeSupportCheck() /*-{
      return !!(window.ArrayBuffer);
  }-*/;
}