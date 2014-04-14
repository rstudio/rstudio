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
 * Includes assert statements in compiled output.
 */
public final class ArgHandlerEnableAssertions extends ArgHandlerFlag {

  private final OptionEnableAssertions option;

  public ArgHandlerEnableAssertions(OptionEnableAssertions option) {
    this.option = option;

    addTagValue("-ea", true);
  }

  @Override
  public String getPurposeSnippet() {
    return "Include assert statements in compiled output.";
  }

  @Override
  public String getLabel() {
    return "checkAssertions";
  }

  @Override
  public boolean setFlag(boolean value) {
    option.setEnableAssertions(value);
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return option.isEnableAssertions();
  }
}
