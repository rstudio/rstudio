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
import com.google.gwt.typedarrays.shared.ArrayBufferView;

/**
 * Base class for {@link ArrayBufferView} implementations.
 */
public abstract class ArrayBufferViewImpl implements ArrayBufferView {

  /**
   * The spec lets the platform's native endianness come through, so we choose to
   * expose little-endian regardless -- if apps care about the endianness but don't
   * test for it, it is likely they assume little-endian.
   */
  static final boolean USE_LITTLE_ENDIAN = true;

  protected final ArrayBufferImpl arrayBuf;
  protected final int byteLength;
  protected final int byteOffset;

  /**
   * @param buffer
   * @param byteOffset
   * @param byteLength
   */
  public ArrayBufferViewImpl(ArrayBuffer buffer, int byteOffset, int byteLength) {
    if (!(buffer instanceof ArrayBufferImpl)) {
      throw new UnsupportedOperationException("Unacceptable ArrayBuffer type");
    }
    this.arrayBuf = (ArrayBufferImpl) buffer;
    this.byteOffset = byteOffset;
    this.byteLength = byteLength;
  }

  @Override
  public ArrayBuffer buffer() {
    return arrayBuf;
  }

  @Override
  public int byteLength() {
    return byteLength;
  }

  @Override
  public int byteOffset() {
    return byteOffset;
  }

  /**
   * Check the index range and throw an exception if out of range, if ok return the
   * byte index of the specified element.
   * 
   * @param index an element index
   * @param bytesPerElement
   * @return the byte index of the start of this element
   */
  protected int checkRange(int index, int bytesPerElement) {
    int byteIndex = index * bytesPerElement;
    if (byteIndex < byteOffset || byteIndex + bytesPerElement > byteLength) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return byteIndex;
  }
}
