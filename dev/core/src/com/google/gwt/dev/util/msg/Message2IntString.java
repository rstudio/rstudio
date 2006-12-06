// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message2IntString extends Message2 {

  public Message2IntString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, int x, String s, Throwable caught) {
    Integer xi = new Integer(x);
    return branch2(logger, xi, s, getFormatter(xi), getFormatter(s), caught);
  }

  public void log(TreeLogger logger, int x, String s, Throwable caught) {
    Integer xi = new Integer(x);
    log2(logger, xi, s, getFormatter(xi), getFormatter(s), caught);
  }

}
