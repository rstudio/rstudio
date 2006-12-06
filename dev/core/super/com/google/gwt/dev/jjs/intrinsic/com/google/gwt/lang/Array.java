// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.lang;

/**
 * This is a magic class the compiler uses as a base class for injected array
 * classes.
 */
public final class Array {

  public final int length;
  protected final int queryId;

  public Array(int length, int typeId, int queryId, String typeName) {
    this.length = length;
    this.typeName = typeName;
    this.typeId = typeId;
    this.queryId = queryId;
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in javascript objects
   * as follows: [a, b, c]
   */
  public static Array initDims(String typeName, Object typeIdExprs,
      Object queryIdExprs, Object dimExprs, Object defaultValue) {
    // ASSERT: dimExprs.length > 0 or else code gen is broken
    //
    return initDims(typeName, typeIdExprs, queryIdExprs, dimExprs, 0,
      getValueCount(dimExprs), defaultValue);
  }

  /**
   * Creates an array like "new T[a][b][c][][]" by passing in javascript objects
   * as follows: [a,b,c]
   */
  private static Array initDims(String typeName, Object typeIdExprs,
      Object queryIdExprs, Object dimExprs, int index, int count,
      Object defaultValue) {
    int length;
    if ((length = getIntValue(dimExprs, index)) < 0)
      throw new NegativeArraySizeException();

    Array result = new Array(length, getIntValue(typeIdExprs, index),
      getIntValue(queryIdExprs, index), typeName);

    ++index;
    if (index < count) {
      typeName = typeName.substring(1);
      for (int i = 0; i < length; ++i)
        _set(result, i, initDims(typeName, typeIdExprs, queryIdExprs, dimExprs,
          index, count, defaultValue));
    } else {
      for (int i = 0; i < length; ++i)
        _set(result, i, defaultValue);
    }

    return result;
  }

  /**
   * Creates an array like "new T[][]{a,b,c,d}" by passing in javascript objects
   * as follows: [a,b,c,d]
   */
  public static final Array initValues(String typeName, int typeId,
      int queryId, Object values) {
    int length = getValueCount(values);
    Array result = new Array(length, typeId, queryId, typeName);
    for (int i = 0; i < length; ++i)
      _set(result, i, getValue(values, i));
    return result;
  }

  /**
   * Performs an array assignment, checking for valid index and type
   */
  public static Object setCheck(Array array, int index, Object value) {
    if (value != null && array.queryId != 0
      && !Cast.instanceOf(value, array.queryId))
      throw new ArrayStoreException();
    return _set(array, index, value);
  }


  /**
   * Sets a value in the array
   */
  private static native Object _set(Array array, int index, Object value) /*-{
    return array[index] = value;
  }-*/;

  /**
   * Gets the length of a JSON array
   */
  private static native int getValueCount(Object values) /*-{
    return values.length; 
  }-*/;

  /**
   * Gets a value from a JSON array
   */
  private native static Object getValue(Object values, int index) /*-{
    return values[index];
  }-*/;

  /**
   * Gets an the first value from a JSON int array
   */
  private native static int getIntValue(Object values, int index) /*-{
    return values[index];
  }-*/;

}
