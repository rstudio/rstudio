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
package com.google.gwt.core.client;

/**
 * Utility class for manipulating JS arrays.  These methods are not on other
 * JavaScriptObject subclasses, such as JsArray, because adding new methods might
 * break existing subtypes.
 */
public class JsArrayUtils {

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * 
   * @param array source array
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static JsArrayInteger readOnlyJsArray(byte[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArrayInteger dest = JsArrayInteger.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * 
   * @param array source array
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static JsArrayNumber readOnlyJsArray(double[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArrayNumber dest = JsArrayNumber.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * 
   * @param array source array
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static JsArrayNumber readOnlyJsArray(float[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArrayNumber dest = JsArrayNumber.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * 
   * @param array source array
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static JsArrayInteger readOnlyJsArray(int[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArrayInteger dest = JsArrayInteger.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * <p>
   * <b>NOTE: long values are not supported in JS, so long emulation is slow
   * and this method assumes that all the values can be safely stored in a
   * double.</b>
   * 
   * @param array source array - its values are assumed to be in the valid range
   *     for doubles -- if the values exceed 2^53, low-order bits will be lost
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static JsArrayNumber readOnlyJsArray(long[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArrayNumber dest = JsArrayNumber.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * 
   * @param array source array
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static JsArrayInteger readOnlyJsArray(short[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArrayInteger dest = JsArrayInteger.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * Take a Java array, and produce a JS array that is only used for reading.  As
   * this is actually a reference to the original array in prod mode, the source
   * must not be modified while this copy is in use or you will get different
   * behavior between DevMode and prod mode.
   * 
   * @param array source array
   * @return JS array, which may be a copy or an alias of the input array
   */
  public static <T extends JavaScriptObject> JsArray<T> readOnlyJsArray(T[] array) {
    if (GWT.isScript()) {
      return arrayAsJsArrayForProdMode(array).cast();
    }
    JsArray<T> dest = JavaScriptObject.createArray().cast();
    for (int i = 0; i < array.length; ++i) {
      dest.push(array[i]);
    }
    return dest;
  }

  /**
   * In production mode, Java arrays really are JS arrays, so just return it.
   * 
   * @param array must be a Java array of some type
   * @return a JavaScriptObject, which should be used as the appropriate type of
   *     JS array depending on the input array type
   */
  private static native JavaScriptObject arrayAsJsArrayForProdMode(Object array) /*-{
    return array;
  }-*/;

  private JsArrayUtils() {
  }

}
