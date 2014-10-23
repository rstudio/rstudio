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

import com.google.gwt.dev.util.arg.OptionJsInteropMode.Mode;
import com.google.gwt.util.tools.ArgHandlerEnum;

/**
 * Set the JsInterop mode.
 */
public class ArgHandlerJsInteropMode extends ArgHandlerEnum<Mode> {
  private final OptionJsInteropMode options;

  public ArgHandlerJsInteropMode(OptionJsInteropMode options) {
    super(Mode.class, options.getJsInteropMode(), false);
    this.options = options;
  }

  @Override
  public String getPurpose() {
    return getPurposeString("Specifies JsInterop mode:");
  }

  @Override
  public String getTag() {
    return "-XjsInteropMode";
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public void setValue(Mode value) {
    if (options.getJsInteropMode() != value) {
      options.setJsInteropMode(value);
    }
  }
}
