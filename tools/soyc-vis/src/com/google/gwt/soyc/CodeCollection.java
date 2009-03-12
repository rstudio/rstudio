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

public class CodeCollection {

  public String codeType = "";
  public TreeSet<String> classes = new TreeSet<String>();
  public TreeSet<String> stories = new TreeSet<String>();
  public float cumPartialSize = 0f;
  public int cumSize = 0;

  public CodeCollection(String type) {
    codeType = type;
  }

  public int getCumSize() {
    cumSize = 0;
    for (String className : classes) {
      if (!GlobalInformation.classToSize.containsKey(className)) {
        System.err.println("*** NO SIZE FOUND FOR CLASS " + className
            + " *****");
      } else {
        cumSize += GlobalInformation.classToSize.get(className);
      }
    }
    return cumSize;
  }

  public float getCumPartialSize() {
    cumPartialSize = 0f;
    for (String className : classes) {
      if (!GlobalInformation.classToPartialSize.containsKey(className)) {
        System.err.println("*** NO PARTIAL SIZE FOUND FOR CLASS " + className
            + " *****");
      } else {
        cumPartialSize += GlobalInformation.classToPartialSize.get(className);
      }
    }
    return cumPartialSize;
  }
}
