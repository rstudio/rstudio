// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message3StringIntString extends Message3 {

  public Message3StringIntString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, String s1, int x, String s2,
      Throwable caught) {
    Integer xi = new Integer(x);
    return branch3(logger, s1, xi, s2, getFormatter(s1), getFormatter(xi),
      getFormatter(s2), caught);
  }

  public void log(TreeLogger logger, String s1, int x, String s2,
      Throwable caught) {
    Integer xi = new Integer(x);
    log3(logger, s1, xi, s2, getFormatter(s1), getFormatter(xi),
      getFormatter(s2), caught);
  }

}
