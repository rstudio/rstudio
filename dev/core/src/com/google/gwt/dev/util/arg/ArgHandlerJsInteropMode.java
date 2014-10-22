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
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.util.tools.ArgHandlerString;

/**
 * Set the JsInterop mode.
 */
public class ArgHandlerJsInteropMode extends ArgHandlerString {
  private final OptionJsInteropMode options;

  public ArgHandlerJsInteropMode(OptionJsInteropMode options) {
    this.options = options;
  }

  @Override
  public String getPurpose() {
    return "Specifies JsInterop mode, either NONE, JS, or CLOSURE (defaults to NONE)";
  }

  @Override
  public String getTag() {
    return "-XjsInteropMode";
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{"[" + Joiner.on(", ").skipNulls().join(
        Mode.values()) + "]"};
  }

  @Override
  public boolean setString(String value) {
    Mode mode = null;
    try {
      mode = Mode.valueOf(value.trim().toUpperCase());
    } catch (Exception e) {
      System.err.println("JsInteropMode " + value + " not recognized");
    }

    if (mode == null) {
      System.err.println("JsInteropMode must be one of [" +
          Joiner.on(", ").skipNulls().join(Mode.values()) + "].");
      return false;
    }
    options.setJsInteropMode(mode);
    return true;
  }
}
