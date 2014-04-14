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

package com.google.gwt.soyc;

import java.util.TreeSet;

/**
 * Holds a set of all classes in a Compile Report, and can calculate size
 * summaries for them.
 */
public class CodeCollection {

  public TreeSet<String> classes = new TreeSet<String>();
  public int cumSize = 0;
  public TreeSet<String> stories = new TreeSet<String>();

  public int getCumSize(SizeBreakdown breakdown) {
    cumSize = 0;
    for (String className : classes) {
      if (breakdown.classToSize.containsKey(className)) {
        cumSize += breakdown.classToSize.get(className);
      }
    }
    return cumSize;
  }
}
