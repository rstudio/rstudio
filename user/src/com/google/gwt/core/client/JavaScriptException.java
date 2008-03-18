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

/**
 * Any JavaScript exceptions occurring within JSNI methods are wrapped as this
 * class when caught in Java code. The wrapping does not occur until the
 * exception passes out of JSNI into Java. Before that, the thrown object
 * remains a native JavaScript exception object, and can be caught in JSNI as
 * normal.
 */
public final class JavaScriptException extends RuntimeException {

  private static String constructMessage(Object e) {
    return "(" + getName(e) + "): " + getDescription(e) + getProperties(e);
  }

  private static String getDescription(Object e) {
    if (e instanceof JavaScriptObject) {
      return getDescription0((JavaScriptObject) e);
    } else {
      return e + "";
    }
  }

  private static native String getDescription0(JavaScriptObject e) /*-{
    if (e == null) {
      return null;
    }
    return e.message == null ? null : e.message;
  }-*/;

  private static JavaScriptObject getException(Object e) {
    if (e instanceof JavaScriptObject) {
      return (JavaScriptObject) e;
    } else {
      return null;
    }
  }

  private static String getName(Object e) {
    if (e == null) {
      return "null";
    } else if (e instanceof JavaScriptObject) {
      return getName0((JavaScriptObject) e);
    } else if (e instanceof String) {
      return "String";
    } else {
      return e.getClass().getName();
    }
  }

  private static native String getName0(JavaScriptObject e) /*-{
    if (e == null) {
      return null;
    }
    return e.name == null ? null : e.name;
  }-*/;

  private static String getProperties(Object e) {
    if (e instanceof JavaScriptObject) {
      return getProperties0((JavaScriptObject) e);
    } else {
      return null;
    }
  }

  /**
   * Returns the list of properties of an unexpected JavaScript exception.
   */
  private static native String getProperties0(JavaScriptObject e) /*-{
    var result = "";
    for (prop in e) {
      if (prop != "name" && prop != "description") {
        result += "\n " + prop + ": " + e[prop];
      }
    }
    return result;
  }-*/;

  /**
   * The original description of the JavaScript exception this class wraps,
   * initialized as <code>e.message</code>.
   */
  private final String description;

  /**
   * The underlying exception this class wraps.
   */
  private final JavaScriptObject exception;

  /**
   * The original type name of the JavaScript exception this class wraps,
   * initialized as <code>e.name</code>.
   */
  private final String name;

  /**
   * @param exception
   */
  public JavaScriptException(Object e) {
    super(constructMessage(e));
    this.name = getName(e);
    this.description = getDescription(e);
    this.exception = getException(e);
  }

  public JavaScriptException(String name, String description) {
    super("JavaScript " + name + " exception: " + description);
    this.name = name;
    this.description = description;
    this.exception = null;
  }

  /**
   * Used for server-side instantiation during JUnit runs. Exceptions are
   * manually marshaled through
   * {@link com.google.gwt.junit.client.impl.ExceptionWrapper} objects.
   * 
   * @param message the detail message
   */
  protected JavaScriptException(String message) {
    super(message);
    this.name = null;
    this.description = message;
    this.exception = null;
  }

  /**
   * Returns the original JavaScript message of the exception; may be
   * <code>null</code>.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the original JavaScript the exception; may be <code>null</code>.
   */
  public JavaScriptObject getException() {
    return exception;
  }

  /**
   * Returns the original JavaScript type name of the exception; may be
   * <code>null</code>.
   */
  public String getName() {
    return name;
  }

}
