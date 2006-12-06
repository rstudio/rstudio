// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message2StringInt extends Message2 {

  public Message2StringInt(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, String s, int x, Throwable caught) {
    Integer xi = new Integer(x);
    log2(logger, s, xi, getFormatter(s), getFormatter(xi), caught);
  }

  public TreeLogger branch(TreeLogger logger, String s, int x, Throwable caught) {
    Integer xi = new Integer(x);
    return branch2(logger, s, xi, getFormatter(s), getFormatter(xi), caught);
  }

}
