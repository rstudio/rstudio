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
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint16Array;
import com.google.gwt.typedarrays.shared.Uint16ArrayTest;

/**
 * Test client {@link Uint16Array} implementations.
 */
public class GwtUint16ArrayTest extends Uint16ArrayTest {

  private static native JsArrayInteger getJsoArray() /*-{
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
    JsArrayInteger src = getJsoArray();
    Uint16Array array = JsUtils.createUint16Array(src);
    validateArrayContents(array, 0);
  }

  public void testSetJsArray() {
    if (!TypedArrays.isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(12);
    Uint16Array array = TypedArrays.createUint16Array(buf);
    setFromJsArray(array, 0);
    validateArrayContents(array, 0);

    buf = TypedArrays.createArrayBuffer(12);
    array = TypedArrays.createUint16Array(buf);
    setFromJsArray(array, 1);
    validateArrayContents(array, 1);
  }

  /**
   * Initialize from a JSO rather than a Java array
   */
  protected void setFromJsArray(Uint16Array array, int offset) {
    JsUtils.set(array, getJsoArray(), offset);
  }
}
