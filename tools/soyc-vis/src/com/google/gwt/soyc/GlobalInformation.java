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
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Information global to the entire SOYC report generator.
 */
public class GlobalInformation {

  public static String backupLocation = "(Source location not known)";
  
  public static Boolean displayDependencies = false;
  public static Boolean displaySplitPoints = false;

  public static HashMap<String, String> classToPackage = new HashMap<String, String>();
  public static HashMap<String, HashSet<String>> classToWhatItDependsOn = new HashMap<String, HashSet<String>>();
  public static HashMap<Integer, Float> fragmentToPartialSize = new HashMap<Integer, Float>();
  public static HashMap<Integer, HashSet<String>> fragmentToStories = new HashMap<Integer, HashSet<String>>();

  public static int numBytesDoubleCounted = 0;
  public static int numFragments = 0;
  public static int numSplitPoints = 0;
  public static Map<String, TreeSet<String>> packageToClasses = new TreeMap<String, TreeSet<String>>();

  public static HashMap<Integer, String> splitPointToLocation = new HashMap<Integer, String>();
  public static HashMap<String, HashSet<String>> storiesToCorrClasses = new HashMap<String, HashSet<String>>();
  public static HashMap<String, HashSet<String>> storiesToCorrClassesAndMethods = new HashMap<String, HashSet<String>>();

  public static HashMap<String, String> storiesToLitType = new HashMap<String, String>();

  public static Settings settings = new Settings();

  public static SizeBreakdown initialCodeBreakdown = new SizeBreakdown(
      "Initially downloaded code", "initial");
  public static SizeBreakdown totalCodeBreakdown = new SizeBreakdown(
      "Total program", "total");

  public static SizeBreakdown[] allSizeBreakdowns() {
    return new SizeBreakdown[] {totalCodeBreakdown, initialCodeBreakdown};
  }

  public static void computePackageSizes() {
    for (SizeBreakdown breakdown : allSizeBreakdowns()) {
      computePackageSizes(breakdown.packageToSize, breakdown.classToSize);
    }
  }

  public static void computePartialPackageSizes() {
    for (SizeBreakdown breakdown : allSizeBreakdowns()) {
      computePartialPackageSizes(breakdown.packageToPartialSize, breakdown.classToPartialSize);
    }
  }

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

  private static void computePartialPackageSizes(
      Map<String, Float> packageToPartialSize,
      Map<String, Float> classToPartialSize) {
    float cumPartialSizeFromPackages = 0f;

    packageToPartialSize.clear();
    for (String packageName : packageToClasses.keySet()) {
      packageToPartialSize.put(packageName, 0f);
      for (String className : packageToClasses.get(packageName)) {
        if (classToPartialSize.containsKey(className)) {
          float curSize = classToPartialSize.get(className);
          cumPartialSizeFromPackages += curSize;
          float newSize = curSize + packageToPartialSize.get(packageName);
          packageToPartialSize.put(packageName, newSize);
        }
      }
    }
  }
}
