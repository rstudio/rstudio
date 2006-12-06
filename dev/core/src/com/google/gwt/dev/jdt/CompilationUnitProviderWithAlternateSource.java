/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

public class CompilationUnitProviderWithAlternateSource implements
    CompilationUnitProvider {
  public CompilationUnitProviderWithAlternateSource(
      CompilationUnitProvider cup, char[] source) {
    this.cup = cup;
    this.source = source;
  }

  public long getLastModified() throws UnableToCompleteException {
    return cup.getLastModified();
  }

  public String getLocation() {
    return cup.getLocation();
  }

  public String getPackageName() {
    return cup.getPackageName();
  }

  public char[] getSource() throws UnableToCompleteException {
    return source;
  }

  public boolean isTransient() {
    return cup.isTransient();
  }

  private final CompilationUnitProvider cup;
  private final char[] source;
}
