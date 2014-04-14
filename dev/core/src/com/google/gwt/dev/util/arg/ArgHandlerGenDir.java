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
 * Argument handler for processing the code generation directory flag.
 */
public final class ArgHandlerGenDir extends ArgHandlerDir {

  private final OptionGenDir option;

  public ArgHandlerGenDir(OptionGenDir option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "Debugging: causes normally-transient generated types to be saved in the specified directory";
  }

  @Override
  public String getTag() {
    return "-gen";
  }

  @Override
  public void setDir(File dir) {
    option.setGenDir(dir);
  }
}