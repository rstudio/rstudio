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
 * Test {@link Float64Array} implementations.
 */
public class Float64ArrayTest extends GWTTestCase {
  
  private static final int BYTES_PER_ELEMENT = Float64Array.BYTES_PER_ELEMENT;

  @Override
  public String getModuleName() {
    // returns null for a pure Java test
    return null;
  }

  public void testBasic() {
    if (!isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    int byteLen = 40;
    ArrayBuffer buf = TypedArrays.createArrayBuffer(byteLen);
    assertEquals(byteLen, buf.byteLength());
    DataView view = TypedArrays.createDataView(buf);
    Float64Array array = TypedArrays.createFloat64Array(buf);
    assertSame(buf, array.buffer());
    assertEquals(byteLen, array.byteLength());
    assertEquals(0, array.byteOffset());
    int len = byteLen / BYTES_PER_ELEMENT;
    assertEquals(len, array.length());
    
    // check that it is initialized to zeros
    for (int i = 0; i < len; ++i) {
      assertEquals(0.0, array.get(i));
    }

    // store some data
    for (int i = 0; i < len; ++i) {
      array.set(i, 1 << (i + 1));
    }

    // check the underlying buffer
    for (int i = 0; i < len; ++i) {
      assertEquals("Byte " + i + ":0", 0, view.getUint8(i * BYTES_PER_ELEMENT));
      assertEquals("Byte " + i + ":1", 0, view.getUint8(i * BYTES_PER_ELEMENT + 1));
      assertEquals("Byte " + i + ":2", 0, view.getUint8(i * BYTES_PER_ELEMENT + 2));
      assertEquals("Byte " + i + ":3", 0, view.getUint8(i * BYTES_PER_ELEMENT + 3));
      assertEquals("Byte " + i + ":4", 0, view.getUint8(i * BYTES_PER_ELEMENT + 4));
      assertEquals("Byte " + i + ":5", 0, view.getUint8(i * BYTES_PER_ELEMENT + 5));
      assertEquals("Byte " + i + ":6", (i & 15) << 4,
          view.getUint8(i * BYTES_PER_ELEMENT + 6));
      assertEquals("Byte " + i + ":7", 64 + (i >> 4),
          view.getUint8(i * BYTES_PER_ELEMENT + 7));
    }

    // modify the underlying buffer and read it back
    view.setFloat64(0, -256, true);
    view.setInt32(12, 0x0000F03F);
    view.setInt32(8, 0);
    assertEquals(-256.0, array.get(0));
    assertEquals(1.0, array.get(1));
  }

  public void testSetFromJavaArray() {
    if (!isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(48);
    Float64Array array = TypedArrays.createFloat64Array(buf);
    setFromJavaArray(array, 0);
    validateArrayContents(array, 0);

    // On Chrome, there is a bug where the first offset is ignored, so this test will fail if there
    // isn't a test with offset 0 first.
    // See http://code.google.com/p/chromium/issues/detail?id=109785
    buf = TypedArrays.createArrayBuffer(48);
    array = TypedArrays.createFloat64Array(buf);
    setFromJavaArray(array, 1);
    validateArrayContents(array, 1);
  }

  /**
   * Not all browsers support Float64Array despite otherwise supporting typed arrays.
   * 
   * @return true if the current environment supports Float64Array
   */
  protected boolean isSupported() {
    return TypedArrays.isSupported();
  }

  protected void setFromJavaArray(Float64Array array, int offset) {
    double[] values = new double[] {
        1.0, Double.NEGATIVE_INFINITY, Double.NaN, Double.MAX_VALUE,
    };
    array.set(values, offset);
  }

  protected void validateArrayContents(Float64Array array, int offset) {
    for (int i = 0; i < offset; ++i) {
      assertEquals("index " + i, 0.0, array.get(i));
    }
    assertEquals("Index " + offset, 1.0, array.get(offset++));
    assertTrue("Index " + offset, Double.isInfinite(array.get(offset)));
    assertTrue("Index " + offset, array.get(offset++) < 0);
    assertTrue("Index " + offset, Double.isNaN(array.get(offset++)));
    assertEquals("Index " + offset, Double.MAX_VALUE, array.get(offset++));
    for (int i = offset + 4; i < array.length(); ++i) {
      assertEquals("index " + i, 0.0, array.get(i));
    }
  }
}
