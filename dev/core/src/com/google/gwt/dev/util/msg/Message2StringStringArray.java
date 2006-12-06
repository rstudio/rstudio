// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message2StringStringArray extends Message2 {
  public Message2StringStringArray(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, String s, String[] sa, Throwable caught) {
    log2(logger, s, sa, getFormatter(s), getFormatter(sa), caught);
  }

  public TreeLogger branch(TreeLogger logger, String s, String[] sa,
      Throwable caught) {
    return branch2(logger, s, sa, getFormatter(s), getFormatter(sa), caught);
  }
}
