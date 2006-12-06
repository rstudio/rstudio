// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.net.URL;

public final class Message2URLInt extends Message2 {

  public Message2URLInt(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, URL u, int x, Throwable caught) {
    Integer xi = new Integer(x);
    log2(logger, u, xi, getFormatter(u), getFormatter(xi), caught);
  }

  public TreeLogger branch(TreeLogger logger, URL u, int x, Throwable caught) {
    Integer xi = new Integer(x);
    return branch2(logger, u, xi, getFormatter(u), getFormatter(xi), caught);
  }
}
