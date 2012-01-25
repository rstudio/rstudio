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
 * Test {@link DataView} implementations.
 */
public class DataViewTest extends GWTTestCase {

  public void testBasic() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    int len = 40;
    ArrayBuffer buf = TypedArrays.createArrayBuffer(len);
    assertEquals(len, buf.byteLength());
    DataView view = TypedArrays.createDataView(buf);
    assertSame(buf, view.buffer());
    assertEquals(len, view.byteLength());
    assertEquals(0, view.byteOffset());
    
    // check that it is initialized to zeros
    for (int i = 0; i < len; ++i) {
      assertEquals(0, view.getUint8(i));
    }

    // store some data
    view.setUint8(0, 0x01);
    view.setUint8(1, 0x20);
    view.setUint8(2, 0x04);
    view.setUint8(3, 0x80);

    // read it out as bytes
    assertEquals(0x01, view.getUint8(0));
    assertEquals(0x20, view.getUint8(1));
    assertEquals(0x04, view.getUint8(2));
    assertEquals(0x80, view.getUint8(3));
    assertEquals(0x01, view.getInt8(0));
    assertEquals(0x20, view.getInt8(1));
    assertEquals(0x04, view.getInt8(2));
    assertEquals(-128, view.getInt8(3));

    // read it out as 16-bit values
    assertEquals(0x0120, view.getUint16(0));
    assertEquals(0x0480, view.getUint16(2));
    assertEquals(0x2001, view.getUint16(0, true));
    assertEquals(0x8004, view.getUint16(2, true));
    assertEquals(0x0120, view.getInt16(0));
    assertEquals(0x0480, view.getInt16(2));
    assertEquals(0x2001, view.getInt16(0, true));
    assertEquals((short) 0x8004, view.getInt16(2, true));

    // read it out as 32-bit values
    assertEquals(0x01200480L, view.getUint32(0));
    assertEquals(0x80042001L, view.getUint32(0, true));
    assertEquals(0x01200480, view.getInt32(0));
    assertEquals(0x80042001, view.getInt32(0, true));
  }

  @Override
  public String getModuleName() {
    // returns null for a pure Java test
    return null;
  }
}
