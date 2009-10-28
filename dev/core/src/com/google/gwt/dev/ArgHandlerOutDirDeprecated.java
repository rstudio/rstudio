/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.dev.util.arg.OptionOutDir;
import com.google.gwt.util.tools.ArgHandlerOutDir;

import java.io.File;

/**
 * Deprecated handler for -out options.
 */
@Deprecated
public class ArgHandlerOutDirDeprecated extends ArgHandlerOutDir {

  OptionOutDir option;

  public ArgHandlerOutDirDeprecated(OptionOutDir option) {
    this.option = option;
  }

  /**
   * Override the {@link ArgHandlerOutDir}'s default, which is to set and
   * outDir for the current directory. We don't want a default outDir because we
   * want a warDir to be the default if no options are specified.
   */
  @Override
  public String[] getDefaultArgs() {
    return null;
  }

  @Override
  public String getPurpose() {
    return super.getPurpose() + " (deprecated)";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public void setDir(File dir) {
    option.setOutDir(dir);
  }

}
