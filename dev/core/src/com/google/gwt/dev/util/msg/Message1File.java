// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.io.File;

public final class Message1File extends Message1 {

  public Message1File(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, File f, Throwable caught) {
    return branch1(logger, f, getFormatter(f), caught);
  }

  public void log(TreeLogger logger, File f, Throwable caught) {
    log1(logger, f, getFormatter(f), caught);
  }
}
