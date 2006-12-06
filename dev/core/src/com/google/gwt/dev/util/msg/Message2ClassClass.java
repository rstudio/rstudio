// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message2ClassClass extends Message2 {

  public Message2ClassClass(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, Class c1, Class c2, Throwable caught) {
    log2(logger, c1, c2, getFormatter(c1), getFormatter(c2), caught);
  }
  
  public TreeLogger branch(TreeLogger logger, Class c1, Class c2, Throwable caught) {
    return branch2(logger, c1, c2, getFormatter(c1), getFormatter(c2), caught);
  }
}
