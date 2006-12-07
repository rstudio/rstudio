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
package com.google.gwt.core.client;

/**
 * Supports core functionality that in some cases requires direct support from
 * the compiler and runtime systems such as runtime type information and
 * deferred binding.
 */
public final class GWT {

  /**
   * This interface is used to catch exceptions at the "top level" just before
   * they escape to the browser. This is used in places where the browser calls
   * into user code such as event callbacks, timers, and RPC.
   * 
   * In hosted mode, the default handler prints a stack trace to the log window.
   * In web mode, the default handler is null and thus exceptions are allowed to
   * escape, which provides an opportunity to use a JavaScript debugger.
   */
  public interface UncaughtExceptionHandler {
    void onUncaughtException(Throwable e);
  }

  // web mode default is to let the exception go
  // hosted mode default is to log the exception to the log window
  private static UncaughtExceptionHandler sUncaughtExceptionHandler = null;

  /**
   * Instantiates a class via deferred binding.
   * 
   * <p>
   * The argument to {@link #create(Class)}&#160;<i>must</i> be a class
   * literal because the web mode compiler must be able to statically determine
   * the requested type at compile-time. This can be tricky because using a
   * {@link Class} variable may appear to work correctly in hosted mode.
   * </p>
   * 
   * @param classLiteral a class literal specifying the base class to be
   *          instantiated
   * @return the new instance, which must be typecast to the requested class.
   */
  public static Object create(Class classLiteral) {
    /*
     * In hosted mode, this whole class definition is replaced at runtime with
     * an implementation defined by the hosting environment. Maintainers: see
     * {@link com.google.gwt.dev.shell.HostedModeSourceOracle#CU_Meta}.
     * 
     * In web mode, the compiler directly replaces calls to this method with a
     * new Object() type expression of the correct rebound type.
     */
    throw new RuntimeException(
        "GWT has not been properly initialized; if you are running a unit test, check that your test case extends GWTTestCase");
  }

 /**
   * Gets the URL prefix of the module which should be prepended to URLs that
   * are intended to be module-relative, such as RPC entry points and files in
   * the module's public path.
   * 
   * @return if non-empty, the base URL is guaranteed to end with a slash
   */
  public static String getModuleBaseURL() {
    return Impl.getModuleBaseURL();
  }

 /**
   * Gets the name of the running module.
   */
  public static native String getModuleName() /*-{
   return $moduleName;
   }-*/;

 /**
   * Gets the class name of the specified object, as would be returned by
   * <code>o.getClass().getName()</code>.
   * 
   * @param o the object whose class name is being sought, or <code>null</code>
   * @return the class name of the specified object, or <code>null</code> if
   *         <code>o</code> is <code>null</code>
   */
  public static native String getTypeName(Object o) /*-{
    return (o == null) ? null : o.@java.lang.Object::typeName;
  }-*/;

  /**
   * Returns the currently active uncaughtExceptionHandler. "Top level" methods
   * that dispatch events from the browser into user code must call this method
   * on entry to get the active handler. If the active handler is null, the
   * entry point must allow exceptions to escape into the browser. If the
   * handler is non-null, exceptions must be caught and routed to the handler.
   * See the source code for
   * <code>{@link com.google.gwt.user.client.DOM}.dispatchEvent()</code> for
   * an example of how to handle this correctly.
   * 
   * @return the currently active handler, or null if no handler is active.
   */
  public static UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return sUncaughtExceptionHandler;
  };

  /**
   * Determines whether or not the running program is script or bytecode.
   */
  public static boolean isScript() {
    return true;
  }

  /**
   * Logs a message to the development shell logger in hosted mode. Calls are
   * optimized out in web mode.
   */
  public static void log(String message, Throwable e) {
    // intentionally empty in web mode.
  }

  /**
   * Sets a custom uncaught exception handler. See
   * {@link #getUncaughtExceptionHandler()} for details.
   * 
   * @param handler the handler that should be called when an exception is about
   *          to escape to the browser, or <code>null</code> to clear the
   *          handler and allow exceptions to escape.
   */
  public static void setUncaughtExceptionHandler(
      UncaughtExceptionHandler handler) {
    sUncaughtExceptionHandler = handler;
  };
}
