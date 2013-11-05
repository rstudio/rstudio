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

import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * An argument handler for providing a list of paths to input precompiled library files.
 */
public class ArgHandlerLibraries extends ArgHandlerString {

  private final OptionLibraryPaths option;

  public ArgHandlerLibraries(OptionLibraryPaths option) {
    this.option = option;
  }

  @Override
  public String getPurpose() {
    return "The path(s) to .gwtlib library file(s).";
  }

  @Override
  public String getTag() {
    return "-libraries";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"library[s]"};
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean setString(String value) {
    List<String> libraryPaths = Lists.newArrayList(
        Splitter.on(File.pathSeparator).omitEmptyStrings().trimResults().split(value));
    libraryPaths.removeAll(Collections.singleton(""));
    if (!libraryPaths.isEmpty()) {
      option.setLibraryPaths(libraryPaths);
    }
    return true;
  }
}
