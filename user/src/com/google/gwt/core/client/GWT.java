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
package com.google.gwt.core.client;

import com.google.gwt.core.client.impl.AsyncFragmentLoader;
import com.google.gwt.core.client.impl.Impl;

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

  /**
   * An {@link UncaughtExceptionHandler} that logs errors to
   * {@link GWT#log(String, Throwable)}. This is the default exception handler
   * in hosted mode. In web mode, the default exception handler is
   * <code>null</code>.
   */
  private static final class DefaultUncaughtExceptionHandler implements
      UncaughtExceptionHandler {
    public void onUncaughtException(Throwable e) {
      log("Uncaught exception escaped", e);
    }
  }

  /**
   * This constant is used by {@link #getPermutationStrongName} when running in
   * hosted mode.
   */
  public static final String HOSTED_MODE_PERMUTATION_STRONG_NAME = "HostedMode";

  /**
   * Always <code>null</code> in web mode; in hosted mode provides the
   * implementation for certain methods.
   */
  private static GWTBridge sGWTBridge = null;

  /**
   * Defaults to <code>null</code> in web mode and an instance of
   * {@link DefaultUncaughtExceptionHandler} in hosted mode.
   */
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
  @SuppressWarnings("unused")
  public static <T> T create(Class<?> classLiteral) {
    if (sGWTBridge == null) {
      /*
       * In web mode, the compiler directly replaces calls to this method with a
       * new Object() type expression of the correct rebound type.
       */
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
   * Gets the URL prefix of the hosting page, useful for prepending to relative
   * paths of resources which may be relative to the host page. Typically, you
   * should use {@link #getModuleBaseURL()} unless you have a specific reason to
   * load a resource relative to the host page.
   * 
   * @return if non-empty, the base URL is guaranteed to end with a slash
   */
  public static String getHostPageBaseURL() {
    return Impl.getHostPageBaseURL();
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
  public static String getModuleName() {
    return Impl.getModuleName();
  }

  /**
   * Returns the permutation's strong name. This can be used to distinguish
   * between different permutations of the same module. In hosted mode, this
   * method will return {@value #HOSTED_MODE_PERMUTATION_STRONG_NAME}.
   */
  public static String getPermutationStrongName() {
    if (GWT.isScript()) {
      return Impl.getPermutationStrongName();
    } else {
      return HOSTED_MODE_PERMUTATION_STRONG_NAME;
    }
  }

  /**
   * @deprecated Use {@link Object#getClass()}, {@link Class#getName()}
   */
  @Deprecated
  public static String getTypeName(Object o) {
    return (o == null) ? null : o.getClass().getName();
  }

  /**
   * Returns the currently active uncaughtExceptionHandler. "Top level" methods
   * that dispatch events from the browser into user code must call this method
   * on entry to get the active handler. If the active handler is null, the
   * entry point must allow exceptions to escape into the browser. If the
   * handler is non-null, exceptions must be caught and routed to the handler.
   * See the source code for <code>DOM.dispatchEvent()</code> for an example
   * of how to handle this correctly.
   * 
   * @return the currently active handler, or null if no handler is active.
   */
  public static UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return sUncaughtExceptionHandler;
  }

  public static String getVersion() {
    if (sGWTBridge == null) {
      return getVersion0();
    } else {
      return sGWTBridge.getVersion();
    }
  }

  /**
   * Returns <code>true</code> when running inside the normal GWT environment,
   * either in hosted mode or web mode. Returns <code>false</code> if this
   * code is running in a plain JVM. This might happen when running shared code
   * on the server, or during the bootstrap sequence of a GWTTestCase test.
   */
  public static boolean isClient() {
    // Replaced with "true" by GWT compiler.
    return sGWTBridge != null && sGWTBridge.isClient();
  }

  /**
   * Determines whether or not the running program is script or bytecode.
   */
  public static boolean isScript() {
    // Replaced with "true" by GWT compiler.
    return false;
  }

  /**
   * Logs a message to the development shell logger in hosted mode. Calls are
   * optimized out in web mode.
   */
  @SuppressWarnings("unused")
  public static void log(String message, Throwable e) {
    if (sGWTBridge != null) {
      sGWTBridge.log(message, e);
    }
  }

  /**
   * The same as {@link #runAsync(RunAsyncCallback)}, except with an extra
   * parameter to provide a name for the call. The name parameter should be
   * supplied with a class literal. No two runAsync calls in the same program
   * should use the same name.
   */
  public static void runAsync(Class<?> name, RunAsyncCallback callback) {
    runAsyncWithoutCodeSplitting(callback);
  }

  /**
   * Run the specified callback once the necessary code for it has been loaded.
   */
  public static void runAsync(RunAsyncCallback callback) {
    runAsyncWithoutCodeSplitting(callback);
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
  }

  /**
   * Called via reflection in hosted mode; do not ever call this method in web
   * mode.
   */
  static void setBridge(GWTBridge bridge) {
    sGWTBridge = bridge;
    if (bridge != null) {
      setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());
    }
  }

  private static native String getVersion0() /*-{
    return $gwt_version;
  }-*/;

  /**
   * This implementation of runAsync simply calls the callback. It is only used
   * when no code splitting has occurred.
   */
  private static void runAsyncWithoutCodeSplitting(RunAsyncCallback callback) {
    /*
     * By default, just call the callback. This allows using
     * <code>runAsync</code> in code that might or might not run in a web
     * browser.
     */
    if (isScript()) {
      /*
       * It's possible that the code splitter does not run, even for a
       * production build. Signal a lightweight event, anyway, just so that
       * there isn't a complete lack of lightweight events for runAsync.
       */
      AsyncFragmentLoader.BROWSER_LOADER.logEventProgress("noDownloadNeeded", "begin");
      AsyncFragmentLoader.BROWSER_LOADER.logEventProgress("noDownloadNeeded", "end");
    }

    UncaughtExceptionHandler handler = sUncaughtExceptionHandler;
    if (handler == null) {
      callback.onSuccess();
    } else {
      try {
        callback.onSuccess();
      } catch (Throwable e) {
        handler.onUncaughtException(e);
      }
    }
  }
}
