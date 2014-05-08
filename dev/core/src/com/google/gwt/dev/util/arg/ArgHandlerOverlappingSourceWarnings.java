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
 * Toggles the display of overlapping source include warnings (during monolithic compiles).
 */
public final class ArgHandlerOverlappingSourceWarnings extends ArgHandlerFlag {

  private final OptionWarnOverlappingSource optionWarnOverlappingSource;

  public ArgHandlerOverlappingSourceWarnings(OptionWarnOverlappingSource option) {
    this.optionWarnOverlappingSource = option;
  }

  @Override
  public boolean getDefaultValue() {
    return false;
  }

  @Override
  public String getLabel() {
    return "overlappingSourceWarnings";
  }

  @Override
  public String getPurposeSnippet() {
    return "Whether to show warnings during monolithic compiles for overlapping source inclusion.";
  }

  @Override
  public boolean setFlag(boolean value) {
    optionWarnOverlappingSource.setWarnOverlappingSource(value);
    return true;
  }
}
