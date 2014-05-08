/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.util.tools.ArgHandlerFile;

import java.io.File;

/**
 * Optionally specifies a file into which detailed missing dependency information will be written.
 */
public final class ArgHandlerMissingDepsFile extends ArgHandlerFile {

  private final OptionMissingDepsFile option;

  public ArgHandlerMissingDepsFile(OptionMissingDepsFile option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "Specifies a file into which detailed missing dependency information will be written.";
  }

  @Override
  public String getTag() {
    return "-missingDepsFile";
  }

  @Override
  public void setFile(File missingDepsFile) {
    option.setMissingDepsFile(missingDepsFile);
  }
}
