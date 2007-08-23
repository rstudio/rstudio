/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.Comparator;

/**
 * Provides information about a single compilation unit on demand.
 */
public interface CompilationUnitProvider {

  Comparator<CompilationUnitProvider> LOCATION_COMPARATOR = new Comparator<CompilationUnitProvider>() {
    public int compare(CompilationUnitProvider cups1,
        CompilationUnitProvider cups2) {
      String loc1 = cups1.getLocation();
      String loc2 = cups2.getLocation();
      return loc1.compareTo(loc2);
    }
  };

  long getLastModified() throws UnableToCompleteException;

  String getLocation();

  String getPackageName();

  char[] getSource() throws UnableToCompleteException;

  boolean isTransient();
}
