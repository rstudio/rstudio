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
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Float64Array;
import com.google.gwt.typedarrays.shared.Int16Array;
import com.google.gwt.typedarrays.shared.Int32Array;
import com.google.gwt.typedarrays.shared.Int8Array;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.TypedArrays.Impl;
import com.google.gwt.typedarrays.shared.Uint16Array;
import com.google.gwt.typedarrays.shared.Uint32Array;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;

/**
 * Pure Java implementation class for typed arrays.
 */
public class JavaImpl extends Impl {

  @Override
  public ArrayBuffer createArrayBuffer(int length) {
    return new ArrayBufferImpl(length);
  }

  @Override
  public DataView createDataView(ArrayBuffer buffer, int byteOffset, int byteLength) {
    return new DataViewImpl(buffer, byteOffset, byteLength);
  }

  @Override
  public Float32Array createFloat32Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Float32ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Float32Array createFloat32Array(float[] array) {
    Float32Array result = TypedArrays.createFloat32Array(array.length);
    result.set(array);
    return result;
  }

  @Override
  public Float64Array createFloat64Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Float64ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Int16Array createInt16Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Int16ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Int32Array createInt32Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Int32ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Int8Array createInt8Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Int8ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Uint16Array createUint16Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Uint16ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Uint32Array createUint32Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Uint32ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Uint8Array createUint8Array(ArrayBuffer buffer, int byteOffset, int length) {
    return new Uint8ArrayImpl(buffer, byteOffset, length);
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(ArrayBuffer buffer, int byteOffset, int length) {
    return new Uint8ClampedArrayImpl(buffer, byteOffset, length);
  }

  @Override
  protected boolean runtimeSupportCheck() {
    return true;
  }
}
