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
package com.google.gwt.core.ext.typeinfo;

import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.Comparator;

/**
 * Provides information about a single compilation unit on demand.
 */
public interface CompilationUnitProvider {

  long getLastModified() throws UnableToCompleteException;

  boolean isTransient();
  
  String getLocation();

  String getPackageName();

  char[] getSource() throws UnableToCompleteException;

  Comparator LOCATION_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      String loc1 = ((CompilationUnitProvider) o1).getLocation();
      String loc2 = ((CompilationUnitProvider) o2).getLocation();
      return loc1.compareTo(loc2);
    }
  };
}
