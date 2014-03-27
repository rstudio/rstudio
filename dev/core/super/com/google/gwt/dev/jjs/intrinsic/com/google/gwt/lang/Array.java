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
 * This is an intrinsic class that contains the implementation details for Java arrays. <p>
 *
 * This class should contain only static methods or fields.
 */
public final class Array {
  // Array element type classes
  private static final int TYPE_JAVA_OBJECT = 0;
  private static final int TYPE_JAVA_OBJECT_OR_JSO = 1;
  private static final int TYPE_JSO = 2;
  private static final int TYPE_JAVA_LANG_OBJECT = 3;
  private static final int TYPE_PRIMITIVE_LONG = 4;
  private static final int TYPE_PRIMITIVE_NUMBER = 5;
  private static final int TYPE_PRIMITIVE_BOOLEAN = 6;

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
    initValues(a.getClass(), Util.getCastableTypeMap(a), Array.getElementTypeId(a),
        Array.getElementTypeClass(a), result);
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
    // TODO(rluble): The behaviour here seems erroneous as the array elements will not be
    // initialized but left undefined. However the usages seem to be safe and changing here
    // might have performace penalty. Maybe rename to createUninitializedFrom(), to make
    // the meaning clearer.
    Array result = initializeArrayElementsWithDefaults(TYPE_JAVA_OBJECT, length);
    initValues(a.getClass(), Util.getCastableTypeMap(a), Array.getElementTypeId(a),
        Array.getElementTypeClass(a), result);
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
   * @param elementTypeClass whether the element type is java.lang.Object
   *        ({@link TYPE_JAVA_LANG_OBJECT}), is guaranteed to be a java object
   *        ({@link TYPE_JAVA_OBJECT}), is guaranteed to be a JSO
   *        ({@link TYPE_JSO}), can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}) or
   *        or some primitive type {@link TYPE_PRIMITIVE_BOOLEAN}, {@link TYPE_PRIMITIVE_LONG} or
   *        {@link TYPE_PRIMITIVE_NUMBER}.
   * @param length the length of the array
   * @return the new array
   */
  public static Array initDim(Class<?> arrayClass, JavaScriptObject castableTypeMap,
      JavaScriptObject elementTypeId, int length, int elementTypeClass) {
    Array result = initializeArrayElementsWithDefaults(elementTypeClass, length);
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
   * @param leafElementTypeClass whether the element type is java.lang.Object
   *        ({@link TYPE_JAVA_LANG_OBJECT}), is guaranteed to be a java object
   *        ({@link TYPE_JAVA_OBJECT}), is guaranteed to be a JSO
   *        ({@link TYPE_JSO}), can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}) or
   *        or some primitive type {@link TYPE_PRIMITIVE_BOOLEAN}, {@link TYPE_PRIMITIVE_LONG} or
   *        {@link TYPE_PRIMITIVE_NUMBER}.
   * @param dimExprs the length of each dimension, from highest to lower
   * @return the new array
   */
  public static Array initDims(Class<?> arrayClasses[], JavaScriptObject[] castableTypeMapExprs,
      JavaScriptObject[] elementTypeIds, int leafElementTypeClass, int[] dimExprs, int count) {
    return initDims(arrayClasses, castableTypeMapExprs, elementTypeIds, leafElementTypeClass,
        dimExprs, 0, count);
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
   *        ({@link TYPE_JSO}), can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}) or
   *        or some primitive type {@link TYPE_PRIMITIVE_BOOLEAN}, {@link TYPE_PRIMITIVE_LONG} or
   *        {@link TYPE_PRIMITIVE_NUMBER}.
   * @param array the JSON array that will be transformed into a GWT array
   * @return values; having wrapped it for GWT
   */
  public static Array initValues(Class<?> arrayClass, JavaScriptObject castableTypeMap,
      JavaScriptObject elementTypeId, int elementTypeClass, Array array) {
    setClass(array, arrayClass);
    Util.setCastableTypeMap(array, castableTypeMap);
    Util.setTypeMarker(array);
    Array.setElementTypeId(array, elementTypeId);
    Array.setElementTypeClass(array, elementTypeClass);
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
      int elementTypeClass = Array.getElementTypeClass(array);
      JavaScriptObject elementTypeId = Array.getElementTypeId(array);
      if (elementTypeClass == TYPE_JAVA_OBJECT && !Cast.canCast(value, elementTypeId)) {
        // value must be castable to elementType.
        throw new ArrayStoreException();
      } else if (elementTypeClass == TYPE_JSO && !Cast.isJavaScriptObject(value)) {
        // value must be a JavaScriptObject
        throw new ArrayStoreException();
      } else if (elementTypeClass == TYPE_JAVA_OBJECT_OR_JSO
          && !Cast.isJavaScriptObject(value)
          && !Cast.canCast(value, elementTypeId)) {
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
   * Creates a primitive JSON array of a given the element type class.
   */
  private static native Array initializeArrayElementsWithDefaults(
      int elementTypeClass, int length) /*-{
    var array = new Array(length);
    var initValue;
    switch (elementTypeClass) {
      case @com.google.gwt.lang.Array::TYPE_JAVA_OBJECT:
      case @com.google.gwt.lang.Array::TYPE_JAVA_OBJECT_OR_JSO:
      case @com.google.gwt.lang.Array::TYPE_JAVA_LANG_OBJECT:
      case @com.google.gwt.lang.Array::TYPE_JSO:
        // Do not initialize as undefined is equivalent to null
        return array;
      case @com.google.gwt.lang.Array::TYPE_PRIMITIVE_LONG:
        // Fill array with the type used by LongLib
        // TODO(rluble): This should refer to the zero long value defined in LongLib
        initValue = {l: 0, m: 0, h:0};
        break;
      case @com.google.gwt.lang.Array::TYPE_PRIMITIVE_NUMBER:
          initValue = 0;
        break;
      case @com.google.gwt.lang.Array::TYPE_PRIMITIVE_BOOLEAN:
        initValue = false;
        break;
    }

    for ( var i = 0; i < length; ++i) {
      array[i] = initValue;
    }
    return array;
  }-*/;

  private static Array initDims(Class<?> arrayClasses[], JavaScriptObject[] castableTypeMapExprs,
      JavaScriptObject[] elementTypeIds, int leafElementTypeClass, int[] dimExprs,
      int index, int count) {
    int length = dimExprs[index];
    boolean isLastDim = (index == (count - 1));
    // All dimensions but the last are plain reference types.
    int elementTypeClass = isLastDim ? leafElementTypeClass : TYPE_JAVA_OBJECT;

    Array result = initializeArrayElementsWithDefaults(elementTypeClass, length);
    initValues(arrayClasses[index], castableTypeMapExprs[index],
        elementTypeIds[index], elementTypeClass, result);

    if (!isLastDim) {
      // Recurse to next dimension.
      ++index;
      for (int i = 0; i < length; ++i) {
        set(result, i, initDims(arrayClasses, castableTypeMapExprs,
            elementTypeIds, leafElementTypeClass, dimExprs, index, count));
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

  private static native void setElementTypeId(Object array, JavaScriptObject elementTypeId) /*-{
    array.__elementTypeId$ = elementTypeId;
  }-*/;

  private static native JavaScriptObject getElementTypeId(Object array) /*-{
    return array.__elementTypeId$;
  }-*/;

  private static native void setElementTypeClass(Object array, int elementTypeClass) /*-{
    array.__elementTypeClass$ = elementTypeClass;
  }-*/;

  private static native int getElementTypeClass(Object array) /*-{
    return array.__elementTypeClass$;
  }-*/;
}

