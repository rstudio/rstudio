// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message1ToString extends Message1 {

  public Message1ToString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, Object o, Throwable caught) {
    return branch1(logger, o, getToStringFormatter(), caught);
  }

  public void log(TreeLogger logger, Object o, Throwable caught) {
    log1(logger, o, getToStringFormatter(), caught);
  }
}
