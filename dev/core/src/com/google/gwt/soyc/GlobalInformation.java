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
 * Compile Report information about a compiled module.
 */
public class GlobalInformation {
  private static final SizeBreakdown[] EMPTY_SIZE_BREAKDOWN = new SizeBreakdown[0];
  public Map<String, Map<String, String>> dependencies = null;
  private Map<String, String> classToPackage = new TreeMap<String, String>();
  private HashMap<String, HashSet<String>> classToWhatItDependsOn = new HashMap<String, HashSet<String>>();
  private Map<Integer, SizeBreakdown> exclusiveCodeBreakdowns = new HashMap<Integer, SizeBreakdown>();
  private SizeBreakdown initialCodeBreakdown = new SizeBreakdown(
      "Initially downloaded code", "initial");
  private SizeBreakdown leftoversBreakdown = new SizeBreakdown(
      "Leftovers code, code not in any other split point", "leftovers");
  private int numSplitPoints = 0;
  private Map<String, TreeSet<String>> packageToClasses = new TreeMap<String, TreeSet<String>>();
  private final String permutationId;
  private ArrayList<Integer> splitPointInitialLoadSequence = new ArrayList<Integer>();
  private HashMap<Integer, String> splitPointToLocation = new HashMap<Integer, String>();
  private SizeBreakdown totalCodeBreakdown = new SizeBreakdown("Total program",
      "total");

  public GlobalInformation(String permutationId) {
    this.permutationId = permutationId;
  }

  public SizeBreakdown[] allSizeBreakdowns() {
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

  /**
   * Computes all package sizes.
   */
  public void computePackageSizes() {
    for (SizeBreakdown breakdown : allSizeBreakdowns()) {
      computePackageSizes(breakdown.packageToSize, breakdown.classToSize);
    }
  }

  /**
   * Gets the mapping from each class to its package.
   * 
   * @return classToPackage
   */
  public final Map<String, String> getClassToPackage() {
    return classToPackage;
  }

  /**
   * Gets the mapping from a class to what it depends on.
   * 
   * @return classToWhatItDependsOn
   */
  public final HashMap<String, HashSet<String>> getClassToWhatItDependsOn() {
    return classToWhatItDependsOn;
  }

  /**
   * Gets the exclusive code breakdown.
   * 
   * @return exclusiveCodeBreakdown
   */
  public final Map<Integer, SizeBreakdown> getExclusiveCodeBreakdowns() {
    return exclusiveCodeBreakdowns;
  }

  /**
   * Gets the initial code breakdown.
   * 
   * @return initialCodeBreakdown
   */
  public final SizeBreakdown getInitialCodeBreakdown() {
    return initialCodeBreakdown;
  }

  /**
   * Gets the leftovers code breakdown.
   * 
   * @return leftoversCodeBreakdown
   */
  public final SizeBreakdown getLeftoversBreakdown() {
    return leftoversBreakdown;
  }

  /**
   * Gets the number of split points.
   * 
   * @return numSplitPoints
   */
  public final int getNumSplitPoints() {
    return numSplitPoints;
  }

  /**
   * Gets the mapping from packages to classes.
   * 
   * @return packageToClasses
   */
  public final Map<String, TreeSet<String>> getPackageToClasses() {
    return packageToClasses;
  }

  public String getPermutationId() {
    return permutationId;
  }

  /**
   * Gets the initial load sequence.
   * 
   * @return splitPointInitialLoadSequence
   */
  public final ArrayList<Integer> getSplitPointInitialLoadSequence() {
    return splitPointInitialLoadSequence;
  }

  /**
   * Gets the mapping from split points to locations where they were set.
   * 
   * @return splitPointToLocation
   */
  public final HashMap<Integer, String> getSplitPointToLocation() {
    return splitPointToLocation;
  }

  /**
   * Gets the total code breakdown.
   * 
   * @return totalCodeBreakdown
   */
  public final SizeBreakdown getTotalCodeBreakdown() {
    return totalCodeBreakdown;
  }

  /**
   * Increments the split point count.
   */
  public final void incrementSplitPoints() {
    numSplitPoints++;
  }

  /**
   * Gets an exclusive code breakdown for a split point.
   * 
   * @param sp split point
   * @return exlusive code breakdown for sp
   */
  public SizeBreakdown splitPointCodeBreakdown(int sp) {
    assert sp >= 1 && sp <= numSplitPoints;
    if (!exclusiveCodeBreakdowns.containsKey(sp)) {
      exclusiveCodeBreakdowns.put(sp, new SizeBreakdown("split point " + sp
          + ": " + splitPointToLocation.get(sp), "sp" + sp));
    }
    return exclusiveCodeBreakdowns.get(sp);
  }

  /**
   * Computes package sizes from class sizes. TODO(spoon) move this to the
   * SizeBreakdown class.
   * 
   * @param packageToSize mapping from packages to their sizes
   * @param classToSize mapping from classes to their sizes
   */
  private void computePackageSizes(Map<String, Integer> packageToSize,
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
