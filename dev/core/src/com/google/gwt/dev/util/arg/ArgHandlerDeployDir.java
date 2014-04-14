/*
 * Copyright 2010 Google Inc.
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
 * Argument handler for processing the deploy directory flag.
 */
public class ArgHandlerDeployDir extends ArgHandlerDir {
  // The default argument is handled in LinkOptionsImpl since it is relative
  // to the war directory.

  private final OptionDeployDir option;

  public ArgHandlerDeployDir(OptionDeployDir option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "The directory into which deployable but not servable output files"
        + " will be written (defaults to 'WEB-INF/deploy' under the -war "
        + "directory/jar, and may be the same as the -extra directory/jar)";
  }

  @Override
  public String getTag() {
    return "-deploy";
  }

  @Override
  public void setDir(File dir) {
    option.setDeployDir(dir);
  }
}
