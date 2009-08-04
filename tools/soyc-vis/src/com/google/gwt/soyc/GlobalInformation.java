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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Information global to the entire SOYC report generator.
 */
public class GlobalInformation {
  private static final SizeBreakdown[] EMPTY_SIZE_BREAKDOWN = new SizeBreakdown[0];

  public static Boolean displayDependencies = false;
  public static Boolean displaySplitPoints = false;

  public static HashMap<String, String> classToPackage = new HashMap<String, String>();
  public static Map<String, TreeSet<String>> packageToClasses = new TreeMap<String, TreeSet<String>>();

  public static HashMap<String, HashSet<String>> classToWhatItDependsOn = new HashMap<String, HashSet<String>>();

  public static int numSplitPoints = 0;

  public static HashMap<Integer, String> splitPointToLocation = new HashMap<Integer, String>();
  public static ArrayList<Integer> splitPointInitialLoadSequence = new ArrayList<Integer>();

  public static Settings settings = new Settings();

  public static SizeBreakdown initialCodeBreakdown = new SizeBreakdown(
      "Initially downloaded code", "initial");
  public static SizeBreakdown leftoversBreakdown = new SizeBreakdown(
      "Leftovers code, code not in any other category", "leftovers");
  public static SizeBreakdown totalCodeBreakdown = new SizeBreakdown(
      "Total program", "total");
  private static Map<Integer, SizeBreakdown> exclusiveCodeBreakdowns = new HashMap<Integer, SizeBreakdown>();

  public static SizeBreakdown[] allSizeBreakdowns() {
    List<SizeBreakdown> breakdowns = new ArrayList<SizeBreakdown>();
    breakdowns.add(totalCodeBreakdown);
    breakdowns.add(initialCodeBreakdown);
    if (numSplitPoints > 0) {
      breakdowns.add(leftoversBreakdown);
      for (int sp = 1; sp <= numSplitPoints; sp++) {
        breakdowns.add(splitPointCodeBreakdown(sp));
      }
    }
    return breakdowns.toArray(EMPTY_SIZE_BREAKDOWN);
  }

  public static void computePackageSizes() {
    for (SizeBreakdown breakdown : allSizeBreakdowns()) {
      computePackageSizes(breakdown.packageToSize, breakdown.classToSize);
    }
  }

  public static SizeBreakdown splitPointCodeBreakdown(int sp) {
    assert sp >= 1 && sp <= numSplitPoints;
    if (!exclusiveCodeBreakdowns.containsKey(sp)) {
      exclusiveCodeBreakdowns.put(sp, new SizeBreakdown("split point " + sp
          + ": " + splitPointToLocation.get(sp), "sp" + sp));
    }
    return exclusiveCodeBreakdowns.get(sp);
  }

  /**
   * TODO(spoon) move this to the SizeBreakdown class.
   */
  private static void computePackageSizes(Map<String, Integer> packageToSize,
      Map<String, Integer> classToSize) {
    packageToSize.clear();
    for (String packageName : packageToClasses.keySet()) {
      packageToSize.put(packageName, 0);
      for (String className : packageToClasses.get(packageName)) {
        if (classToSize.containsKey(className)) {
          int curSize = classToSize.get(className);
          int newSize = curSize + packageToSize.get(packageName);
          packageToSize.put(packageName, newSize);
        }
      }
    }
  }
}
