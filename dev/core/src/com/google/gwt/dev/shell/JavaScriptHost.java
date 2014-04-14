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
 * This class contains a set of static methods that can be used to interact with
 * the browser in hosted mode.
 */
public class JavaScriptHost {

  private static ShellJavaScriptHost sHost;

  /**
   * Throws {@code NullPointerException} if {@code o} is null.
   *<p>
   * Used by {@code RewriteJsniMethods} to ensure JSO instance methods
   * are not called against null instances.
   */
  public static Object checkNotNull(Object o) {
    if (o == null) {
      throw new NullPointerException("JSO instance was null");
    }
    return o;
  }

  public static void exceptionCaught(Object exception) {
    sHost.exceptionCaught(exception);
  }

  /**
   * Invoke a native JavaScript function that returns a boolean value.
   */
  public static boolean invokeNativeBoolean(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeBoolean(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns a byte value.
   */
  public static byte invokeNativeByte(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeByte(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns a character value.
   */
  public static char invokeNativeChar(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeChar(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns a double value.
   */
  public static double invokeNativeDouble(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeDouble(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns a float value.
   */
  public static float invokeNativeFloat(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeFloat(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns an integer value.
   */
  public static int invokeNativeInt(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeInt(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns a long value.
   */
  public static long invokeNativeLong(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeLong(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns an object value.
   */
  public static Object invokeNativeObject(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeObject(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns a short value.
   */
  public static short invokeNativeShort(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    return sHost.invokeNativeShort(name, jthis, types, args);
  }

  /**
   * Invoke a native JavaScript function that returns no value.
   */
  public static void invokeNativeVoid(String name, Object jthis,
      Class<?>[] types, Object[] args) throws Throwable {
    sHost.invokeNativeVoid(name, jthis, types, args);
  }

  /**
   * This method is called via reflection from the {@link CompilingClassLoader},
   * providing the hosted mode application with all of the methods it needs to
   * interface with the browser and the server (for deferred binding).
   */
  public static void setHost(ShellJavaScriptHost host) {
    sHost = host;
  }
}
