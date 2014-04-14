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
package com.google.gwt.dev.shell;

/**
 * Represents a JavaScript value.
 *
 * Note that in general the various get*() methods will return
 * platform-independent values only if the corresponding is*() method returns
 * true. In some cases, an IllegalStateException may be thrown if the JavaScript
 * value is not of the appropriate type or bogus values may be returned. Note
 * that getString will try very hard to return a reasonable result for any
 * value, but it is intended only for human consumption and the exact format for
 * anything besides a string value cannot be relied upon.
 */
public abstract class JsValue {

  /**
   * Provides interface for methods to be exposed on JavaScript side.
   */
  public interface DispatchMethod {
    boolean invoke(JsValue jsthis, JsValue[] jsargs, JsValue returnValue);
  }

  /**
   * Provides interface for objects to be exposed on JavaScript side.
   */
  public interface DispatchObject {
    JsValue getField(int dispatchId);

    JsValue getField(String name);

    int getFieldId(String name);

    Object getTarget();

    void setField(int dispatchId, JsValue value);

    void setField(String name, JsValue value);
  }

  /**
   * Get the value of the object as a boolean. May attempt to convert the value
   * to a boolean if it is not a boolean.
   *
   * @return the value of the underlying object as a boolean
   */
  public abstract boolean getBoolean();

  /**
   * Get the value of the object as an integer. May attempt to convert the value
   * to an integer if it is not an integer.
   *
   * @return the value of the underlying object as an int
   */
  public abstract int getInt();

  /**
   * Get the wrapper object for a wrapped Java object. Only valid if
   * isWrappedJavaObject() is true.
   *
   * @return the Java object wrapper
   */
  public abstract DispatchObject getJavaObjectWrapper();

  /**
   * Returns a unique value corresponding to the underlying JavaScript object.
   * In general, two different JsValues will return the same value IFF the
   * underlying JavaScript objects are identical (===).
   *
   * @return a unique number corresponding to the underlying object, or
   *         <code>0</code> if {@link #isJavaScriptObject()} is
   *         <code>false</code>
   */
  public abstract int getJavaScriptObjectPointer();

  /**
   * Get the value of the object as a double. May attempt to convert the value
   * to a double if it is not a double.
   *
   * @return the value of the underlying object as a double
   */
  public abstract double getNumber();

  /**
   * Get the value of the object as a string. Will coerce the underlying type to
   * a string, but stable cross-platform behavior is only guaranteed when
   * {@link #isString()} is <code>true</code>.
   *
   * @return the value of the underlying object as a string
   */
  public abstract String getString();

  /**
   * Returns a human-readable string describing the type of the JS object. This
   * is intended only for human consumption and may vary across platforms.
   */
  public abstract String getTypeString();

  /**
   * Returns a wrapped Java method.
   */
  public abstract DispatchMethod getWrappedJavaFunction();

  /**
   * Unwrap a wrapped Java object.
   *
   * @return the original Java object wrapped in this JS object
   */
  public abstract Object getWrappedJavaObject();

  /**
   * Returns true if the JS value is a boolean.
   */
  public abstract boolean isBoolean();

  /**
   * Returns true if getInt() can be used on this value.
   */
  public abstract boolean isInt();

  /**
   * Returns true if the JS value is a native JS object.
   */
  public abstract boolean isJavaScriptObject();

  /**
   * Returns true if the JS value is null.
   */
  public abstract boolean isNull();

  /**
   * Returns true if the JS value is a numeric type.
   */
  public abstract boolean isNumber();

  /**
   * Returns true if the JS value is a string.
   */
  public abstract boolean isString();

  /**
   * Returns true if the JS value is undefined (void).
   */
  public abstract boolean isUndefined();

  /**
   * Returns true if the JS value contains a wrapped Java method.
   */
  public abstract boolean isWrappedJavaFunction();

  /**
   * Returns true if the JS value is a wrapped Java object.
   */
  public abstract boolean isWrappedJavaObject();

  /**
   * Sets the JS object to be a boolean value.
   *
   * @param val the boolean value to set
   */
  public abstract void setBoolean(boolean val);

  /**
   * Sets the JS object to be a number, passed as an byte.
   *
   * @param val the value to store
   */
  public abstract void setByte(byte val);

  /**
   * Sets the JS object to be a number, passed as a char.
   *
   * @param val the value to store
   */
  public abstract void setChar(char val);

  /**
   * Sets the JS object to be a number, passed as a double.
   *
   * @param val the value to store
   */
  public abstract void setDouble(double val);

  /**
   * Sets the JS object to be a number, passed as an int.
   *
   * @param val the value to store
   */
  public abstract void setInt(int val);

  /**
   * Set the JS object to be null.
   *
   * @throws com.google.gwt.dev.shell.HostedModeException
   */
  public abstract void setNull();

  /**
   * Sets the JS object to be a number, passed as a short.
   *
   * @param val the value to store
   */
  public abstract void setShort(short val);

  /**
   * Set the JS object to the supplied string.
   *
   * @param val the string to put in the JS object
   * @throws HostedModeException on JS allocation failures
   */
  public abstract void setString(String val);

  /**
   * Set the JS object to be undefined (void).
   *
   * @throws HostedModeException on JS allocation failures
   */
  public abstract void setUndefined();

  /**
   * Make this JsValue refer to the same underlying object as another JsValue.
   *
   * @param other JsValue to copy JS object from
   */
  public abstract void setValue(JsValue other);

  /**
   * Set the JS object to the supplied object, which will be wrapped in a
   * platform-dependent JavaScript class.
   *
   * @param <T> the type of the Java object to wrap
   * @param cl the classloader to create the wrapper object with
   * @param val the Java object to wrap
   * @throws HostedModeException
   */
  public abstract <T> void setWrappedJavaObject(CompilingClassLoader cl, T val);

  /**
   * Produce a string representation of the JsValue.
   */
  @Override
  public String toString() {
    if (isUndefined()) {
      return "void";
    } else if (isNull()) {
      return "null";
    } else if (isBoolean()) {
      return "bool: " + (getBoolean() ? "true" : "false");
    } else if (isInt()) {
      return "int: " + Integer.toString(getInt());
    } else if (isNumber()) {
      return "double: " + Double.toString(getNumber());
    } else if (isWrappedJavaObject()) {
      Object wrappedObject = getWrappedJavaObject();
      if (wrappedObject == null) {
        return "Java static dispatch";
      }
      // avoid calling toString on the wrapped object, as this can be expensive
      return "Java object: " + wrappedObject.getClass().getName() + '@'
          + System.identityHashCode(wrappedObject);
    } else if (isJavaScriptObject()) {
      return getTypeString();
    } else if (isString()) {
      return "string: '" + getString() + "'";
    } else if (isWrappedJavaFunction()) {
      return "Java method: " + getWrappedJavaFunction().toString();
    }
    return getTypeString();
  }
}
