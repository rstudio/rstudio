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
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * Splits code on runAsync boundaries.
 */
public class ArgHandlerDisableRunAsync extends ArgHandlerFlag {

  private final OptionRunAsyncEnabled option;

  public ArgHandlerDisableRunAsync(OptionRunAsyncEnabled option) {
    this.option = option;

    addTagValue("-XdisableRunAsync", false);
  }

  @Override
  public String getPurposeSnippet() {
    return "Split code on runAsync boundaries.";
  }

  @Override
  public String getLabel() {
    return "codeSplitting";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag(boolean value) {
    option.setRunAsyncEnabled(value);
    return true;
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return option.isRunAsyncEnabled();
  }
}
