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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint32Array;
import com.google.gwt.typedarrays.shared.Uint32ArrayTest;

/**
 * Test client {@link Uint32Array} implementations.
 */
public class GwtUint32ArrayTest extends Uint32ArrayTest {

  private static native JsArrayNumber getJsoArray() /*-{
    return [ 1, 2, 65536, -1 ];
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.typedarrays.TypedArraysTest";
  }

  public void testCreateJsArray() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    JsArrayNumber src = getJsoArray();
    Uint32Array array = JsUtils.createUint32Array(src);
    validateArrayContents(array, 0);
  }

  public void testSetJsArray() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(24);
    Uint32Array array = TypedArrays.createUint32Array(buf);
    setFromJsArray(array, 0);
    validateArrayContents(array, 0);

    buf = TypedArrays.createArrayBuffer(24);
    array = TypedArrays.createUint32Array(buf);
    setFromJsArray(array, 1);
    validateArrayContents(array, 1);
  }

  /**
   * Initialize from a JSO rather than a Java array
   */
  protected void setFromJsArray(Uint32Array array, int offset) {
    JsUtils.set(array, getJsoArray(), offset);
  }
}
