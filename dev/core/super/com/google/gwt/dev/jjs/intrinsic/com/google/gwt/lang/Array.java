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

import static javaemul.internal.InternalPreconditions.checkArrayType;
import static javaemul.internal.InternalPreconditions.checkNotNull;

import com.google.gwt.core.client.JavaScriptObject;

import javaemul.internal.annotations.DoNotInline;
import javaemul.internal.annotations.HasNoSideEffects;

/**
 * This is an intrinsic class that contains the implementation details for Java arrays. <p>
 *
 * This class should contain only static methods or fields.
 */
public final class Array {
  // Array element type classes. Needs to be in sync with enums in TypeCategory.java.
  private static final int TYPE_JAVA_OBJECT = 0;
  private static final int TYPE_JAVA_OBJECT_OR_JSO = 1;
  private static final int TYPE_JSO = 2;
  private static final int TYPE_NATIVE_ARRAY = 3;
  private static final int TYPE_ARRAY = 4;
  private static final int TYPE_JSO_ARRAY = 5;
  private static final int TYPE_JAVA_LANG_OBJECT = 6;
  private static final int TYPE_JAVA_LANG_STRING = 7;
  private static final int TYPE_JAVA_LANG_DOUBLE = 8;
  private static final int TYPE_JAVA_LANG_BOOLEAN = 9;
  private static final int TYPE_JS_NATIVE = 10;
  private static final int TYPE_JS_UNKNOWN_NATIVE = 11;
  private static final int TYPE_JS_FUNCTION = 12;
  private static final int TYPE_PRIMITIVE_LONG = 13;
  private static final int TYPE_PRIMITIVE_NUMBER = 14;
  private static final int TYPE_PRIMITIVE_BOOLEAN = 15;

  public static <T> T[] stampJavaTypeInfo(Object array, T[] referenceType) {
    if (Array.getElementTypeCategory(referenceType) != TYPE_JS_UNKNOWN_NATIVE) {
      stampJavaTypeInfo(referenceType.getClass(), Util.getCastableTypeMap(referenceType),
          Array.getElementTypeId(referenceType),
          Array.getElementTypeCategory(referenceType), array);
    }
    return Array.asArray(array);
  }

