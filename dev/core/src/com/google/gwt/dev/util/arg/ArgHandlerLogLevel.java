// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.arg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.util.tools.ArgHandler;

/**
 * Arugment handler for processing the log level flag.
 */
public abstract class ArgHandlerLogLevel extends ArgHandler {

  public String[] getDefaultArgs() {
    return new String[]{getTag(), "INFO"};
  }

  public String getPurpose() {
    return "The level of logging detail: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL";
  }

  public String getTag() {
    return "-logLevel";
  }

  public String[] getTagArgs() {
    return new String[]{"level"};
  }

  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      TreeLogger.Type level = TreeLogger.Type.valueOf(args[startIndex + 1]);
      if (level != null) {
        setLogLevel(level);
        return 1;
      }
    }

    System.err.println(getTag() + " should be followed by one of");
    System.err.println("  ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL");
    return -1;
  }

  public abstract void setLogLevel(Type level);

}
