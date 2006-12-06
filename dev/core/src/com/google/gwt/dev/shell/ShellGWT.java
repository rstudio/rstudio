// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

/**
 * This class is the hosted-mode peer for {@link com.google.gwt.core.client.GWT}.
 */
public class ShellGWT {

  public static Object create(Class classLiteral) {
    return JavaScriptHost.rebindAndCreate(classLiteral);
  }

  public static String getTypeName(Object o) {
    return o != null ? o.getClass().getName() : null;
  }
  
  public static void log(String message, Throwable e) {
    JavaScriptHost.log(message, e);
  }
}
