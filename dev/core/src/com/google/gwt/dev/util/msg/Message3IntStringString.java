// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message3IntStringString extends Message3 {

  public Message3IntStringString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, int x, String s1, String s2,
      Throwable caught) {
    Integer xi = new Integer(x);
    return branch3(logger, xi, s1, s2, getFormatter(xi), getFormatter(s1),
      getFormatter(s2), caught);
  }

  public void log(TreeLogger logger, int x, String s1, String s2,
      Throwable caught) {
    Integer xi = new Integer(x);
    log3(logger, xi, s1, s2, getFormatter(xi), getFormatter(s1),
      getFormatter(s2), caught);
  }
}
