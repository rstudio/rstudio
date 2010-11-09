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

import com.google.gwt.util.tools.ArgHandlerInt;

/**
 * Argument handler for specifying the which permutation to run.
 */
public class ArgHandlerPerm extends ArgHandlerInt {
  private final OptionPerm option;

  public ArgHandlerPerm(OptionPerm option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "Specifies (0-based) the permutation to compile";
  }

  @Override
  public String getTag() {
    return "-perm";
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{"perm"};
  }

  @Override
  public void setInt(int value) {
    // TODO Auto-generated method stub
    if (value < 0) {
      System.err.println(getTag() + " error: negative value '" + value
          + "' is not allowed");
    } else {
      option.setPermToCompile(value);
    }
  }
}
