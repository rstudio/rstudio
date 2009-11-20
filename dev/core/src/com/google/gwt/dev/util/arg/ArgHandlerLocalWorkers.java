/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.util.tools.ArgHandlerInt;

/**
 * An arg handler to specify the number of local workers that should be used by
 * the compiler.
 */
public class ArgHandlerLocalWorkers extends ArgHandlerInt {

  private final OptionLocalWorkers options;

  public ArgHandlerLocalWorkers(OptionLocalWorkers options) {
    this.options = options;
  }

  @Override
  public String[] getDefaultArgs() {
    // Default to 1 for now; we might do an "auto" later.
    return new String[] {getTag(), "1"};
  }

  @Override
  public String getPurpose() {
    return "The number of local workers to use when compiling permutations";
  }

  @Override
  public String getTag() {
    return "-localWorkers";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"count"};
  }

  @Override
  public void setInt(int value) {
    options.setLocalWorkers(value);
  }
}
