// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * Argument handler for processing the tree logger boolean flag. 
 */
public abstract class ArgHandlerTreeLoggerFlag extends ArgHandlerFlag {

  public String getPurpose() {
    return "Logs output in a graphical tree view";
  }

  public String getTag() {
    return "-treeLogger";
  }

  public abstract boolean setFlag();
}