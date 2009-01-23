/**
 * 
 */
package com.google.gwt.dev;

import com.google.gwt.dev.util.arg.OptionOutDir;
import com.google.gwt.util.tools.ArgHandlerOutDir;

import java.io.File;

/**
 * Deprecated handler for -out options
 */
public class ArgHandlerOutDirDeprecated extends ArgHandlerOutDir {

  OptionOutDir option;
  
  public ArgHandlerOutDirDeprecated(OptionOutDir option) {
    this.option = option;
  }
  
  public String getPurpose() {
    return super.getPurpose() + " (deprecated)";
  }

  public boolean isUndocumented() {
    return true;
  }

  @Override
  public void setDir(File dir) {
    option.setOutDir(dir);
  }
  
}
