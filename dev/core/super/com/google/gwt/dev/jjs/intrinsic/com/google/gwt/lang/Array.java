/*
 * Copyright 2008 Google Inc.
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

/**
 * This is a magic class the compiler uses as a base class for injected array
 * classes.
 */
public final class Array {

  private static final class ExpandoWrapper {
    /**
     * A JS array containing the names of any expandos we need to add to arrays
     * (such as "hashCode", "equals", "toString").
     */
    private static final Object expandoNames = makeEmptyJsArray();

    /**
     * A JS array containing the values of any expandos we need to add to arrays
     * (such as hashCode(), equals(), toString()).
     */
    private static final Object expandoValues = makeEmptyJsArray();

    static {
      initExpandos(new Array(), expandoNames, expandoValues);
    }

    public static void wrapArray(Array array) {
      wrapArray(array, expandoNames, expandoValues);
    }

    private static native void initExpandos(Array protoType,
        Object expandoNames, Object expandoValues) /*-{
      var i = 0, value;
      for (var name in protoType) {
        // Only copy non-null values over; this generally means only functions
        // will get copied over, and not fields, which is good because we will
        // setup the fields manually and it's best if length doesn't get blown
        // away.
        if (value = protoType[name]) {
          expandoNames[i] = name;
          expandoValues[i] = value;
          ++i;
        }
      }
    }-*/;

    private static native Object makeEmptyJsArray() /*-{
      return [];
    }-*/;

    private static native void wrapArray(Array array, Object expandoNames,
        Object expandoValues) /*-{
      for (var i = 0, c = expandoNames.length; i < c; ++i) {
        array[expandoNames[i]] = expandoValues[i];
      }
    }-*/;
  }

  /*
   * TODO: static init instead of lazy init when we can elide the clinit calls.
   */

  static final int FALSE_SEED_TYPE = 2;

  static final int LONG_SEED_TYPE = 3;

  static final int NULL_SEED_TYPE = 0;

  static final int ZERO_SEED_TYPE = 1;

  /**
   * Creates a copy of the specified array.
   */
  public static <T> T[] clone(T[] array) {
    return cloneSubrange(array, 0, array.length);
  }

  /**
   * Creates a copy of a subrange of the specified array.
   */
  public static <T> T[] cloneSubrange(T[] array, int fromIndex, int toIndex) {
    Array a = asArrayType(array);
    Array result = arraySlice(a, fromIndex, toIndex);
    initValues(a.getClass(), Util.getTypeId(a), a.queryId, result);
    // implicit type arg not inferred (as of JDK 1.5.0_07)
    return Array.<T> asArray(result);
  }

  /**
   * Creates a new array of the exact same type and length as a given array.
   */
  public static <T> T[] createFrom(T[] array) {
    return createFrom(array, array.length);
  }

  /**
   * Creates an empty array of the exact same type as a given array, with the
   * specified length.
   */
  public static <T> T[] createFrom(T[] array, int length) {
    Array a = asArrayType(array);
    Array result = createFromSeed(NULL_SEED_TYPE, length);
    initValues(a.getClass(), Util.getTypeId(a), a.queryId, result);
    // implicit type arg not inferred (as of JDK 1.5.0_07)
    return Array.<T> asArray(result);
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   * 
   * @param arrayClass the class of the array
   * @param typeId the typeId of the array
   * @param queryId the queryId of the array
   * @param length the length of the array
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false
   * @return the new array
   */
  public static Array initDim(Class arrayClass, int typeId, int queryId,
      int length, int seedType) {
    Array result = createFromSeed(seedType, length);
    initValues(arrayClass, typeId, queryId, result);
    return result;
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   * 
   * @param arrayClasses the class of each dimension of the array
   * @param typeIdExprs the typeId at each dimension, from highest to lowest
   * @param queryIdExprs the queryId at each dimension, from highest to lowest
   * @param dimExprs the length at each dimension, from highest to lower
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false
   * @return the new array
   */
  public static Array initDims(Class arrayClasses[], int[] typeIdExprs,
      int[] queryIdExprs, int[] dimExprs, int count, int seedType) {
    return initDims(arrayClasses, typeIdExprs, queryIdExprs, dimExprs, 0,
        count, seedType);
  }

  /**
   * Creates an array like "new T[][]{a,b,c,d}" by passing in a native JSON
   * array, [a, b, c, d].
   * 
   * @param arrayClass the class of the array
   * @param typeId the typeId of the array
   * @param queryId the queryId of the array
   * @param array the JSON array that will be transformed into a GWT array
   * @return values; having wrapped it for GWT
   */
  public static Array initValues(Class arrayClass, int typeId, int queryId,
      Array array) {
    ExpandoWrapper.wrapArray(array);
    array.arrayClass = arrayClass;
    Util.setTypeId(array, typeId);
    array.queryId = queryId;
    return array;
  }

  /**
   * Performs an array assignment, checking for valid index and type.
   */
  public static Object setCheck(Array array, int index, Object value) {
    if (value != null) {
      if (array.queryId > 0 && !Cast.canCastUnsafe(Util.getTypeId(value), array.queryId)) {
        throw new ArrayStoreException();
      }
      if (array.queryId < 0 && Cast.isJavaObject(value)) {
        throw new ArrayStoreException();
      }
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
   * @param seedType the primitive type of the array; 0: null; 1: zero;
   *     2: false; 3: (long) 0
   * @param length the requested length
   * @see #NULL_SEED_TYPE
   * @see #ZERO_SEED_TYPE
   * @see #FALSE_SEED_TYPE
   * @return the new JSON array
   */
  private static native Array createFromSeed(int seedType, int length) /*-{
    var array = new Array(length);
    if (seedType == 3) {
      // Fill array with the type used by LongLib
      for (var i = 0; i < length; ++i) {
        var value = new Object();
        value.l = value.m = value.h = 0;
        array[i] = value;
      }
    } else if (seedType > 0) {
      var value = [null, 0, false][seedType];
      for (var i = 0; i < length; ++i) {
        array[i] = value;
      }
    }
    return array;
  }-*/;

  private static Array initDims(Class arrayClasses[], int[] typeIdExprs,
      int[] queryIdExprs, int[] dimExprs, int index, int count, int seedType) {
    int length = dimExprs[index];
    boolean isLastDim = (index == (count - 1));

    Array result = createFromSeed(isLastDim ? seedType : NULL_SEED_TYPE, length);
    initValues(arrayClasses[index], typeIdExprs[index], queryIdExprs[index], result);

    if (!isLastDim) {
      // Recurse to next dimension.
      ++index;
      for (int i = 0; i < length; ++i) {
        set(result, i, initDims(arrayClasses, typeIdExprs, queryIdExprs, dimExprs,
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

  /*
   * Explicitly initialize all fields to JS false values; see comment in
   * ExpandoWrapper.initExpandos().
   */
  public volatile int length = 0;
  protected Class arrayClass = null;
  protected int queryId = 0;

  @Override
  public Class getClass() {
    return arrayClass;
  }
}
