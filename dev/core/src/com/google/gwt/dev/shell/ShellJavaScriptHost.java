// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * This interface contains all of the methods that must be exposed to a hosted
 * mode application via its JavaScriptHost class. JavaScriptHost contains a
 * method called setHost() that must be called when a new application is
 * initialized.
 * 
 * This interface works with JavaScriptHost to keep running applications at 
 * arms-length via an isolated class loader (this requires that there be no 
 * explicit dependencies between the shell and any client-side classes).
 */
public interface ShellJavaScriptHost {

  /**
   * Defines a new native JavaScript function.
   * 
   * @param name the function's name, usually a JSNI signature
   * @param paramNames parameter names
   * @param js the script body
   */
  abstract void createNative(String file, int line, String name,
      String[] paramNames, String js);

  /**
   * Releases a handle.
   * 
   * @param opaque the handle to be released
   */
  abstract void ditchHandle(int opaque);

  /**
   * Invoke a native JavaScript function that returns a boolean value.
   */
  abstract boolean invokeNativeBoolean(String name, Object jthis,
      Class[] types, Object[] args);

  /**
   * Invoke a native JavaScript function that returns a byte value.
   */
  abstract byte invokeNativeByte(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a character value.
   */
  abstract char invokeNativeChar(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a double value.
   */
  abstract double invokeNativeDouble(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a float value.
   */
  abstract float invokeNativeFloat(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a handle value.
   */
  abstract Object invokeNativeHandle(String name, Object jthis,
      Class returnType, Class[] types, Object[] args);

  /**
   * Invoke a native JavaScript function that returns an integer value.
   */
  abstract int invokeNativeInt(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a long value.
   */
  abstract long invokeNativeLong(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns an object value.
   */
  abstract Object invokeNativeObject(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a short value.
   */
  abstract short invokeNativeShort(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns a string value.
   */
  abstract String invokeNativeString(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Invoke a native JavaScript function that returns no value.
   */
  abstract void invokeNativeVoid(String name, Object jthis, Class[] types,
      Object[] args);

  /**
   * Resolves a deferred binding request and create the requested object.
   */
  abstract Object rebindAndCreate(String requestedTypeName)
      throws UnableToCompleteException;

  /**
   * Call this when a JavaScript exception is caught.
   */
  abstract void exceptionCaught(int number, String name, String description);

  /**
   * Logs to the dev shell logger.
   */
  abstract void log(String message, Throwable e);
}
