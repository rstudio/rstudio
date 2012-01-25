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
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Float64Array;
import com.google.gwt.typedarrays.shared.Int16Array;
import com.google.gwt.typedarrays.shared.Int32Array;
import com.google.gwt.typedarrays.shared.Int8Array;
import com.google.gwt.typedarrays.shared.Uint16Array;
import com.google.gwt.typedarrays.shared.Uint32Array;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;
import com.google.gwt.typedarrays.shared.TypedArrays.Impl;

/**
 * The implementation class for browsers known to have no
 * support (even emulated) for typed arrays.
 */
public class NoSupportImpl extends Impl {

  @Override
  public ArrayBuffer createArrayBuffer(int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public DataView createDataView(ArrayBuffer buffer, int byteOffset,
      int byteLength) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Float32Array createFloat32Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Float32Array createFloat32Array(float[] array) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Float64Array createFloat64Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Int16Array createInt16Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Int32Array createInt32Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Int8Array createInt8Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Uint16Array createUint16Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Uint32Array createUint32Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Uint8Array createUint8Array(ArrayBuffer buffer, int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  public Uint8ClampedArray createUint8ClampedArray(ArrayBuffer buffer,
      int byteOffset, int length) {
    throw new UnsupportedOperationException("typed arrays not supported");
  }

  @Override
  protected boolean mightBeSupported() {
    return false;
  }
}