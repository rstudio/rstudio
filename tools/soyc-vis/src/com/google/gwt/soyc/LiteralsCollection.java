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

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * A collection of literals.
 */
public class LiteralsCollection {
  public int cumSize = 0;
  public int cumStringSize = 0;
  public String literalType = "";
  public Map<String, HashSet<String>> literalToLocations = new TreeMap<String, HashSet<String>>();
  public Map<String, HashSet<String>> storyToLocations = new HashMap<String, HashSet<String>>();
  public TreeMap<String, String> stringLiteralToType = new TreeMap<String, String>();
  public Map<String, Integer> stringTypeToSize = new HashMap<String, Integer>();
  public Map<String, Integer> stringTypeToCount = new HashMap<String, Integer>();

  public LiteralsCollection(String type) {
    literalType = type;
  }

  /**
   * Utility method.
   */
  public void printAllStrings() {
    int iSum = 0;
    System.out.println("--- now printing strings ---");
    for (String st : stringLiteralToType.keySet()) {
      int numBytes = st.getBytes().length;
      iSum += numBytes;
      System.out.println(st + "[" + numBytes + "]");
    }
    System.out.println("sum: " + iSum);
    System.out.println("--- done printing strings ---");
  }

}
