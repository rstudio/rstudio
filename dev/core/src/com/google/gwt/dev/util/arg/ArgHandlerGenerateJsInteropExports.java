/*
 * Copyright 2013 Google Inc.
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
 * Enable the generation of JsInterop exports.
 */
public class ArgHandlerGenerateJsInteropExports extends ArgHandlerFlag {
  private final OptionGenerateJsInteropExports options;

  public ArgHandlerGenerateJsInteropExports(OptionGenerateJsInteropExports options) {
    this.options = options;
  }
  @Override
  public String getPurposeSnippet() {
    return "Generate exports for JsInterop purposes."
        + " If no -includeJsInteropExport/-excludeJsInteropExport provided, generates all exports.";
  }

  @Override
  public String getLabel() {
    return "generateJsInteropExports";
  }

  @Override
  public boolean setFlag(boolean value) {
    options.setGenerateJsInteropExports(value);
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return options.shouldGenerateJsInteropExports();
  }
}
