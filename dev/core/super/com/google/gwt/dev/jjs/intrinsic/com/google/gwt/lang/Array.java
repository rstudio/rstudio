/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This is a magic class the compiler uses as a base class for injected array
 * classes.
 */
public final class Array {

  /*
   * TODO: static init instead of lazy init when we can elide the clinit calls.
   */

  static final int FALSE_SEED_TYPE = 2;

  static final int NULL_SEED_TYPE = 0;

  static final int ZERO_SEED_TYPE = 1;

  /**
   * Stores the prototype for java.lang.Object so that arrays can get their
   * polymorphic methods via expando.
   */
  private static JavaScriptObject protoTypeObject;

  /**
   * Creates a copy of a subrange of the specified array.
   */
  public static <T> T[] cloneSubrange(T[] array, int fromIndex, int toIndex) {
    Array a = asArrayType(array);
    Array result = arraySlice(a, fromIndex, toIndex);
    initValues(a.typeName, a.typeId, a.queryId, result);
    return asArray(result);
  }

  /**
   * Creates a new array of the exact same type as a given array but with the
   * specified length.
   */
  public static <T> T[] clonify(T[] array, int length) {
    Array a = asArrayType(array);
    Array result = createFromSeed(NULL_SEED_TYPE, length);
    initValues(a.typeName, a.typeId, a.queryId, result);
    return asArray(result);
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   * 
   * @param typeName the typeName of the array
   * @param typeId the typeId of the array
   * @param queryId the queryId of the array
   * @param length the length of the array
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false
   * @return the new array
   */
  public static Array initDim(String typeName, int typeId, int queryId,
      int length, int seedType) {
    Array result = createFromSeed(seedType, length);
    initValues(typeName, typeId, queryId, result);
    return result;
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   * 
   * @param typeName the typeName of the array
   * @param typeIdExprs the typeId at each dimension, from highest to lowest
   * @param queryIdExprs the queryId at each dimension, from highest to lowest
   * @param dimExprs the length at each dimension, from highest to lower
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false
   * @return the new array
   */
  public static Array initDims(String typeName, int[] typeIdExprs,
      int[] queryIdExprs, int[] dimExprs, int seedType) {
    return initDims(typeName, typeIdExprs, queryIdExprs, dimExprs, 0,
        dimExprs.length, seedType);
  }

  /**
   * Creates an array like "new T[][]{a,b,c,d}" by passing in a native JSON
   * array, [a, b, c, d].
   * 
   * @param typeName the typeName of the array
   * @param typeId the typeId of the array
   * @param queryId the queryId of the array
   * @param array the JSON array that will be transformed into a GWT array
   * @return values; having wrapped it for GWT
   */
  public static final Array initValues(String typeName, int typeId,
      int queryId, Array array) {
    if (protoTypeObject == null) {
      protoTypeObject = getPrototype(new Object());
    }
    wrapArray(array, protoTypeObject);
    array.typeName = typeName;
    array.typeId = typeId;
    array.queryId = queryId;
    return array;
  }

  /**
   * Performs an array assignment, checking for valid index and type.
   */
  public static Object setCheck(Array array, int index, Object value) {
    if (value != null && array.queryId != 0
        && !Cast.instanceOf(value, array.queryId)) {
      throw new ArrayStoreException();
    }
    return set(array, index, value);
  }

  private static native Array arraySlice(Array array, int fromIndex, int toIndex) /*-{
    return array.slice(fromIndex, toIndex);
  }-*/;
  
  /**
   * Use JSNI to effect a castless type change.
   */
  private static native <T> T[] asArray(Array array) /*-{
    return array;
  }-*/;

  /**
   * Use JSNI to effect a castless type change.
   */
  private static native <T> Array asArrayType(T[] array) /*-{
    return array;
  }-*/;

  /**
   * Creates a primitive JSON array of a given seedType.
   * 
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false
   * @param length the requested length
   * @see #NULL_ARRAY
   * @see #ZERO_ARRAY
   * @see #FALSE_ARRAY
   * @return the new JSON array
   */
  private static native Array createFromSeed(int seedType, int length) /*-{
    var seedArrays = [[null], [0], [false]];
    var blankArray = seedArrays[seedType];

    while (blankArray.length < length) {
      // Doubles each iteration.
      blankArray = blankArray.concat(blankArray);
    }
    
    // Remove the excess.
    blankArray.length = length;
    return blankArray;
  }-*/;

  private static native JavaScriptObject getPrototype(Object object) /*-{
    object.constructor.prototype;
  }-*/;

  private static Array initDims(String typeName, int[] typeIdExprs,
      int[] queryIdExprs, int[] dimExprs, int index, int count, int seedType) {
    int length = dimExprs[index];
    if (length < 0) {
      throw new NegativeArraySizeException();
    }

    boolean isLastDim = (index == (count - 1));

    Array result = createFromSeed(isLastDim ? seedType : NULL_SEED_TYPE, length);
    initValues(typeName, typeIdExprs[index], queryIdExprs[index], result);

    if (!isLastDim) {
      // Recurse to next dimension.
      ++index;
      typeName = typeName.substring(1);
      for (int i = 0; i < length; ++i) {
        set(result, i, initDims(typeName, typeIdExprs, queryIdExprs, dimExprs,
            index, count, seedType));
      }
    }
    return result;
  }

  /**
   * Sets a value in the array.
   */
  private static native Object set(Array array, int index, Object value) /*-{
    return array[index] = value;
  }-*/;

  private static native Array wrapArray(Array array, JavaScriptObject prototype) /*-{
    for (var i in prototype) {
      array[i] = prototype[i];
    }
    return array;
  }-*/;

  public int length;

  protected int queryId;
}
