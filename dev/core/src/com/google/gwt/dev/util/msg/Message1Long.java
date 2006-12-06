// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message1Long extends Message1 {

  public Message1Long(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, long x, Throwable caught) {
    Long xl = new Long(x);
    return branch1(logger, xl, getFormatter(xl), caught);
  }

  public void log(TreeLogger logger, long x, Throwable caught) {
    Long xl = new Long(x);
    log1(logger, xl, getFormatter(xl), caught);
  }

}