  /**
   * Returns an untyped uninitialized array.
   */
  static native Object[] newArray(int size) /*-{
    return new Array(size);
  }-*/;

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   *
   * @param leafClassLiteral the class literal for the leaf class
   * @param castableTypeMap the map of types to which this array can be casted,
   *          in the form of a JSON map object
   * @param elementTypeId the typeId of array elements
   * @param elementTypeCategory whether the element type is java.lang.Object
   *        ({@link TYPE_JAVA_LANG_OBJECT}), is guaranteed to be a java object
   *        ({@link TYPE_JAVA_OBJECT}), is guaranteed to be a JSO
   *        ({@link TYPE_JSO}), can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}) or
   *        or some primitive type {@link TYPE_PRIMITIVE_BOOLEAN}, {@link TYPE_PRIMITIVE_LONG} or
   *        {@link TYPE_PRIMITIVE_NUMBER}.
   * @param length the length of the array
   * @param dimensions the number of dimensions of the array
   * @return the new array
   */
  public static Object initUnidimensionalArray(Class<?> leafClassLiteral,
      JavaScriptObject castableTypeMap, JavaScriptObject elementTypeId, int length,
      int elementTypeCategory, int dimensions) {
    Object result = initializeArrayElementsWithDefaults(elementTypeCategory, length);
    if (elementTypeCategory != TYPE_JS_UNKNOWN_NATIVE) {
      stampJavaTypeInfo(getClassLiteralForArray(leafClassLiteral, dimensions), castableTypeMap,
          elementTypeId, elementTypeCategory, result);
    }
    return result;
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in a native JSON
   * array, [a, b, c].
   *
   * @param leafClassLiteral the class literal for the leaf class
   * @param castableTypeMapExprs the JSON castableTypeMap of each dimension,
   *          from highest to lowest
   * @param elementTypeIds the elementTypeId of each dimension, from highest to lowest
   * @param leafElementTypeCategory whether the element type is java.lang.Object
   *        ({@link TYPE_JAVA_LANG_OBJECT}), is guaranteed to be a java object
   *        ({@link TYPE_JAVA_OBJECT}), is guaranteed to be a JSO
   *        ({@link TYPE_JSO}), can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}) or
   *        or some primitive type {@link TYPE_PRIMITIVE_BOOLEAN}, {@link TYPE_PRIMITIVE_LONG} or
   *        {@link TYPE_PRIMITIVE_NUMBER}.
   * @param dimExprs the length of each dimension, from highest to lower
   * @return the new array
   */
  public static Object initMultidimensionalArray(Class<?> leafClassLiteral,
      JavaScriptObject[] castableTypeMapExprs,
      JavaScriptObject[] elementTypeIds, int leafElementTypeCategory, int[] dimExprs, int count) {
    return initMultidimensionalArray(leafClassLiteral, castableTypeMapExprs, elementTypeIds,
        leafElementTypeCategory,
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
   * @param elementTypeCategory whether the element type is java.lang.Object
   *        ({@link TYPE_JAVA_LANG_OBJECT}), is guaranteed to be a java object
   *        ({@link TYPE_JAVA_OBJECT}), is guaranteed to be a JSO
   *        ({@link TYPE_JSO}), can be either ({@link TYPE_JAVA_OBJECT_OR_JSO}) or
   *        or some primitive type {@link TYPE_PRIMITIVE_BOOLEAN}, {@link TYPE_PRIMITIVE_LONG} or
   *        {@link TYPE_PRIMITIVE_NUMBER}.
   * @param array the JSON array that will be transformed into a GWT array
   * @return values; having wrapped it for GWT
   */
  public static Object stampJavaTypeInfo(Class<?> arrayClass, JavaScriptObject castableTypeMap,
      JavaScriptObject elementTypeId, int elementTypeCategory, Object array) {
    setClass(array, arrayClass);
    Util.setCastableTypeMap(array, castableTypeMap);
    Util.setTypeMarker(array);
    Array.setElementTypeId(array, elementTypeId);
    Array.setElementTypeCategory(array, elementTypeCategory);
    return array;
  }

  /**
   * Performs an array assignment, after validating the type of the value being
   * stored. The form of the type check depends on the value of elementTypeId and
   * elementTypeCategory as follows:
   * <p>
   * If the elementTypeCategory is {@link TYPE_JAVA_OBJECT}, this indicates a normal cast check
   * should be performed, using the elementTypeId as the cast destination type.
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
  public static Object setCheck(Object array, int index, Object value) {
    checkArrayType(value == null || canSet(array, value));
    return set(array, index, value);
  }

  @HasNoSideEffects
  private static boolean canSet(Object array, Object value) {
    switch (Array.getElementTypeCategory(array)) {
      case TYPE_JAVA_LANG_STRING:
        return Cast.instanceOfString(value);
      case TYPE_JAVA_LANG_DOUBLE:
        return Cast.instanceOfDouble(value);
      case TYPE_JAVA_LANG_BOOLEAN:
        return Cast.instanceOfBoolean(value);
      case TYPE_ARRAY:
        return Cast.instanceOfArray(value);
      case TYPE_JS_FUNCTION:
        return Cast.instanceOfFunction(value);
      case TYPE_JAVA_OBJECT:
        return Cast.canCast(value, Array.getElementTypeId(array));
      case TYPE_JSO:
        return Cast.isJavaScriptObject(value);
      case TYPE_JAVA_OBJECT_OR_JSO:
        return Cast.isJavaScriptObject(value)
            || Cast.canCast(value, Array.getElementTypeId(array));
      default:
        return true;
    }
  }

  /**
   * Use JSNI to effect a castless type change.
   */
  private static native <T> T[] asArray(Object array) /*-{
    return array;
  }-*/;

  /**
   * Creates a primitive JSON array of a given the element type class.
   */
  private static native Object initializeArrayElementsWithDefaults(
      int elementTypeCategory, int length) /*-{
    var array = new Array(length);
    var initValue;
    switch (elementTypeCategory) {
      case @com.google.gwt.lang.Array::TYPE_PRIMITIVE_LONG:
        // Fill array with fast long version of 0, which is just the number 0; so fall through.
      case @com.google.gwt.lang.Array::TYPE_PRIMITIVE_NUMBER:
          initValue = 0;
        break;
      case @com.google.gwt.lang.Array::TYPE_PRIMITIVE_BOOLEAN:
        initValue = false;
        break;
      default:
        // Do not initialize as undefined is equivalent to null
        return array;
    }

    for ( var i = 0; i < length; ++i) {
      array[i] = initValue;
    }
    return array;
  }-*/;

  private static Object initMultidimensionalArray(Class<?> leafClassLiteral,
      JavaScriptObject[] castableTypeMapExprs, JavaScriptObject[] elementTypeIds,
      int leafElementTypeCategory, int[] dimExprs, int index, int count) {
    int length = dimExprs[index];
    boolean isLastDimension = (index == (count - 1));
    // All dimensions but the last are plain reference types.
    int elementTypeCategory = isLastDimension ? leafElementTypeCategory : TYPE_JAVA_OBJECT;

    Object result = initializeArrayElementsWithDefaults(elementTypeCategory, length);
    if (leafElementTypeCategory != TYPE_JS_UNKNOWN_NATIVE) {
      stampJavaTypeInfo(getClassLiteralForArray(leafClassLiteral, count - index),
          castableTypeMapExprs[index], elementTypeIds[index], elementTypeCategory, result);
    }

    if (!isLastDimension) {
      // Recurse to next dimension.
      ++index;
      for (int i = 0; i < length; ++i) {
        set(result, i, initMultidimensionalArray(leafClassLiteral, castableTypeMapExprs,
            elementTypeIds, leafElementTypeCategory, dimExprs, index, count));
      }
    }
    return result;
  }

  // This method is package protected so that it is indexed. {@link ImplementClassLiteralsAsFields}
  // will insert calls to this method when array class literals are constructed.
  //
  // Inlining is prevented on this very hot method to avoid a subtantial increase in
  // {@link JsInliner} execution time.
  @DoNotInline
  static <T> Class<T> getClassLiteralForArray(Class<?> clazz , int dimensions) {
    return getClassLiteralForArrayImpl(clazz, dimensions);
  }

  private static native int getElementTypeCategory(Object array) /*-{
    return array.__elementTypeCategory$ == null
        ? @Array::TYPE_JS_UNKNOWN_NATIVE
        : array.__elementTypeCategory$;
  }-*/;

  @HasNoSideEffects
  private static native JavaScriptObject getElementTypeId(Object array) /*-{
    return array.__elementTypeId$;
  }-*/;

  // DO NOT INLINE this method into {@link getClassLiteralForArray}.
  // The purpose of this method is to avoid introducing a public api to {@link java.lang.Class}.
  private static native <T>  Class<T> getClassLiteralForArrayImpl(
      Class<?> clazz , int dimensions) /*-{
    return @java.lang.Class::getClassLiteralForArray(*)(clazz, dimensions);
  }-*/;

  /**
   * Sets a value in the array.
   */
  private static native Object set(Object array, int index, Object value) /*-{
    return array[index] = value;
  }-*/;

  // violator pattern so that the field remains private
  private static native void setClass(Object o, Class<?> clazz) /*-{
    o.@java.lang.Object::___clazz = clazz;
  }-*/;

  private static native void setElementTypeId(Object array, JavaScriptObject elementTypeId) /*-{
    array.__elementTypeId$ = elementTypeId;
  }-*/;

  private static native void setElementTypeCategory(Object array, int elementTypeCategory) /*-{
    array.__elementTypeCategory$ = elementTypeCategory;
  }-*/;

  /**
   * Returns true if {@code src} is a Java array.
   */
  @HasNoSideEffects
  static boolean isJavaArray(Object src) {
    return Cast.isArray(src) && Util.hasTypeMarker(src);
  }

  /**
   * Returns true if {@code src} is a Java array.
   */
  static boolean isPrimitiveArray(Object array) {
    int elementTypeCategory = getElementTypeCategory(array);
    return elementTypeCategory >= TYPE_PRIMITIVE_LONG
        && elementTypeCategory <= TYPE_PRIMITIVE_BOOLEAN;
  };

  public static Object ensureNotNull(Object array) {
    return checkNotNull(array);
  }

  private Array() {
  }
}

