// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message1StringArray extends Message1 {

  public Message1StringArray(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, String[] sa, Throwable caught) {
    log1(logger, sa, getFormatter(sa), caught);
  }

  public TreeLogger branch(TreeLogger logger, String[] sa, Throwable caught) {
    return branch1(logger, sa, getFormatter(sa), caught);
  }
}
