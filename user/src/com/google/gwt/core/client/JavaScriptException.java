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

import com.google.gwt.core.client.impl.JavaScriptExceptionBase;

/**
 * Any JavaScript exceptions occurring within JSNI methods are wrapped as this
 * class when caught in Java code. The wrapping does not occur until the
 * exception passes out of JSNI into Java. Before that, the thrown object
 * remains a native JavaScript exception object, and can be caught in JSNI as
 * normal.
 * <p>
 * The return value of {@link #getStackTrace()} may vary between browsers due to
 * variations in the underlying error-reporting capabilities. When possible, the
 * stack trace will be the stack trace of the underlying error object. If it is
 * not possible to accurately report a stack trace, a zero-length array will be
 * returned. In those cases where the underlying stack trace cannot be
 * determined, {@link #fillInStackTrace()} can be called in the associated catch
 * block to create a stack trace corresponding to the location where the
 * JavaScriptException object was created.
 *
 * <pre>
 * try {
 *   nativeMethod();
 * } catch (JavaScriptException e) {
 *   if (e.getStackTrace().length == 0) {
 *     e.fillInStackTrace();
 *   }
 * }
 * </pre>
 */
public final class JavaScriptException extends JavaScriptExceptionBase {

  private static final Object NOT_SET = new Object();

  private static String getExceptionDescription(Object e) {
    if (e instanceof JavaScriptObject) {
      return getExceptionDescription0((JavaScriptObject) e);
    } else {
      return e + "";
    }
  }

  private static native String getExceptionDescription0(JavaScriptObject e) /*-{
    return (e == null) ? null : e.message;
  }-*/;

  private static String getExceptionName(Object e) {
    if (e == null) {
      return "null";
    } else if (e instanceof JavaScriptObject) {
      return getExceptionName0((JavaScriptObject) e);
    } else if (e instanceof String) {
      return "String";
    } else {
      return e.getClass().getName();
    }
  }

  private static native String getExceptionName0(JavaScriptObject e) /*-{
    return (e == null) ? null : e.name;
  }-*/;

  /**
   * The original description of the JavaScript exception this class wraps,
   * initialized as <code>e.message</code>.
   */
  private String description = "";

  /**
   * The underlying exception this class wraps.
   */
  private final Object e;

  /**
   * A constructed message describing this exception.
   */
  private String message;

  /**
   * The original type name of the JavaScript exception this class wraps,
   * initialized as <code>e.name</code>.
   */
  private String name;

  /**
   * @param e the object caught in JavaScript that triggered the exception
   */
  public JavaScriptException(Object e) {
    this(e, "");
  }

  /**
   * @param e the object caught in JavaScript that triggered the exception
   * @param description to include in getMessage(), e.g. at the top of a stack
   *          trace
   */
  public JavaScriptException(Object e, String description) {
    super(e);
    this.e = e;
    this.description = description;
  }

  public JavaScriptException(String name, String description) {
    super(null);
    this.message = "JavaScript " + name + " exception: " + description;
    this.name = name;
    this.description = description;
    this.e = NOT_SET;
  }

  /**
   * Used for testing instantiations.
   *
   * @param message the detail message
   */
  protected JavaScriptException(String message) {
    super(null);
    this.message = this.description = message;
    this.e = NOT_SET;
    fillInStackTrace();
  }

  /**
   * Returns {@code true} if a thrown object is not set for the exception.
   */
  public boolean isThrownSet() {
    return e != NOT_SET;
  }

  /**
   * Returns the original thrown object from javascript; may be {@code null}.
   */
  public Object getThrown() {
    return e == NOT_SET ? null : e;
  }

  /**
   * Returns the original JavaScript message of the exception; may be
   * <code>null</code>.
   */
  public String getDescription() {
    ensureInit();
    return description;
  }

  /**
   * Returns the original JavaScript the exception; may be <code>null</code>.
   *
   * @deprecated deprecated in favor for {@link #getThrown()} and {@link #isThrownSet()}
   */
  @Deprecated
  public JavaScriptObject getException() {
    return (e instanceof JavaScriptObject) ? (JavaScriptObject) e : null;
  }

  @Override
  public String getMessage() {
    ensureInit();
    return message;
  }

  /**
   * Returns the original JavaScript type name of the exception; may be
   * <code>null</code>.
   */
  public String getName() {
    ensureInit();
    return name;
  }

  private void ensureInit() {
    if (message == null) {
      Object exception = getThrown();
      name = getExceptionName(exception);
      description = description + ": " + getExceptionDescription(exception);
      message = "(" + name + ") " + description;
    }
  }

}
