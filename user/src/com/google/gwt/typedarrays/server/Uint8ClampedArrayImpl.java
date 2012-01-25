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
import com.google.gwt.typedarrays.shared.Uint8ClampedArray;

/**
 * Pure Java implementation of {@link Uint8ClampedArray}.
 */
public final class Uint8ClampedArrayImpl extends Uint8ArrayImpl implements Uint8ClampedArray {

  /**
   * @param buffer
   * @param byteOffset
   * @param byteLength
   */
  public Uint8ClampedArrayImpl(ArrayBuffer buffer, int byteOffset, int byteLength) {
    super(buffer, byteOffset, byteLength);
  }

  @Override
  public void set(int index, int value) {
    super.set(index, Math.max(0, Math.min(value, 255)));
  }

  @Override
  public Uint8ClampedArray subarray(int begin) {
    int count = byteLength() - byteOffset();
    return subarray(begin, count);
  }

  @Override
  public Uint8ClampedArray subarray(int begin, int end) {
    int count = byteLength() - byteOffset();
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
    return new Uint8ClampedArrayImpl(buffer(), begin, end);
  }
}
