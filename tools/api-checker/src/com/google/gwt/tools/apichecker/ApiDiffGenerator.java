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

package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * encapsulates a class that produces the diff between two api's.
 */
public class ApiDiffGenerator {

  public static final String DELIMITER = " ";

  @SuppressWarnings("unchecked")
  public static HashSet<String> findCommonElements(HashSet<String> s1,
      HashSet<String> s2, String name) {
    HashSet<String> intersection = (HashSet<String>) s1.clone();
    intersection.retainAll(s2);
    s1.removeAll(intersection);
    s2.removeAll(intersection);
    return intersection;
  }

  /**
   * The two types might belong to different typeOracles.
   */
  public static boolean isFirstTypeAssignableToSecond(JType firstType,
      JType secondType) {

    // getJNISignature() does TypeErasure
    if (firstType.getJNISignature().equals(secondType.getJNISignature())) {
      return true;
    }
    JClassType classType1 = firstType.isClassOrInterface();
    JClassType classType2 = secondType.isClassOrInterface();
    if (classType1 == null || classType2 == null) {
      return false;
    }
    TypeOracle newApiTypeOracle = classType2.getOracle();
    // get the appropriate classObject in the newApi
    JClassType firstClassType = newApiTypeOracle.findType(classType1.getQualifiedSourceName());
    JClassType secondClassType = newApiTypeOracle.findType(classType2.getQualifiedSourceName());
    // The types might not necessarily exist in the newApi
    if (firstClassType == null || secondClassType == null) {
      return false;
    }
    return firstClassType.isAssignableTo(secondClassType);
  }

  HashMap<String, ApiPackageDiffGenerator> intersectingPackages = new HashMap<String, ApiPackageDiffGenerator>();

  HashSet<String> missingPackageNames = null;
  ApiContainer newApi = null;

  ApiContainer oldApi = null;

  ApiDiffGenerator(ApiContainer newApi, ApiContainer oldApi) {
    this.newApi = newApi;
    this.oldApi = oldApi;
  }

  public void cleanApiDiff() {
    Iterator<ApiPackageDiffGenerator> tempIterator = intersectingPackages.values().iterator();
    while (tempIterator.hasNext()) {
      tempIterator.next().cleanApiDiff();
    }
  }

  /**
   * Compares 2 APIs for source compatibility. Algorithm: First find packages
   * that are in one but not in another. Then, look at at classes in the common
   * packages. Look at public classes.
   * 
   */
  public void computeApiDiff() throws NotFoundException {
    HashSet<String> newApiPackageNames = newApi.getApiPackageNames();
    missingPackageNames = oldApi.getApiPackageNames();
    HashSet<String> intersection = findCommonElements(newApiPackageNames,
        missingPackageNames, "ROOT");
    // Inspect each of the classes in each of the packages in the intersection
    Iterator<String> tempIterator = intersection.iterator();
    while (tempIterator.hasNext()) {
      String packageName = tempIterator.next();
      ApiPackageDiffGenerator temp = new ApiPackageDiffGenerator(packageName,
          this);
      intersectingPackages.put(packageName, temp);
      temp.computeApiDiff();
    }
  }

  public ApiClassDiffGenerator findApiClassDiffGenerator(JClassType classType) {
    String className = classType.getQualifiedSourceName();
    int i = className.length() - 1;
    while (i >= 0) {
      int dot = className.lastIndexOf('.', i);
      String pkgName = "";
      String typeName = className;
      if (dot != -1) {
        pkgName = className.substring(0, dot);
        typeName = className.substring(dot + 1);
        i = dot - 1;
      } else {
        i = -1;
      }
      ApiClassDiffGenerator result = findApiClassDiffGenerator(pkgName,
          typeName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  // TODO(amitmanjhi): cache the results
  /**
   * Finds a type given its package-relative name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   * 
   * @return <code>null</code> if the type is not found
   */
  public ApiClassDiffGenerator findApiClassDiffGenerator(String pkgName,
      String typeName) {
    ApiPackageDiffGenerator pkg = findApiPackageDiffGenerator(pkgName);
    if (pkg != null) {
      ApiClassDiffGenerator type = pkg.findApiClassDiffGenerator(pkgName + "."
          + typeName);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  public ApiPackageDiffGenerator findApiPackageDiffGenerator(String key) {
    return intersectingPackages.get(key);
  }

  public ApiContainer getNewApiContainer() {
    return newApi;
  }

  public ApiContainer getOldApiContainer() {
    return oldApi;
  }

  public String printApiDiff() {
    StringBuffer sb = new StringBuffer();
    Iterator<String> missingPackagesIterator = missingPackageNames.iterator();
    while (missingPackagesIterator.hasNext()) {
      sb.append(missingPackagesIterator.next() + DELIMITER
          + ApiChange.Status.MISSING + "\n");
    }
    Iterator<ApiPackageDiffGenerator> tempIterator = intersectingPackages.values().iterator();
    while (tempIterator.hasNext()) {
      sb.append(tempIterator.next().printApiDiff());
    }
    return sb.toString();
  }

}
