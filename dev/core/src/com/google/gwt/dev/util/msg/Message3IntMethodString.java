// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.lang.reflect.Method;

public final class Message3IntMethodString extends Message3 {

  public Message3IntMethodString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, int x, Method m, String s,
      Throwable caught) {
    Integer xi = new Integer(x);
    return branch3(logger, xi, m, s, getFormatter(xi), getFormatter(m),
      getFormatter(s), caught);
  }


  public void log(TreeLogger logger, int x, Method m, String s, Throwable caught) {
    Integer xi = new Integer(x);
    log3(logger, xi, m, s, getFormatter(xi), getFormatter(m), getFormatter(s),
      caught);
  }

}
