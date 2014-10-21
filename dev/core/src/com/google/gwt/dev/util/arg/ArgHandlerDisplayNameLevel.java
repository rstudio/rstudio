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

import com.google.gwt.util.tools.ArgHandlerString;

/**
 * Argument handler set setting the display name setting in the compiler.
 */
public class ArgHandlerDisplayNameLevel extends ArgHandlerString {

  private final OptionDisplayNameLevel option;

  public ArgHandlerDisplayNameLevel(OptionDisplayNameLevel option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "Emit extra information allow chrome dev tools to display Java identifiers in many" +
        " places instead of JavaScript functions.";
  }

  @Override
  public String getTag() {
    return "-XdisplayNameMode";
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean setString(String str) {
    String value = str.toUpperCase();
    if ("NONE".equals(value)) {
      option.setDisplayNameMode(DisplayNameMode.NONE);
    } else if ("ONLY_METHOD_NAME".equals(value)) {
      option.setDisplayNameMode(DisplayNameMode.ONLY_METHOD_NAME);
    } else if ("ABBREVIATED".equals(value)) {
      option.setDisplayNameMode(DisplayNameMode.ABBREVIATED);
    } else if ("FULL".equals(value)) {
      option.setDisplayNameMode(DisplayNameMode.FULL);
    } else {
      return false;
    }
    return true;
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"NONE | ONLY_METHOD_NAME | ABBREVIATED | FULL"};
  }
}
