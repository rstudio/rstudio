/*
 * Copyright 2013 Google Inc.
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
 * Argument handler for processing the -saveSourceOutput option.
 */
public final class ArgHandlerSaveSourceOutput extends ArgHandlerDir {

  private final OptionSaveSourceOutput option;

  public ArgHandlerSaveSourceOutput(OptionSaveSourceOutput option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "Overrides where source files useful to debuggers will be written. "
        + "Default: saved with extras.";
  }

  @Override
  public String getTag() {
    return "-saveSourceOutput";
  }

  @Override
  public void setDir(File dir) {
    option.setSaveSourceOutput(dir);
  }
}
