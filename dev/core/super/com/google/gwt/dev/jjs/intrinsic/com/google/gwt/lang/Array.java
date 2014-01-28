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

  // Array initialization values types
  private static final int INIT_TO_NULL = 0;
  private static final int INIT_TO_ZERO_INT  = 1;
  private static final int INIT_TO_FALSE = 2;
  private static final int INIT_TO_ZERO_LONG = 3;

  // Array element type classes
  private static final int TYPE_JAVA_OBJECT = 0;
  private static final int TYPE_JAVA_OBJECT_OR_JSO = 1;
  private static final int TYPE_JSO = 2;
  private static final int TYPE_JAVA_LANG_OBJECT = 3;

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
    initValues(a.getClass(), Util.getCastableTypeMap(a), a.elementTypeId,
        a.elementTypeClass, result);
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
    Array result = initializeArrayElementsWithDefaults(INIT_TO_NULL, length);
    initValues(a.getClass(), Util.getCastableTypeMap(a), a.elementTypeId,
        a.elementTypeClass, result);
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
   * @param elementTypeId the typeId of array elements
   * @param elementTypeClass whether the element type is java.lang.Object (TYPE_JAVA_LANG_OBJECT),
   *        is guaranteed to be a java object (TYPE_JAVA_OBJECT), is guaranteed to be a JSO
   *        (TYPE_JSO) or can be either (TYPE_JAVA_OBJECT_OR_JSO).
   * @param length the length of the array
   * @param initValueType what is the initial value for elements;
   *           INIT_TO_NULL: null; INIT_TO_ZERO_INT: zero; INIT_TO_FALSE: false;
   *           INIT_TO_ZERO_LONG: long
   * @return the new array
   */
  public static Array initDim(Class<?> arrayClass, JavaScriptObject castableTypeMap,
      int elementTypeId, int length, int elementTypeClass, int initValueType) {
    Array result = initializeArrayElementsWithDefaults(initValueType, length);
    initValues(arrayClass, castableTypeMap, elementTypeId, elementTypeClass, result);
    return result;
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   *
   * @param arrayClasses the class of each dimension of the array
   * @param castableTypeMapExprs the JSON castableTypeMap of each dimension,
   *          from highest to lowest
   * @param elementTypeIds the elementTypeId of each dimension, from highest to lowest
   * @param leafElementTypeClass whether the leaf element type is java.lang.Object
   *        (TYPE_JAVA_LANG_OBJECT), is guaranteed to be a java object (TYPE_JAVA_OBJECT),
   *        is guaranteed to be a JSO (TYPE_JSO) or can be either (TYPE_JAVA_OBJECT_OR_JSO).
   * @param dimExprs the length of each dimension, from highest to lower
   * @param leafElementInitValueType what is the initial value for leaf elements;
   *           INIT_TO_NULL: null; INIT_TO_ZERO_INT: zero; INIT_TO_FALSE: false;
   *           INIT_TO_ZERO_LONG: long
   * @return the new array
   */
  public static Array initDims(Class<?> arrayClasses[],
      JavaScriptObject[] castableTypeMapExprs, int[] elementTypeIds,
      int leafElementTypeClass, int[] dimExprs, int count, int leafElementInitValueType) {
    return initDims(arrayClasses, castableTypeMapExprs, elementTypeIds, leafElementTypeClass,
        dimExprs, 0, count, leafElementInitValueType);
  }

  /**
   * Creates an array like "new T[][]{a,b,c,d}" by passing in a native JSON
   * array, [a, b, c, d].
   *
   * @param arrayClass the class of the array
   * @param castableTypeMap the map of types to which this array can be casted,
   *          in the form of a JSON map object
   * @param elementTypeId the typeId of array elements
   * @param elementTypeClass whether the element type is java.lang.Object
   *        ({@link TYPE_JAVA_LANG_OBJECT}), is guaranteed to be a java object
   *        ({@link TYPE_JAVA_OBJECT}), is guaranteed to be a JSO
   *        ({@link TYPE_JSO}) or can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}).
   * @param array the JSON array that will be transformed into a GWT array
   * @return values; having wrapped it for GWT
   */
  public static Array initValues(Class<?> arrayClass, JavaScriptObject castableTypeMap,
      int elementTypeId, int elementTypeClass, Array array) {
    ExpandoWrapper.wrapArray(array);
    setClass(array, arrayClass);
    Util.setCastableTypeMap(array, castableTypeMap);
    array.elementTypeId = elementTypeId;
    array.elementTypeClass = elementTypeClass;
    return array;
  }

  /**
   * Performs an array assignment, after validating the type of the value being
   * stored. The form of the type check depends on the value of elementTypeId and elementTypeClass
   * as follows:
   * <p>
   * If the elementTypeClass is {@link TYPE_JAVA_OBJECT}, this indicates a normal cast check should
   * be performed, using the elementTypeId as the cast destination type.
   * JavaScriptObjects cannot be stored in this case.
   * <p>
   * If the elementTypeId is {@link TYPE_JAVA_LANG_OBJECT}, this is the cast target for the Object
   * type, in which case all types can be stored, including JavaScriptObject.
   * <p>
   * If the elementTypeId is {@link TYPE_JSO}, this indicates that only JavaScriptObjects can be
   * stored.
   * <p>
   * If the elementTypeId is {@link TYPE_JAVA_OBJECT_OR_JSO}, this indicates that both
   * JavaScriptObjects, and Java types can be stored. In the case of Java types, a normal cast check
   * should be performed, using the elementTypeId as the cast destination type.
   * This case is provided to support arrays declared with an interface type, which has dual
   * implementations (i.e. interface types which have both Java and JavaScriptObject
   * implementations).
   * <p>
   * Attempting to store an object that cannot satisfy the castability check
   * throws an {@link ArrayStoreException}.
   */
  public static Object setCheck(Array array, int index, Object value) {
    if (value != null) {
      if (array.elementTypeClass == TYPE_JAVA_OBJECT
          && !Cast.canCast(value, array.elementTypeId)) {
        // value must be castable to elementType.
        throw new ArrayStoreException();
      } else if (array.elementTypeClass == TYPE_JSO && Cast.isJavaObject(value)) {
        // value must be a JavaScriptObject
        throw new ArrayStoreException();
      } else if (array.elementTypeClass == TYPE_JAVA_OBJECT_OR_JSO
          && !Cast.isJavaScriptObject(value)
          && !Cast.canCast(value, array.elementTypeId)) {
        // value must be a JavaScriptObject, or else castable to the elementType.
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
   * @see #INIT_TO_NULL
   * @see #INIT_TO_ZERO_INT
   * @see #INIT_TO_FALSE
   * @see #INIT_TO_ZERO_LONG
   * @return the new JSON array
   */
  private static native Array initializeArrayElementsWithDefaults(int initValueType,
      int length) /*-{
    var array = new Array(length);
    if (initValueType == @com.google.gwt.lang.Array::INIT_TO_NULL) {
      // Do not initialize as undefined is equivalent to null
      return array;
    }

    var initValue;
    if (initValueType == @com.google.gwt.lang.Array::INIT_TO_ZERO_LONG) {
      // Fill array with the type used by LongLib
      initValue = {l: 0, m: 0, h:0};
    } else if (initValueType ==  @com.google.gwt.lang.Array::INIT_TO_ZERO_INT) {
      initValue = 0;
    } else { // initValueType == @com.google.gwt.lang.Array::INIT_TO_FALSE
      initValue = false;
    }

    for ( var i = 0; i < length; ++i) {
      array[i] = initValue;
    }
    return array;
  }-*/;

  private static Array initDims(Class<?> arrayClasses[], JavaScriptObject[] castableTypeMapExprs,
      int[] elementTypeIds, int leafElementTypeClass, int[] dimExprs,
      int index, int count, int leafInitValueType) {
    int length = dimExprs[index];
    boolean isLastDim = (index == (count - 1));

    // All dimensions but the last are reference types hence initied to null
    Array result = initializeArrayElementsWithDefaults(isLastDim ? leafInitValueType :
       INIT_TO_NULL, length);
    initValues(arrayClasses[index], castableTypeMapExprs[index],
        elementTypeIds[index], isLastDim ? leafElementTypeClass : TYPE_JAVA_OBJECT, result);

    if (!isLastDim) {
      // Recurse to next dimension.
      ++index;
      for (int i = 0; i < length; ++i) {
        set(result, i, initDims(arrayClasses, castableTypeMapExprs,
            elementTypeIds, leafElementTypeClass, dimExprs, index, count, leafInitValueType));
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
  protected int elementTypeId = 0;
  protected int elementTypeClass = 0;
}
