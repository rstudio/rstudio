// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message2StringString extends Message2 {

  public Message2StringString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, String s1, String s2,
      Throwable caught) {
    return branch2(logger, s1, s2, getFormatter(s1), getFormatter(s2), caught);
  }

  public void log(TreeLogger logger, String s1, String s2, Throwable caught) {
    log2(logger, s1, s2, getFormatter(s1), getFormatter(s2), caught);
  }
}
