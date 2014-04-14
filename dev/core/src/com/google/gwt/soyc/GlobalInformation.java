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
      "Leftovers code, code not in any other fragment", "leftovers");
  private Map<String, TreeSet<String>> packageToClasses = new TreeMap<String, TreeSet<String>>();
  private final String permutationId;
  private ArrayList<Integer> initialFragmentLoadSequence = new ArrayList<Integer>();
  private HashMap<Integer, List<String>> fragmentDescriptors =
      new HashMap<Integer, List<String>>();
  private SizeBreakdown totalCodeBreakdown = new SizeBreakdown("Total program",
      "total");

  public GlobalInformation(String permutationId) {
    this.permutationId = permutationId;
  }

  public SizeBreakdown[] allSizeBreakdowns() {
    List<SizeBreakdown> breakdowns = new ArrayList<SizeBreakdown>();
    breakdowns.add(totalCodeBreakdown);
    breakdowns.add(initialCodeBreakdown);
    if (getNumFragments() > 0) {
      breakdowns.add(leftoversBreakdown);
      for (int fragment = 1; fragment <= getNumFragments(); fragment++) {
        breakdowns.add(fragmentCodeBreakdown(fragment));
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
   * Gets the initial fragment size breakdown.
   *
   * @return initialCodeBreakdown
   */
  public final SizeBreakdown getInitialCodeBreakdown() {
    return initialCodeBreakdown;
  }

  /**
   * Gets the leftovers fragment size breakdown.
   *
   * @return leftoversCodeBreakdown
   */
  public final SizeBreakdown getLeftoversBreakdown() {
    return leftoversBreakdown;
  }

  /**
   * Gets the number of fragments..
   *
   * @return the number of fragments.
   */
  public final int getNumFragments() {
    return fragmentDescriptors.size();
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
   * @return initialFragmentLoadSequence
   */
  public final ArrayList<Integer> getInitialFragmentLoadSequence() {
    return initialFragmentLoadSequence;
  }

  /**
   * Adds a descriptor (a split point) to a fragment.
   *
   * @param fragment the fragment number.
   * @param desc a string describing a split point for fragment <code>fragment</code>
   *
   */
  public final void addFragmentDescriptor(int fragment, String desc) {
    List<String> descriptions = fragmentDescriptors.get(fragment);
    if (descriptions == null) {
      descriptions = new ArrayList<String>();
      fragmentDescriptors.put(fragment, descriptions);
    }
    descriptions.add(desc);
  }

  /**
   * Gets the descriptors associated with a
   * fragment.
   *
   * @param fragment the fragment number
   * @return a list of descriptors (each representing a single split point.
   */
  public final List<String> getFragmentDescriptors(int fragment) {
    return fragmentDescriptors.get(fragment);
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
   * Gets an exclusive code breakdown for a fragment.
   *
   * @param fragment the fragment id
   * @return exlusive code breakdown for fragment
   */
  public SizeBreakdown fragmentCodeBreakdown(int fragment) {
    assert fragment >= 1 && fragment <= getNumFragments();
    if (!exclusiveCodeBreakdowns.containsKey(fragment)) {
      exclusiveCodeBreakdowns.put(fragment, new SizeBreakdown("split point " + fragment
          + ": " + fragmentDescriptors.get(fragment), "fragment" + fragment));
    }
    return exclusiveCodeBreakdowns.get(fragment);
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
