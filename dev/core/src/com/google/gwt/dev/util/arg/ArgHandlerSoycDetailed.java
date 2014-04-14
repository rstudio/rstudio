/*
 * Copyright 2009 Google Inc.
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
 * Emits extra, detailed compile-report information in the "Story Of Your Compile".
 */
public class ArgHandlerSoycDetailed extends ArgHandlerFlag {

  private final OptionSoycDetailed optionSoycDetailed;
  private final OptionSoycEnabled optionSoycEnabled;

  public <T extends OptionSoycDetailed & OptionSoycEnabled> ArgHandlerSoycDetailed(
      T options) {
    optionSoycDetailed = options;
    optionSoycEnabled = options;

    addTagValue("-XsoycDetailed", true);
  }

  @Override
  public String getPurposeSnippet() {
    return "Emit extra, detailed compile-report information in the \"Story Of Your Compile\" "
        + "at the expense of compile time.";
  }

  @Override
  public String getLabel() {
    return "detailedSoyc";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag(boolean value) {
    optionSoycDetailed.setSoycExtra(value);
    optionSoycEnabled.setSoycEnabled(value);
    return true;
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return optionSoycDetailed.isSoycExtra() && optionSoycEnabled.isSoycEnabled();
  }
}
