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
 * Collects SOYC metrics and output in xml but not html format.
 */
public class ArgHandlerDisableSoycHtml extends ArgHandlerFlag {

  private final OptionSoycHtmlDisabled optionSoycHtmlDisabled;
  private final OptionSoycEnabled optionSoycEnabled;

  public <T extends OptionSoycHtmlDisabled & OptionSoycEnabled> ArgHandlerDisableSoycHtml(
      T options) {
    optionSoycHtmlDisabled = options;
    optionSoycEnabled = options;

    addTagValue("-XdisableSoycHtml", false);
  }

  @Override
  public String getPurposeSnippet() {
    return "Collect SOYC metrics and output in xml but not html format.";
  }

  @Override
  public String getLabel() {
    return "soycHtmlOnly";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag(boolean value) {
    optionSoycHtmlDisabled.setSoycHtmlDisabled(!value);
    optionSoycEnabled.setSoycEnabled(!value);
    return true;
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return !optionSoycHtmlDisabled.isSoycHtmlDisabled() && !optionSoycEnabled.isSoycEnabled();
  }
}
