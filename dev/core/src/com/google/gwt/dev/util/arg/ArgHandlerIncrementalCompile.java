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

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * Whether recompiles should process only changed files and construct JS output by
 * linking old and new JS on a per class basis.
 */
public class ArgHandlerIncrementalCompile extends ArgHandlerFlag {

  private final OptionIncrementalCompile option;

  public ArgHandlerIncrementalCompile(OptionIncrementalCompile option) {
    this.option = option;
    addTagValue("-XcompilePerFile", true);
  }

  @Override
  public String getPurposeSnippet() {
    return "Compiles faster by reusing data from the previous compile.";
  }

  @Override
  public String getLabel() {
    return "incremental";
  }

  @Override
  public boolean setFlag(boolean value) {
    option.setIncrementalCompileEnabled(value);
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return option.isIncrementalCompileEnabled();
  }
}
