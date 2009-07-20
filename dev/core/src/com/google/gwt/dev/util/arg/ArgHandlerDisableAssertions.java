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
 * Handler for -da arg to disable assertions in compiled code.
 */
public final class ArgHandlerDisableAssertions extends ArgHandlerFlag {

  private final OptionEnableAssertions option;

  public ArgHandlerDisableAssertions(OptionEnableAssertions option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "Debugging: disables checking assertion statements in the compiled output.";
  }

  @Override
  public String getTag() {
    return "-da";
  }

  @Override
  public boolean setFlag() {
    option.setEnableAssertions(false);
    return true;
  }

}
