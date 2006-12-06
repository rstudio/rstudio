// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.log;

import javax.servlet.ServletContext;

public class ServletContextTreeLogger extends AbstractTreeLogger {

  public ServletContextTreeLogger(ServletContext ctx) {
    this.ctx = ctx;
  }

  protected AbstractTreeLogger doBranch() {
    return new ServletContextTreeLogger(ctx);
  }

  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught) {
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught);
  }

  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type,
      String msg, Throwable caught) {
    if (caught != null) {
      ctx.log(msg, caught);
    } else {
      ctx.log(msg);
    }
  }

  private final ServletContext ctx;
}
