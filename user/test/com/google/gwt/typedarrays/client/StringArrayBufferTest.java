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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint8Array;

/**
 * Test converting to/from a string encoding of an array buffer.
 */
public class StringArrayBufferTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.typedarrays.TypedArraysTest";
  }

  public void testFromString() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    String str = "\u0001\u0000\u0080\u0081";
    ArrayBuffer buf = JsUtils.arrayBufferFromString(str);
    Uint8Array view = TypedArrays.createUint8Array(buf);
    assertEquals(4, buf.byteLength());
    assertEquals(1, view.get(0));
    assertEquals(0, view.get(1));
    assertEquals(128, view.get(2));
    assertEquals(129, view.get(3));
  }

  public void testToString() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    Uint8Array view = TypedArrays.createUint8Array(4);
    view.set(0, 1);
    view.set(1, 0);
    view.set(2, 128);
    view.set(3, 129);
    String str = JsUtils.stringFromArrayBuffer(view.buffer());
    assertEquals(4, str.length());
    assertEquals(1, str.charAt(0));
    assertEquals(0, str.charAt(1));
    assertEquals(128, str.charAt(2));
    assertEquals(129, str.charAt(3));
  }
}
