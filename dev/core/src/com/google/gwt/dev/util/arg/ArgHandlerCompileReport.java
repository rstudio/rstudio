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
 * Enables Story Of Your Compile data-collection.
 */
public class ArgHandlerCompileReport extends ArgHandlerFlag {

  private final OptionSoycEnabled options;

  public ArgHandlerCompileReport(OptionSoycEnabled options) {
    this.options = options;
  }

  @Override
  public String getPurposeSnippet() {
    return "Compile a report that tells the \"Story of Your Compile\".";
  }

  @Override
  public String getLabel() {
    return "compileReport";
  }

  @Override
  public boolean setFlag(boolean value) {
    options.setSoycEnabled(value);
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return options.isSoycEnabled();
  }
}
