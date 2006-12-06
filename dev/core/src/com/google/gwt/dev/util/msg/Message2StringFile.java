// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.io.File;

public final class Message2StringFile extends Message2 {

  public Message2StringFile(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, String s, File f, Throwable caught) {
    return branch2(logger, s, f, getFormatter(s), getFormatter(f), caught);
  }

  public void log(TreeLogger logger, String s, File f, Throwable caught) {
    log2(logger, s, f, getFormatter(s), getFormatter(f), caught);
  }

}
