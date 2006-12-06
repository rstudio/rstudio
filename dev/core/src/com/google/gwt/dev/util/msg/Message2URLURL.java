// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.net.URL;

public final class Message2URLURL extends Message2 {

  public Message2URLURL(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, URL u1, URL u2, Throwable caught) {
    log2(logger, u1, u2, getFormatter(u1), getFormatter(u2), caught);
  }

  public TreeLogger branch(TreeLogger logger, URL u1, URL u2, Throwable caught) {
    return branch2(logger, u1, u2, getFormatter(u1), getFormatter(u2), caught);
  }
}
