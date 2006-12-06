// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message2LongString extends Message2 {

  public Message2LongString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, long x, String s, Throwable caught) {
    Long xi = new Long(x);
    return branch2(logger, xi, s, getFormatter(xi), getFormatter(s), caught);
  }

  public void log(TreeLogger logger, long x, String s, Throwable caught) {
    Long xi = new Long(x);
    log2(logger, xi, s, getFormatter(xi), getFormatter(s), caught);
  }
}
