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
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;

/**
 * Emulated version of {@link Uint8ClampedArray} that is implemented using a {@link Uint8Array}.
 */
public class Uint8ClampedArrayNativeEmul implements Uint8ClampedArray {

  /**
   * Create a {@link Uint8ClampedArray} instance on an existing
   * {@link ArrayBuffer}.
   * 
   * @param buffer
   * @param byteOffset
   * @param length
   * @return {@link Uint8ClampedArray} instance
   */
  public static Uint8ClampedArray create(ArrayBuffer buffer, int byteOffset, int length) {
    return new Uint8ClampedArrayNativeEmul(buffer, byteOffset, length);
  }

  /**
   * Create a {@link Uint8ClampedArray} instance from a short array.
   * 
   * @param array
   * @return {@link Uint8ClampedArray} instance
   */
  public static Uint8ClampedArray create(short[] array) {
    int len = array.length;
    Uint8ClampedArray result = create(ArrayBufferNative.create(len), 0, len);
    result.set(array);
    return result;
  }

  /**
   * Create a {@link Uint8ClampedArray} instance from a JavaScript array
   * containing integers.
   * 
   * @param array JavaScript array object
   * @return {@link Uint8ClampedArray} instance
   */
  public static Uint8ClampedArray create(JsArrayInteger array) {
    int len = array.length();
    Uint8ClampedArray result = create(ArrayBufferNative.create(len), 0, len);
    JsUtils.set(result, array);
    return result;
  }

  private static short clamp(int val) {
    return (short) Math.max(0, Math.min(val, 255));
  }

  private final Uint8Array real;

  /**
   * Internal constructor for creating an {@link Uint8ClampedArrayNativeEmul}
   * on an existing {@link ArrayBuffer} instance. 
   * 
   * @param buffer
   * @param byteOffset
   * @param length
   */
  protected Uint8ClampedArrayNativeEmul(ArrayBuffer buffer, int byteOffset, int length) {
    real = Uint8ArrayNative.create(buffer, byteOffset, length);
  }

  @Override
  public ArrayBuffer buffer() {
    return real.buffer();
  }

  @Override
  public int byteLength() {
    return real.byteLength();
  }

  @Override
  public int byteOffset() {
    return real.byteOffset();
  }

  @Override
  public short get(int index) {
    return real.get(index);
  }

  @Override
  public int length() {
    return real.length();
  }

  @Override
  public void set(int index, int value) {
    real.set(index, clamp(value));
  }

  @Override
  public void set(int[] array) {
    set(array, 0);
  }

  @Override
  public void set(int[] array, int offset) {
    int len = array.length;
    for (int i = 0; i < len; ++i) {
      real.set(offset++, clamp(array[i]));
    }
  }

  @Override
  public void set(short[] array) {
    set(array, 0);
  }

  @Override
  public void set(short[] array, int offset) {
    int len = array.length;
    for (int i = 0; i < len; ++i) {
      real.set(offset++, clamp(array[i]));
    }
  }

  @Override
  public void set(Uint8Array array) {
    set(array, 0);
  }

  @Override
  public void set(Uint8Array array, int offset) {
    int len = array.length();
    for (int i = 0; i < len; ++i) {
      real.set(offset++, clamp(array.get(i)));
    }
  }

  @Override
  public Uint8ClampedArray subarray(int begin) {
    return subarray(begin, byteLength());
  }

  @Override
  public Uint8ClampedArray subarray(int begin, int end) {
    int len = byteLength();
    if (begin < 0) {
      begin += len;
    }
    if (end < 0) {
      end += len;
    }
    if (begin < 0) {
      begin = 0;
    } else if (begin > len) {
      begin = len;
    }
    if (end < begin) {
      end = begin;
    } else if (end > len) {
      end = len;
    }
    return new Uint8ClampedArrayNativeEmul(buffer(), byteOffset() + begin, end - begin);
  }
}
