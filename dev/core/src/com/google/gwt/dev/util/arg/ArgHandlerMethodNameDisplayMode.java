/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode.Mode;
import com.google.gwt.util.tools.ArgHandlerEnum;

/**
 * Argument handler set setting the display name setting in the compiler.
 */
public class ArgHandlerMethodNameDisplayMode extends ArgHandlerEnum<Mode> {

  private final OptionMethodNameDisplayMode option;

  public ArgHandlerMethodNameDisplayMode(OptionMethodNameDisplayMode option) {
    super(Mode.class, option.getMethodNameDisplayMode(), false);
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return getPurposeString("Specifies method display name mode for chrome devtools:");
  }

  @Override
  public String getTag() {
    return "-XmethodNameDisplayMode";
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  public void setValue(Mode value) {
    if (option.getMethodNameDisplayMode() != value) {
      option.setMethodNameDisplayMode(value);
    }
  }
}
