// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerDir;

/**
 * Argument handler for processing the code generation directory flag. 
 */
public abstract class ArgHandlerGenDir extends ArgHandlerDir {

  public String getPurpose() {
    return "The directory into which generated files will be written for review";
  }

  public String getTag() {
    return "-gen";
  }
}