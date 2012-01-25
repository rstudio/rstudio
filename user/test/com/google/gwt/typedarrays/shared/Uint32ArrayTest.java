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
package com.google.gwt.typedarrays.shared;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test {@link Uint32Array} implementations.
 */
public class Uint32ArrayTest extends GWTTestCase {
  
  private static final int BYTES_PER_ELEMENT = Uint32Array.BYTES_PER_ELEMENT;

  protected void setFromJavaLongArray(Uint32Array array, int offset) {
    long[] values = new long[] {
        1, 2, 65536, -1,
    };
    array.set(values, offset);
  }

  protected void setFromJavaDoubleArray(Uint32Array array, int offset) {
    double[] values = new double[] {
        1, 2, 65536, -1,
    };
    array.set(values, offset);
  }

  protected void validateArrayContents(Uint32Array array, int offset) {
    for (int i = 0; i < offset; ++i) {
      assertEquals("index " + i, 0, array.get(i));
    }
    assertEquals("Index " + offset, 1, array.get(offset++));
    assertEquals("Index " + offset, 2, array.get(offset++));
    assertEquals("Index " + offset, 65536, array.get(offset++));
    assertEquals("Index " + offset, 0xFFFFFFFFL, array.get(offset++));
    for (int i = offset + 4; i < array.length(); ++i) {
      assertEquals("index " + i, 0, array.get(i));
    }
  }

  public void testBasic() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    int byteLen = 40;
    ArrayBuffer buf = TypedArrays.createArrayBuffer(byteLen);
    assertEquals(byteLen, buf.byteLength());
    DataView view = TypedArrays.createDataView(buf);
    Uint32Array array = TypedArrays.createUint32Array(buf);
    assertSame(buf, array.buffer());
    assertEquals(byteLen, array.byteLength());
    assertEquals(0, array.byteOffset());
    int len = byteLen / BYTES_PER_ELEMENT;
    assertEquals(len, array.length());
    
    // check that it is initialized to zeros
    for (int i = 0; i < len; ++i) {
      assertEquals(0, array.get(i));
    }

    // store some data
    for (int i = 0; i < len; ++i) {
      array.set(i, 0x04030200 + i);
    }

    // check the underlying buffer
    for (int i = 0; i < len; ++i) {
      assertEquals(i, view.getUint8(i * BYTES_PER_ELEMENT));
      assertEquals(2, view.getUint8(i * BYTES_PER_ELEMENT + 1));
      assertEquals(3, view.getUint8(i * BYTES_PER_ELEMENT + 2));
      assertEquals(4, view.getUint8(i * BYTES_PER_ELEMENT + 3));
    }

    // modify the underlying buffer and read it back
    view.setInt32(0, -256, true);
    view.setInt32(4, 128);
    assertEquals(0xFFFFFF00L, array.get(0));
    assertEquals(0x80000000L, array.get(1));
  }

  public void testSetFromJavaLongArray() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(24);
    Uint32Array array = TypedArrays.createUint32Array(buf);
    setFromJavaLongArray(array, 0);
    validateArrayContents(array, 0);

    // On Chrome, there is a bug where the first offset is ignored, so this test will fail if there
    // isn't a test with offset 0 first.
    // See http://code.google.com/p/chromium/issues/detail?id=109785
    buf = TypedArrays.createArrayBuffer(24);
    array = TypedArrays.createUint32Array(buf);
    setFromJavaLongArray(array, 1);
    validateArrayContents(array, 1);
  }

  public void testSetFromJavaDoubleArray() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(24);
    Uint32Array array = TypedArrays.createUint32Array(buf);
    setFromJavaDoubleArray(array, 0);
    validateArrayContents(array, 0);

    // On Chrome, there is a bug where the first offset is ignored, so this test will fail if there
    // isn't a test with offset 0 first.
    // See http://code.google.com/p/chromium/issues/detail?id=109785
    buf = TypedArrays.createArrayBuffer(24);
    array = TypedArrays.createUint32Array(buf);
    setFromJavaDoubleArray(array, 1);
    validateArrayContents(array, 1);
  }

  @Override
  public String getModuleName() {
    // returns null for a pure Java test
    return null;
  }
}
