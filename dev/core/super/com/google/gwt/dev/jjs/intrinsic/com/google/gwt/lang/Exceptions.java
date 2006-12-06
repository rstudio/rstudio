// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptException;

/**
 * This is a magic class the compiler uses to throw and check exceptions.
 */
final class Exceptions {
  
  static Object caught(Object e) {
    if (e instanceof Throwable)
      return e;
    return new JavaScriptException(javaScriptExceptionName(e),
      javaScriptExceptionDescription(e));
  }

  /**
   * Returns the name of an unexpected JavaScript exception (not a normal Java
   * one).
   */
  private static native String javaScriptExceptionName(Object e) /*-{
    return e.name;
  }-*/;

  /**
   * Returns the description of an unexpected JavaScript exception (not a normal
   * Java one).
   */
  private static native String javaScriptExceptionDescription(Object e) /*-{
    return e.message;
  }-*/;

  /**
   * Easily throw a ClassCastException from native or generated code.
   */
  static void throwClassCastException() throws ClassCastException {
    throw new ClassCastException();
  }

  /**
   * Easily throw a NullPointerException from native or generated code.
   */
  static void throwNullPointerException() throws NullPointerException {
    throw new NullPointerException();
  }

}
