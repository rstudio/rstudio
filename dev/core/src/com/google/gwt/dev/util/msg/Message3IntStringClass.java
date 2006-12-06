// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message3IntStringClass extends Message3 {

  public Message3IntStringClass(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, int x, String s, Class c,
      Throwable caught) {
    Integer xi = new Integer(x);
    return branch3(logger, xi, s, c, getFormatter(xi), getFormatter(s),
      getFormatter(c), caught);
  }

  public void log(TreeLogger logger, int x, String s, Class c, Throwable caught) {
    Integer xi = new Integer(x);
    log3(logger, xi, s, c, getFormatter(xi), getFormatter(s), getFormatter(c),
      caught);
  }
}
