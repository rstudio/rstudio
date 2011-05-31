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
 * An ArgHandler that enables detailed Story Of Your Compile data collection,
 * but disables HTML report generation, leaving only XML output.
 *
 */
public class ArgHandlerDisableSoycHtml extends ArgHandlerFlag {
  private final OptionSoycHtmlDisabled options;

  public ArgHandlerDisableSoycHtml(OptionSoycHtmlDisabled options) {
    this.options = options;
  }

  @Override
  public String getPurpose() {
    return "Enable SOYC reporting without HTML report generation.";
  }

  @Override
  public String getTag() {
    return "-XdisableSoycHtml";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag() {
    options.setSoycHtmlDisabled(true);
    options.setSoycEnabled(true);
    return true;
  }
}
