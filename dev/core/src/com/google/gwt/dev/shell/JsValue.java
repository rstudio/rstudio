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
package com.google.gwt.dev.shell;

import java.util.Vector;

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
   * Allows JsValue subclasses to clean themselves up.
   */
  protected interface JsCleanup {
    void doCleanup();
  }

  /**
   * For a thread-safety check to make sure only one thread ever accesses it.
   */
  private static Thread theOnlyThreadAllowed;

  /**
   * A queue of JsCleanup objects ready to be released by the main thread.
   */
  private static Vector<JsCleanup> toBeReleased = new Vector<JsCleanup>();

  private static final Object toBeReleasedLock = new Object();

  /**
   * The main thread should call this from time to time to release hosted-mode
   * objects that Java is no longer referencing.
   */
  public static void mainThreadCleanup() {
    checkThread();
    Vector<JsCleanup> temp;
    synchronized (toBeReleasedLock) {
      temp = toBeReleased;
      toBeReleased = new Vector<JsCleanup>();
    }
    for (JsCleanup cleanup : temp) {
      cleanup.doCleanup();
    }
    temp.clear();
  }

  /**
   * Ensures that the current thread is actually the UI thread.
   */
  private static synchronized void checkThread() {
    if (theOnlyThreadAllowed == null) {
      theOnlyThreadAllowed = Thread.currentThread();
    } else if (theOnlyThreadAllowed != Thread.currentThread()) {
      throw new RuntimeException("This object has permanent thread affinity.");
    }
  }

  /**
   * Moves this JS value to the queue of objects that are ready to be released.
   */
  private static void queueCleanup(JsCleanup cleanup) {
    // Add to the queue to be released by the main thread later.
    //
    synchronized (toBeReleasedLock) {
      toBeReleased.add(cleanup);
    }
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
   * Get the value of the object as a double. May attempt to convert the value
   * to a double if it is not a double.
   * 
   * @return the value of the underlying object as a double
   */
  public abstract double getNumber();

  /**
   * Get the value of the object as a string. Tries very hard to return a
   * reasonable value for any underyling type, but this should only be used for
   * human consumption as the exact format is not reliable across platforms.
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
   * @throws HostedModeException
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
   * Wrap a Java method as a JavaScript function pointer.
   * 
   * @param string the name of the method
   * @param dispMethod the DispatchMethod object describing the method tow wrap
   */
  // TODO(jat): platform-independent version of this?
  // The problem is that each platform has different conventions for passing
  // JavaScript values back into a Java method.
  // public abstract void setWrappedFunction(String string,
  // DispatchMethod dispMethod);
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
      return "Java object: " + getWrappedJavaObject().toString();
    } else if (isJavaScriptObject()) {
      return "JS object [" + getTypeString() + "] : " + getString();
    } else if (isString()) {
      return "string: '" + getString() + "'";
    } else {
      return "*unknown type: " + getTypeString() + "*";
    }
  }

  /**
   * Create an object which frees the underlying JS resource.
   * 
   * @return a JsCleanup object which will free the underlying JS resource
   */
  protected abstract JsCleanup createCleanupObject();

  /**
   * When the Java object is garbage-collected, make sure the associated JS
   * resource is freed. A helper object is used to avoid issues with
   * resurrecting this object.
   */
  @Override
  protected final void finalize() throws Throwable {
    queueCleanup(createCleanupObject());
  }
}
