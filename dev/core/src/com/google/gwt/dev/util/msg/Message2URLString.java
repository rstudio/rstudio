// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.net.URL;

public final class Message2URLString extends Message2 {

  public Message2URLString(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, URL u, String s, Throwable caught) {
    log2(logger, u, s, getFormatter(u), getFormatter(s), caught);
  }

  public TreeLogger branch(TreeLogger logger, URL u, String s, Throwable caught) {
    return branch2(logger, u, s, getFormatter(u), getFormatter(s), caught);
  }
}
