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
import com.google.gwt.typedarrays.shared.Int8Array;

/**
 * Pure Java implementation of {@link Int8Array}.
 */
public final class Int8ArrayImpl extends ArrayBufferViewImpl implements Int8Array {

  /**
   * @param buffer
   * @param byteOffset
   * @param byteLength
   */
  public Int8ArrayImpl(ArrayBuffer buffer, int byteOffset, int byteLength) {
    super(buffer, byteOffset, byteLength);
  }

  @Override
  public byte get(int index) {
    return arrayBuf.getInt8(checkRange(index, BYTES_PER_ELEMENT));
  }

  @Override
  public int length() {
    return byteLength() / BYTES_PER_ELEMENT;
  }

  @Override
  public void set(byte[] array) {
    set(array, 0);
  }

  @Override
  public void set(byte[] array, int offset) {
    int len = array.length;
    if (offset + len > length()) {
      throw new IndexOutOfBoundsException();
    }
    for (int i = 0; i < len; ++i) {
      set(offset++, array[i]);
    }
  }

  @Override
  public void set(int index, int value) {
    arrayBuf.setInt8(checkRange(index, BYTES_PER_ELEMENT), (byte) (value & 0xFF));
  }

  @Override
  public void set(int[] array) {
    set(array, 0);
  }

  @Override
  public void set(int[] array, int offset) {
    int len = array.length;
    if (offset + len > length()) {
      throw new IndexOutOfBoundsException();
    }
    for (int i = 0; i < len; ++i) {
      set(offset++, array[i]);
    }
  }

  @Override
  public void set(Int8Array array) {
    set(array, 0);
  }

  @Override
  public void set(Int8Array array, int offset) {
    int len = array.length();
    if (offset + len > length()) {
      throw new IndexOutOfBoundsException();
    }
    for (int i = 0; i < len; ++i) {
      set(offset++, array.get(i));
    }
  }

  @Override
  public Int8Array subarray(int begin) {
    int count = (byteLength() - byteOffset()) / BYTES_PER_ELEMENT;
    return subarray(begin, count);
  }

  @Override
  public Int8Array subarray(int begin, int end) {
    int count = (byteLength() - byteOffset()) / BYTES_PER_ELEMENT;
    if (begin < 0) {
      begin += count;
      if (begin < 0) {
        begin = 0;
      }
    } else if (begin > count) {
      begin = count;
    }
    if (end < 0) {
      end += count;
      if (end < 0) {
        end = 0;
      }
    } else if (end > count) {
      end = count;
    }
    if (end < begin) {
      end = begin;
    }
    return new Int8ArrayImpl(arrayBuf, begin * BYTES_PER_ELEMENT, end * BYTES_PER_ELEMENT);
  }
}
