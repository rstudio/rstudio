// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.net.URL;

public final class Message1URL extends Message1 {

  public Message1URL(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, URL u, Throwable caught) {
    return branch1(logger, u, getFormatter(u), caught);
  }

  public void log(TreeLogger logger, URL u, Throwable caught) {
    log1(logger, u, getFormatter(u), caught);
  }
}
