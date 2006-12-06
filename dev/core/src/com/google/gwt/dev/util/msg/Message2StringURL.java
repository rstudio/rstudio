// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.net.URL;

public final class Message2StringURL extends Message2 {

  public Message2StringURL(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, String s, URL u, Throwable caught) {
    return branch2(logger, s, u, getFormatter(s), getFormatter(u), caught);
  }

  public void log(TreeLogger logger, String s, URL u, Throwable caught) {
    log2(logger, s, u, getFormatter(s), getFormatter(u), caught);
  }

}
