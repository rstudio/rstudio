/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.shared;

import com.google.gwt.core.shared.impl.JsLogger;

/**
 * Supports core functionality that in some cases requires direct support from
 * the compiler and runtime systems such as runtime type information and
 * deferred binding.
 */
public final class GWT {

  /**
   * Always <code>null</code> in Production Mode; in Development Mode provides
   * the implementation for certain methods.
   */
  private static GWTBridge sGWTBridge = null;

  /**
   * The implementation of GWT.log() to use in JavaScript.
   */
  private static final JsLogger logger;

  static {
    if (isScript()) {
      logger = create(JsLogger.class);
    } else {
      logger = null;
    }
  }

  /**
   * Instantiates a class via deferred binding.
   *
   * <p>
   * The argument to {@link #create(Class)}&#160;<i>must</i> be a class literal
   * because the Production Mode compiler must be able to statically determine
   * the requested type at compile-time. This can be tricky because using a
   * {@link Class} variable may appear to work correctly in Development Mode.
   * </p>
   *
   * @param classLiteral a class literal specifying the base class to be
   *          instantiated
   * @return the new instance, which must be cast to the requested class
   */
  public static <T> T create(Class<?> classLiteral) {
    /*
     * In Production Mode, the compiler directly replaces calls to this method
     * with a new Object() type expression of the correct rebound type.
     */
    return createImpl(classLiteral);
  }

  /**
   * Instantiates a class via deferred binding.
   *
   * @param classLiteral a class literal specifying the base class to be
   *          instantiated
   * @return the new instance, which must be cast to the requested class
   */
  public static <T> T createImpl(Class<?> classLiteral) {
    if (sGWTBridge == null) {
      throw new UnsupportedOperationException(
          "ERROR: GWT.create() is only usable in client code!  It cannot be called, "
              + "for example, from server code.  If you are running a unit test, "
              + "check that your test case extends GWTTestCase and that GWT.create() "
              + "is not called from within an initializer or constructor.");
    } else {
      return sGWTBridge.<T> create(classLiteral);
    }
  }

  /**
   * Returns the empty string when running in Production Mode, but returns a
   * unique string for each thread in Development Mode (for example, different
   * windows accessing the dev mode server will each have a unique id, and
   * hitting refresh without restarting dev mode will result in a new unique id
   * for a particular window.
   *
   * TODO(unnurg): Remove this function once Dev Mode rewriting classes are in
   * gwt-dev.
   */
  public static String getUniqueThreadId() {
    if (sGWTBridge != null) {
      return sGWTBridge.getThreadUniqueID();
    }
    return "";
  }

  /**
   * Get a human-readable representation of the GWT version used, or null if
   * this is running on the client.
   *
   * @return a human-readable version number, such as {@code "2.5"}
   */
  public static String getVersion() {
    return sGWTBridge == null ? null : sGWTBridge.getVersion();
  }

  /**
   * Returns <code>true</code> when running inside the normal GWT environment,
   * either in Development Mode or Production Mode. Returns <code>false</code>
   * if this code is running in a plain JVM. This might happen when running
   * shared code on the server, or during the bootstrap sequence of a
   * GWTTestCase test.
   */
  public static boolean isClient() {
    // Replaced with "true" by GWT compiler.
    return sGWTBridge != null && sGWTBridge.isClient();
  }

  /**
   * Returns <code>true</code> when running in production mode. Returns
   * <code>false</code> when running either in development mode, or when running
   * in a plain JVM.
   */
  public static boolean isProdMode() {
    // Replaced with "true" by GWT compiler.
    return false;
  }

  /**
   * Determines whether or not the running program is script or bytecode.
   */
  public static boolean isScript() {
    // Replaced with "true" by GWT compiler.
    return false;
  }

  /**
   * Logs a message to the development shell logger in Development Mode, or to
   * the JavaScript console in Super Dev Mode. Calls are optimized out in Production Mode.
   */
  public static void log(String message) {
    log(message, null);
  }

  /**
   * Logs a message to the development shell logger in Development Mode, or to
   * the JavaScript console in Super Dev Mode. Calls are optimized out in Production Mode.
   */
  public static void log(String message, Throwable e) {
    if (sGWTBridge != null) {
      sGWTBridge.log(message, e);
    } else if (logger != null) {
      logger.log(message, e);
    }
  }

  /**
   * Emits a JavaScript "debugger" statement on the line that called this method.
   * If the user has the browser's debugger open, the debugger will stop when the
   * GWT application executes that line. There is no effect in Dev Mode or in
   * server-side code.
   */
  public static void debugger() {
  }

  /**
   * Called via reflection in Development Mode; do not ever call this method in
   * Production Mode.  May be called in server code to initialize server bridge.
   */
  public static void setBridge(GWTBridge bridge) {
    sGWTBridge = bridge;
  }
}
