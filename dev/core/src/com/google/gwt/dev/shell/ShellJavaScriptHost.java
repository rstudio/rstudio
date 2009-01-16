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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.JsniMethod;

import java.util.List;

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
   * Define one or more JSNI methods.
   * 
   * @param logger
   * @param compiledClass
   * @param jsniMethods
   * @param dispatchIdOracle
   */
  void createNativeMethods(TreeLogger logger, List<JsniMethod> jsniMethods,
      DispatchIdOracle dispatchIdOracle);

  /**
   * Call this when a JavaScript exception is caught.
   */
  void exceptionCaught(Object exception);

  /**
   * Invoke a native JavaScript function that returns a boolean value.
   */
  boolean invokeNativeBoolean(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a byte value.
   */
  byte invokeNativeByte(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a character value.
   */
  char invokeNativeChar(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a double value.
   */
  double invokeNativeDouble(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a float value.
   */
  float invokeNativeFloat(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns an integer value.
   */
  int invokeNativeInt(String name, Object jthis, Class<?>[] types, Object[] args)
      throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a long value.
   */
  long invokeNativeLong(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns an object value.
   */
  Object invokeNativeObject(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns a short value.
   */
  short invokeNativeShort(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Invoke a native JavaScript function that returns no value.
   */
  void invokeNativeVoid(String name, Object jthis, Class<?>[] types,
      Object[] args) throws Throwable;

  /**
   * Logs to the dev shell logger.
   */
  void log(String message, Throwable e);

  /**
   * Resolves a deferred binding request and create the requested object.
   */
  <T> T rebindAndCreate(String requestedTypeName)
      throws UnableToCompleteException;
}
