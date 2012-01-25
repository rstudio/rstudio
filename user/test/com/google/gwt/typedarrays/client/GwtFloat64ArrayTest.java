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
import com.google.gwt.typedarrays.shared.Float64Array;
import com.google.gwt.typedarrays.shared.Float64ArrayTest;
import com.google.gwt.typedarrays.shared.TypedArrays;

/**
 * Test client {@link Float64Array} implementations.
 */
public class GwtFloat64ArrayTest extends Float64ArrayTest {

  @Override
  public String getModuleName() {
    return "com.google.gwt.typedarrays.TypedArraysTest";
  }

  public void testCreateJsArray() {
    if (!isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    JsArrayNumber src = getJsoArray();
    Float64Array array = JsUtils.createFloat64Array(src);
    validateArrayContents(array, 0);
  }

  public void testSetJsArray() {
    if (!isSupported()) {
      // TODO: some way of showing test as skipped in this case?
      return;
    }
    ArrayBuffer buf = TypedArrays.createArrayBuffer(48);
    Float64Array array = TypedArrays.createFloat64Array(buf);
    setFromJsArray(array, 0);
    validateArrayContents(array, 0);

    buf = TypedArrays.createArrayBuffer(48);
    array = TypedArrays.createFloat64Array(buf);
    setFromJsArray(array, 1);
    validateArrayContents(array, 1);
  }

  @Override
  protected boolean isSupported() {
    // Safari doesn't currently support Float64Array
    return super.isSupported() && !isSafari();
  }

  /**
   * Initialize from a JSO rather than a Java array
   */
  protected void setFromJsArray(Float64Array array, int offset) {
    JsUtils.set(array, getJsoArray(), offset);
  }

  private static native JsArrayNumber getJsoArray() /*-{
    return [ 1, Number.NEGATIVE_INFINITY, Number.NaN, Number.MAX_VALUE ];
  }-*/;

  private static native boolean isSafari() /*-{
    var ua = navigator.userAgent.toLowerCase();
    return ua.indexOf('safari/') != -1 && ua.indexOf('chrome/') == -1;
  }-*/;
}
