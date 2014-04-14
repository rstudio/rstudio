/*
 * Copyright 2010 Google Inc.
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
 * Enables Compiler Metrics.
 */
public class ArgHandlerCompilerMetrics extends ArgHandlerFlag {

  private final OptionCompilerMetricsEnabled options;

  public ArgHandlerCompilerMetrics(OptionCompilerMetricsEnabled options) {
    this.options = options;
  }

  @Override
  public String getPurposeSnippet() {
    return "Gather compiler metrics.";
  }

  @Override
  public String getLabel() {
    return "compilerMetrics";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag(boolean value) {
    options.setCompilerMetricsEnabled(value);
    return true;
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return options.isCompilerMetricsEnabled();
  }
}
