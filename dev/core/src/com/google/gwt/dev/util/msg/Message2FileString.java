// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.io.File;

public final class Message2FileString extends Message2 {

  public Message2FileString(Type type, String fmt) {
    super(type, fmt);
  }

  public void log(TreeLogger logger, File f, String s, Throwable caught) {
    log2(logger, f, s, getFormatter(f), getFormatter(s), caught);
  }

  public TreeLogger branch(TreeLogger logger, File f, String s, Throwable caught) {
    return branch2(logger, f, s, getFormatter(f), getFormatter(s), caught);
  }
}
