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
 * Test {@link Int16Array} implementations.
 */
public class Int16ArrayTest extends GWTTestCase {
  
  private static final int BYTES_PER_ELEMENT = Int16Array.BYTES_PER_ELEMENT;

  protected void setFromJavaIntArray(Int16Array array, int offset) {
    int[] values = new int[] {
        1, 2, 65536, -1,
    };
    array.set(values, offset);
  }

  protected void validateArrayContents(Int16Array array, int offset) {
    for (int i = 0; i < offset; ++i) {
      assertEquals("index " + i, 0, array.get(i));
    }
    assertEquals("Index " + offset, 1, array.get(offset++));
    assertEquals("Index " + offset, 2, array.get(offset++));
    assertEquals("Index " + offset, 0, array.get(offset++));
    assertEquals("Index " + offset, -1, array.get(offset++));
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
    Int16Array array = TypedArrays.createInt16Array(buf);
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
      array.set(i, 0x200 + i);
    }

    // check the underlying buffer
    for (int i = 0; i < len; ++i) {
      assertEquals(i, view.getUint8(i * BYTES_PER_ELEMENT));
      assertEquals(2, view.getUint8(i * BYTES_PER_ELEMENT + 1));
    }

    // modify the underlying buffer and read it back
    view.setInt16(0, -256, true);
    view.setInt16(2, 128);
    assertEquals(-256, array.get(0));
    assertEquals(-32768, array.get(1));
  }

  public void testSetFromJavaArray() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(12);
    Int16Array array = TypedArrays.createInt16Array(buf);
    setFromJavaIntArray(array, 0);
    validateArrayContents(array, 0);

    // On Chrome, there is a bug where the first offset is ignored, so this test will fail if there
    // isn't a test with offset 0 first.
    // See http://code.google.com/p/chromium/issues/detail?id=109785
    buf = TypedArrays.createArrayBuffer(12);
    array = TypedArrays.createInt16Array(buf);
    setFromJavaIntArray(array, 1);
    validateArrayContents(array, 1);
  }

  @Override
  public String getModuleName() {
    // returns null for a pure Java test
    return null;
  }
}
