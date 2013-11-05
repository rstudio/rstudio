/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.arg;

import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.util.tools.ArgHandlerString;

/**
 * An argument handler for specifying the path for the generated precompiled library file.
 */
public class ArgHandlerOutputLibrary extends ArgHandlerString {

  private final OptionOutputLibraryPath option;

  public ArgHandlerOutputLibrary(OptionOutputLibraryPath option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "The path into which the generated .gwtlib library will be written.";
  }

  @Override
  public String getTag() {
    return "-outLibrary";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"library"};
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean setString(String outputLibraryPath) {
    if (Strings.isNullOrEmpty(outputLibraryPath)) {
      return true;
    }
    option.setOutputLibraryPath(outputLibraryPath);
    return true;
  }
}
