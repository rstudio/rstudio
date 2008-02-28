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

/**
 * This class contains a set of static methods that can be used to interact with
 * the browser in hosted mode.
 */
public class JavaScriptHost {

  private static ShellJavaScriptHost sHost;

  /**
   * Defines a new native JavaScript function.
   * 
   * @param file source file of the function
   * @param line starting line number of the function
   * @param jsniSignature the function's jsni signature
   * @param paramNames the parameter types
   * @param js the script body
   */
  public static void createNative(String file, int line, String jsniSignature,
      String[] paramNames, String js) {
    sHost.createNative(file, line, jsniSignature, paramNames, js);
  }

  public static void exceptionCaught(int number, String name, String description) {
    sHost.exceptionCaught(number, name, description);
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
   * Logs in dev shell.
   */
  public static void log(String message, Throwable e) {
    sHost.log(message, e);
  }

  /**
   * Resolves a deferred binding request and create the requested object.
   */
  public static <T> T rebindAndCreate(Class<?> requestedClass) {
    String className = requestedClass.getName();
    try {
      return sHost.<T>rebindAndCreate(className);
    } catch (Throwable e) {
      String msg = "Deferred binding failed for '" + className
          + "' (did you forget to inherit a required module?)";
      throw new RuntimeException(msg, e);
    }
  }

  /**
   * This method is called via reflection from the shell, providing the hosted
   * mode application with all of the methods it needs to interface with the
   * browser and the server (for deferred binding).
   */
  public static void setHost(ShellJavaScriptHost host) {
    sHost = host;
  }
}
