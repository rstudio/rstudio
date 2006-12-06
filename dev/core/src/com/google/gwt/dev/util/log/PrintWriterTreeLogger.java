// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.log;

import java.io.PrintWriter;

public final class PrintWriterTreeLogger extends AbstractTreeLogger {

  public PrintWriterTreeLogger() {
    this(new PrintWriter(System.out, true));
  }

  public PrintWriterTreeLogger(PrintWriter out) {
    this(out, "");
  }

  protected PrintWriterTreeLogger(PrintWriter out, String indent) {
    this.out = out;
    this.indent = indent;
  }

  protected AbstractTreeLogger doBranch() {
    return new PrintWriterTreeLogger(out, indent + "   ");
  }

  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught) {
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught);
  }

  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type,
      String msg, Throwable caught) {
    out.print(indent);
    if (type.needsAttention()) {
      out.print("[");
      out.print(type.getLabel());
      out.print("] ");
    }

    out.println(msg);
    if (caught != null) {
      caught.printStackTrace(out);
    }
  }

  private final PrintWriter out;
  private final String indent;
}
