// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class Message0 extends Message {

  public Message0(Type type, String fmt) {
    super(type, fmt, 0);
  }

  public void log(TreeLogger logger, Throwable caught) {
    if (logger.isLoggable(fType))
      logger.log(fType, new String(fFmtParts[0]), caught);
  }

  public TreeLogger branch(TreeLogger logger, Throwable caught) {
    // Always branch, even if the branch root is not loggable.
    // See TreeLogger.branch() for details as to why.
    //
    return logger.branch(fType, new String(fFmtParts[0]), caught);
  }
}
