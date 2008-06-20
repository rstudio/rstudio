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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link ApiDiffGenerator} encapsulates a class that produces the diff between
 * two api's.
 */
public final class ApiDiffGenerator {

  public static final String DELIMITER = " ";

  /**
   * The two types might belong to different typeOracles.
   */
  static boolean isFirstTypeAssignableToSecond(JType firstType, JType secondType) {

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
    JClassType firstClassType =
        newApiTypeOracle.findType(classType1.getQualifiedSourceName());
    JClassType secondClassType =
        newApiTypeOracle.findType(classType2.getQualifiedSourceName());
    // The types might not necessarily exist in the newApi
    if (firstClassType == null || secondClassType == null) {
      return false;
    }
    return firstClassType.isAssignableTo(secondClassType);
  }

  @SuppressWarnings("unchecked")
  static Set<String> removeIntersection(Set<String> s1, Set<String> s2) {
    Set<String> intersection = new HashSet<String>(s1);
    intersection.retainAll(s2);
    s1.removeAll(intersection);
    s2.removeAll(intersection);
    return intersection;
  }

  Map<String, ApiPackageDiffGenerator> intersectingPackages =
      new HashMap<String, ApiPackageDiffGenerator>();
  Set<String> missingPackageNames;
  final ApiContainer newApi;
  final ApiContainer oldApi;

  ApiDiffGenerator(ApiContainer newApi, ApiContainer oldApi) {
    this.newApi = newApi;
    this.oldApi = oldApi;
  }

  Collection<ApiChange> getApiDiff(boolean removeDuplicates)
      throws NotFoundException {
    computeApiDiff();
    if (removeDuplicates) {
      cleanApiDiff();
    }
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    Set<ApiPackage> missingPackages =
        oldApi.getApiPackagesBySet(missingPackageNames);
    for (ApiPackage missingPackage : missingPackages) {
      collection.add(new ApiChange(missingPackage, ApiChange.Status.MISSING));
    }
    for (ApiPackageDiffGenerator intersectingPackage : intersectingPackages.values()) {
      collection.addAll(intersectingPackage.getApiDiff());
    }
    return collection;
  }

  ApiClassDiffGenerator findApiClassDiffGenerator(String className) {
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
      ApiClassDiffGenerator result =
          findApiClassDiffGenerator(pkgName, typeName);
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
  ApiClassDiffGenerator findApiClassDiffGenerator(String pkgName,
      String typeName) {
    ApiPackageDiffGenerator pkg = findApiPackageDiffGenerator(pkgName);
    if (pkg != null) {
      ApiClassDiffGenerator type =
          pkg.findApiClassDiffGenerator(pkgName + "." + typeName);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  ApiPackageDiffGenerator findApiPackageDiffGenerator(String key) {
    return intersectingPackages.get(key);
  }

  ApiContainer getNewApiContainer() {
    return newApi;
  }

  ApiContainer getOldApiContainer() {
    return oldApi;
  }

  private void cleanApiDiff() {
    for (ApiPackageDiffGenerator intersectingPackage : intersectingPackages.values()) {
      intersectingPackage.cleanApiDiff();
    }
  }

  /**
   * Compares 2 APIs for source compatibility. Algorithm: First find packages
   * that are in one but not in another. Then, look at at classes in the common
   * packages. Look at public classes.
   * 
   */
  private void computeApiDiff() throws NotFoundException {
    Set<String> newApiPackageNames = newApi.getApiPackageNames();
    missingPackageNames = oldApi.getApiPackageNames();
    Set<String> intersection =
        removeIntersection(newApiPackageNames, missingPackageNames);
    // Inspect each of the classes in each of the packages in the intersection
    for (String packageName : intersection) {
      ApiPackageDiffGenerator tempPackageDiffGenerator =
          new ApiPackageDiffGenerator(packageName, this);
      intersectingPackages.put(packageName, tempPackageDiffGenerator);
      tempPackageDiffGenerator.computeApiDiff();
    }
  }

}
