/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerDir;

import java.io.File;

/**
 * Argument handler for processing the output directory flag.
 */
public class ArgHandlerWarDir extends ArgHandlerDir {

  private final OptionWarDir option;

  public ArgHandlerWarDir(OptionWarDir option) {
    this.option = option;
  }

  @Override
  public String[] getDefaultArgs() {
    return new String[]{getTag(), "war"};
  }

  @Override
  public String getPurpose() {
    return "The directory into which deployable output files will be written (defaults to 'war')";
  }

  @Override
  public String getTag() {
    return "-war";
  }

  @Override
  public void setDir(File dir) {
    option.setWarDir(dir);
  }

}
