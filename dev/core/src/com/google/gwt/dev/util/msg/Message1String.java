// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message1String extends Message1 {

  public Message1String(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, String s, Throwable caught) {
    return branch1(logger, s, getFormatter(s), caught);
  }

  public void log(TreeLogger logger, String s, Throwable caught) {
    log1(logger, s, getFormatter(s), caught);
  }
}
