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
import java.util.TreeMap;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Information global to the entire SOYC report generator.
 */
public class GlobalInformation {

  public static Boolean displayDependencies = false;
  public static Boolean displaySplitPoints = false;

  public static int allOtherFragmentsPartialSize = 0;
  public static HashMap<String, String> classToPackage = new HashMap<String, String>();
  public static HashMap<String, Float> classToPartialSize = new HashMap<String, Float>();
  public static HashMap<String, Integer> classToSize = new HashMap<String, Integer>();
  public static HashMap<String, HashSet<String>> classToWhatItDependsOn = new HashMap<String, HashSet<String>>();
  public static float cumPartialSizeFromPackages = 0f;
  public static int cumSizeAllCode = 0;
  public static int cumSizeFromPackages = 0;
  public static int cumSizeInitialFragment = 0;
  // collect only size for exclusive fragments 
  public static HashMap<Integer, Float> fragmentToPartialSize = new HashMap<Integer, Float>(); 
  public static HashMap<Integer, HashSet<String>> fragmentToStories = new HashMap<Integer, HashSet<String>>();
  public static TreeMap<String, LiteralsCollection> nameToLitColl = new TreeMap<String, LiteralsCollection>();
  public static HashMap<String, CodeCollection> nameToCodeColl = new HashMap<String, CodeCollection>();

  public static int nonAttributedBytes = 0;
  public static HashSet<String> nonAttributedStories = new HashSet<String>();
  public static int numBytesDoubleCounted = 0;
  public static int numFragments = 0;
  public static int numSplitPoints = 0;
  public static TreeMap<String, TreeSet<String>> packageToClasses = new TreeMap<String, TreeSet<String>>();
  public static HashMap<String, Float> packageToPartialSize = new HashMap<String, Float>();
  public static HashMap<String, Integer> packageToSize = new HashMap<String, Integer>();
  public static HashMap<Integer, String> splitPointToLocation = new HashMap<Integer, String>();
  public static HashMap<String, HashSet<String>> storiesToCorrClasses = new HashMap<String, HashSet<String>>();
  public static HashMap<String, HashSet<String>> storiesToCorrClassesAndMethods = new HashMap<String, HashSet<String>>();

  public static HashMap<String, String> storiesToLitType = new HashMap<String, String>();

  public static Settings settings = new Settings();

  public static void computePackageSizes() {
    cumSizeFromPackages = 0;
    packageToSize.clear();
    for (String packageName : packageToClasses.keySet()) {
      packageToSize.put(packageName, 0);
      for (String className : packageToClasses.get(packageName)) {
        if (!classToSize.containsKey(className)) {
          System.err.println("*** NO  SIZE FOUND FOR CLASS " + className
              + " *****");
        } else {
          int curSize = classToSize.get(className);
          cumSizeFromPackages += curSize;
          int newSize = curSize + packageToSize.get(packageName);
          packageToSize.put(packageName, newSize);
        }
      }
    }
  }

  public static void computePartialPackageSizes() {
    cumPartialSizeFromPackages = 0;
    packageToPartialSize.clear();
    for (String packageName : packageToClasses.keySet()) {
      packageToPartialSize.put(packageName, 0f);
      for (String className : packageToClasses.get(packageName)) {
        if (!classToPartialSize.containsKey(className)) {
          System.err.println("*** NO PARTIAL SIZE FOUND FOR CLASS " + className
              + " *****");
        } else {
          float curSize = classToPartialSize.get(className);
          cumPartialSizeFromPackages += curSize;
          float newSize = curSize + packageToPartialSize.get(packageName);
          packageToPartialSize.put(packageName, newSize);
        }
      }
    }
  }

}
