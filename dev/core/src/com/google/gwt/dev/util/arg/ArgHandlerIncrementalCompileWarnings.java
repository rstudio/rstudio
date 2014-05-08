/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * Toggles the display of warnings (during monolithic compiles) for issues that will break in
 * incremental compiles.
 */
public final class ArgHandlerIncrementalCompileWarnings extends ArgHandlerFlag {

  private final OptionStrict optionStrict;
  private final OptionStrictSourceResources optionStrictSourceResources;
  private final OptionWarnMissingDeps optionWarnMissingDeps;

  public <
      T extends OptionWarnMissingDeps & OptionStrictSourceResources &
                OptionStrict> ArgHandlerIncrementalCompileWarnings(T option) {
    this.optionWarnMissingDeps = option;
    this.optionStrictSourceResources = option;
    this.optionStrict = option;
  }

  @Override
  public boolean getDefaultValue() {
    return false;
  }

  @Override
  public String getLabel() {
    return "incrementalCompileWarnings";
  }

  @Override
  public String getPurposeSnippet() {
    return "Whether to show warnings during monolithic compiles for issues that "
        + "will break in incremental compiles (strict compile errors, strict source "
        + "directory inclusion, missing dependencies).";
  }

  @Override
  public boolean setFlag(boolean value) {
    optionWarnMissingDeps.setWarnMissingDeps(value);
    optionStrictSourceResources.setEnforceStrictSourceResources(value);
    optionStrict.setStrict(value);
    return true;
  }
}
