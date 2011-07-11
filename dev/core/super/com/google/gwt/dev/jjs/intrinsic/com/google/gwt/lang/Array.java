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

import com.google.gwt.core.client.JavaScriptObject;

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
      for ( var name in protoType) {
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
      for ( var i = 0, c = expandoNames.length; i < c; ++i) {
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
    initValues(a.getClass(), Util.getCastableTypeMap(a), a.queryId, result);
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
    initValues(a.getClass(), Util.getCastableTypeMap(a), a.queryId, result);
    // implicit type arg not inferred (as of JDK 1.5.0_07)
    return Array.<T> asArray(result);
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   * 
   * @param arrayClass the class of the array
   * @param castableTypeMap the map of types to which this array can be casted,
   *          in the form of a JSON map object
   * @param queryId the queryId of the array
   * @param length the length of the array
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false; 3: long
   * @return the new array
   */
  public static Array initDim(Class<?> arrayClass, 
        JavaScriptObject castableTypeMap, int queryId, int length, int seedType) {
    Array result = createFromSeed(seedType, length);
    initValues(arrayClass, castableTypeMap, queryId, result);
    return result;
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   * 
   * @param arrayClasses the class of each dimension of the array
   * @param castableTypeMapExprs the JSON castableTypeMap of each dimension,
   *          from highest to lowest
   * @param queryIdExprs the queryId of each dimension, from highest to lowest
   * @param dimExprs the length of each dimension, from highest to lower
   * @param seedType the primitive type of the array; 0: null; 1: zero; 2: false; 3: long
   * @return the new array
   */
  public static Array initDims(Class<?> arrayClasses[], 
      JavaScriptObject[] castableTypeMapExprs, int[] queryIdExprs, 
      int[] dimExprs, int count, int seedType) {
    return initDims(arrayClasses, castableTypeMapExprs, queryIdExprs, 
        dimExprs, 0, count, seedType);
  }

  /**
   * Creates an array like "new T[][]{a,b,c,d}" by passing in a native JSON
   * array, [a, b, c, d].
   * 
   * @param arrayClass the class of the array
   * @param castableTypeMap the map of types to which this array can be casted,
   *          in the form of a JSON map object
   * @param queryId the queryId of the array
   * @param array the JSON array that will be transformed into a GWT array
   * @return values; having wrapped it for GWT
   */
  public static Array initValues(Class<?> arrayClass,
      JavaScriptObject castableTypeMap, int queryId, Array array) {
    ExpandoWrapper.wrapArray(array);
    setClass(array, arrayClass);
    Util.setCastableTypeMap(array, castableTypeMap);
    array.queryId = queryId;
    return array;
  }

  /**
   * Performs an array assignment, after validating the type of the value being
   * stored. The form of the type check depends on the value of queryId, as
   * follows:
   * <p>
   * If the queryId is > 0, this indicates a normal cast check should be
   * performed, using the queryId as the cast destination type.
   * JavaScriptObjects cannot be stored in this case.
   * <p>
   * If the queryId == 0, this is the cast target for the Object type, in which
   * case all types can be stored, including JavaScriptObject.
   * <p>
   * If the queryId == -1, this indicates that only JavaScriptObjects can be
   * stored (-1 is the cast target for JavaScriptObject, by convention).
   * <p>
   * If the queryId is < -1, this indicates that both JavaScriptObjects, and
   * Java types can be stored. In the case of Java types, the inverse of the
   * queryId is used for castability testing. This case is provided to support
   * arrays declared with an interface type, which has dual implementations
   * (i.e. interface types which have both Java and JavaScriptObject
   * implementations).
   * <p>
   * Note, by convention, a queryId of 1 is reserved for String, which is a
   * final class, and can't implement an interface, and thus, it's inverse, -1,
   * can safely be interpreted as a special case, as stated above.
   * <p>
   * Attempting to store an object that cannot satisfy the castability check
   * throws an {@link ArrayStoreException}.
   */
  public static Object setCheck(Array array, int index, Object value) {
    if (value != null) {
      if (array.queryId > 0 && !Cast.canCastUnsafe(value, array.queryId)) {
        // value must be castable to queryId
        throw new ArrayStoreException();
      } else if (array.queryId == -1 && Cast.isJavaObject(value)) {
        // value must be a JavaScriptObject
        throw new ArrayStoreException();
      } else if (array.queryId < -1 && !Cast.isJavaScriptObject(value)
          && !Cast.canCastUnsafe(value, -array.queryId)) {
        // value must be a JavaScriptObject, or else castable to the inverse of
        // queryId
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
   * @see #LONG_SEED_TYPE
   * @return the new JSON array
   */
  private static native Array createFromSeed(int seedType, int length) /*-{
    var array = new Array(length);
    if (seedType == 3) {
      // Fill array with the type used by LongLib
      for ( var i = 0; i < length; ++i) {
        var value = new Object();
        value.l = value.m = value.h = 0;
        array[i] = value;
      }
    } else if (seedType > 0) {
      var value = [null, 0, false][seedType];
      for ( var i = 0; i < length; ++i) {
        array[i] = value;
      }
    }
    return array;
  }-*/;

  private static Array initDims(Class<?> arrayClasses[],
      JavaScriptObject[] castableTypeMapExprs, int[] queryIdExprs, int[] dimExprs, 
      int index, int count, int seedType) {
    int length = dimExprs[index];
    boolean isLastDim = (index == (count - 1));

    Array result = createFromSeed(isLastDim ? seedType : NULL_SEED_TYPE, length);
    initValues(arrayClasses[index], castableTypeMapExprs[index], 
        queryIdExprs[index], result);

    if (!isLastDim) {
      // Recurse to next dimension.
      ++index;
      for (int i = 0; i < length; ++i) {
        set(result, i, initDims(arrayClasses, castableTypeMapExprs,
            queryIdExprs, dimExprs, index, count, seedType));
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

  // violator pattern so that the field remains private
  private static native void setClass(Object o, Class<?> clazz) /*-{
    o.@java.lang.Object::___clazz = clazz;
  }-*/;

  /*
   * Explicitly initialize all fields to JS false values; see comment in
   * ExpandoWrapper.initExpandos().
   */

  /**
   * A representation of the necessary cast target for objects stored into this
   * array.
   * 
   * @see #setCheck
   */
  protected int queryId = 0;
}
