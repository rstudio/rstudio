// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

abstract class ChangeBase implements Change {
  
  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(pw);
    logger.setMaxDetail(TreeLogger.INFO);
    describe(logger, TreeLogger.INFO);
    return sw.toString();
  }
  
}
