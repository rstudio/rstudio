/*
 * Copyright 2007 Google Inc.
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
   * Call this when a JavaScript exception is caught.
   */
  abstract void exceptionCaught(int number, String name, String description);

  /**
   * Invoke a native JavaScript function that returns a boolean value.
   */
  abstract boolean invokeNativeBoolean(String name, Object jthis,
      Class[] types, Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a byte value.
   */
  abstract byte invokeNativeByte(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a character value.
   */
  abstract char invokeNativeChar(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a double value.
   */
  abstract double invokeNativeDouble(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a float value.
   */
  abstract float invokeNativeFloat(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a handle value.
   */
  abstract Object invokeNativeHandle(String name, Object jthis,
      Class returnType, Class[] types, Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns an integer value.
   */
  abstract int invokeNativeInt(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a long value.
   */
  abstract long invokeNativeLong(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns an object value.
   */
  abstract Object invokeNativeObject(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a short value.
   */
  abstract short invokeNativeShort(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a string value.
   */
  abstract String invokeNativeString(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns no value.
   */
  abstract void invokeNativeVoid(String name, Object jthis, Class[] types,
      Object[] args) throws Throwable;

  /**
   * Logs to the dev shell logger.
   */
  abstract void log(String message, Throwable e);

  /**
   * Resolves a deferred binding request and create the requested object.
   */
  abstract Object rebindAndCreate(String requestedTypeName)
      throws UnableToCompleteException;
}
