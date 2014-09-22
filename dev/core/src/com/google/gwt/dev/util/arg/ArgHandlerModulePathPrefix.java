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

import com.google.gwt.util.tools.ArgHandlerString;

/**
 * Argument handler for processing a prefix for the output directory.
 */
public class ArgHandlerModulePathPrefix extends ArgHandlerString {

  private final OptionModulePathPrefix option;

  public ArgHandlerModulePathPrefix(OptionModulePathPrefix option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "The subdirectory inside the war dir where DevMode will create module"
        + " directories. (defaults empty for top level)";
  }

  @Override
  public String getTag() {
    return "-modulePathPrefix";
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{""};
  }

  @Override
  public boolean setString(String str) {
    option.setModulePathPrefix(str);
    return true;
  }
}
