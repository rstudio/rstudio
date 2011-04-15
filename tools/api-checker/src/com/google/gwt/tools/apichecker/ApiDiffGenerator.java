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

  private static enum Relation {
    NONE, SUBCLASS, SUPERCLASS;
  }

  public static final String DELIMITER = " ";

  /* variable just for debugging -- find why an apiChange is being removed */
  static final String HAY_API_CHANGE = "";

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
    JClassType firstClassType = newApiTypeOracle.findType(classType1.getQualifiedSourceName());
    JClassType secondClassType = newApiTypeOracle.findType(classType2.getQualifiedSourceName());
    // The types might not necessarily exist in the newApi
    if (firstClassType == null || secondClassType == null) {
      return false;
    }
    return firstClassType.isAssignableTo(secondClassType);
  }

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
      ApiClassDiffGenerator result = findApiClassDiffGenerator(pkgName, typeName);
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
  ApiClassDiffGenerator findApiClassDiffGenerator(String pkgName, String typeName) {
    ApiPackageDiffGenerator pkg = findApiPackageDiffGenerator(pkgName);
    if (pkg != null) {
      ApiClassDiffGenerator type = pkg.findApiClassDiffGenerator(pkgName + "." + typeName);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  ApiPackageDiffGenerator findApiPackageDiffGenerator(String key) {
    return intersectingPackages.get(key);
  }

  Collection<ApiChange> getApiDiff() throws NotFoundException {

    computeApiDiff();

    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    Set<ApiPackage> missingPackages = oldApi.getApiPackagesBySet(missingPackageNames);
    for (ApiPackage missingPackage : missingPackages) {
      collection.add(new ApiChange(missingPackage, ApiChange.Status.MISSING));
    }
    for (ApiPackageDiffGenerator intersectingPackage : intersectingPackages.values()) {
      collection.addAll(intersectingPackage.getApiDiff());
    }
    return collection;
  }

  ApiContainer getNewApiContainer() {
    return newApi;
  }

  ApiContainer getOldApiContainer() {
    return oldApi;
  }

  /**
   * Remove any apiChange x if there is another apiChange y such that the
   * apiElement and status for both x and y are the same and the apiClass for x
   * is a subclass of the apiClass for y.
   * 
   * @param originalCollection collection with duplicates.
   * @return collection minus duplicates.
   */
  Collection<ApiChange> removeDuplicates(Collection<ApiChange> originalCollection) {
    /*
     * Map from the hashCode of an apiChange to the list of ApiChanges. There
     * can be multiple ApiChanges that have the same hashCode, but neither is a
     * subset of another. Example: if B and C both extend A, and there is an
     * ApiChange in B and C due to an api element of A.
     */
    Map<Integer, Collection<ApiChange>> apiChangeMap =
        new HashMap<Integer, Collection<ApiChange>>();
    for (ApiChange apiChange : originalCollection) {
      String apiChangeStr = apiChange.getApiElement().getRelativeSignature();
      Collection<ApiChange> apiChangesSameHashCode =
          apiChangeMap.get(apiChange.hashCodeForDuplication());
      if (apiChangesSameHashCode == null) {
        apiChangesSameHashCode = new HashSet<ApiChange>();
        apiChangeMap.put(apiChange.hashCodeForDuplication(), apiChangesSameHashCode);
      }
      Collection<ApiChange> apiChangesToRemove = new HashSet<ApiChange>();
      boolean addNewElement = true;

      for (ApiChange oldApiChange : apiChangesSameHashCode) {
        String oldApiChangeStr = oldApiChange.getApiElement().getRelativeSignature();
        Relation relation =
            getRelationOfApiClassOfFirstArgToThatOfSecond(apiChange.getApiElement(), oldApiChange
                .getApiElement());
        if (relation == Relation.SUPERCLASS) {
          apiChangesToRemove.add(oldApiChange);
          if (ApiCompatibilityChecker.DEBUG_DUPLICATE_REMOVAL
              && oldApiChangeStr.indexOf(HAY_API_CHANGE) != -1) {
            System.out.println(oldApiChangeStr + " replaced by " + apiChangeStr + ", status = "
                + oldApiChange.getStatus());
          }
        } else if (relation == Relation.SUBCLASS) {
          addNewElement = false;
          if (ApiCompatibilityChecker.DEBUG_DUPLICATE_REMOVAL
              && apiChangeStr.indexOf(HAY_API_CHANGE) != -1) {
            System.out.println(apiChangeStr + " replaced by " + oldApiChangeStr);
          }
        }
      }
      apiChangesSameHashCode.removeAll(apiChangesToRemove);
      if (addNewElement) {
        apiChangesSameHashCode.add(apiChange);
      }
    }
    Collection<ApiChange> prunedCollection = new HashSet<ApiChange>();
    for (Collection<ApiChange> changes : apiChangeMap.values()) {
      prunedCollection.addAll(changes);
    }
    return prunedCollection;
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
    Set<String> intersection = removeIntersection(newApiPackageNames, missingPackageNames);
    // Inspect each of the classes in each of the packages in the intersection
    for (String packageName : intersection) {
      ApiPackageDiffGenerator tempPackageDiffGenerator =
          new ApiPackageDiffGenerator(packageName, this);
      intersectingPackages.put(packageName, tempPackageDiffGenerator);
      tempPackageDiffGenerator.computeApiDiff();
    }
  }

  /**
   * Returns how ApiClass for first element is "related" to the ApiClass for
   * secondElement.
   */
  private Relation getRelationOfApiClassOfFirstArgToThatOfSecond(ApiElement firstApiElement,
      ApiElement secondApiElement) {
    JClassType firstClassType = null;
    JClassType secondClassType = null;
    if (firstApiElement instanceof ApiField) {
      firstClassType = ((ApiField) firstApiElement).getApiClass().getClassObject();
      secondClassType = ((ApiField) secondApiElement).getApiClass().getClassObject();
    }
    if (firstApiElement instanceof ApiAbstractMethod) {
      firstClassType = ((ApiAbstractMethod) firstApiElement).getApiClass().getClassObject();
      secondClassType = ((ApiAbstractMethod) secondApiElement).getApiClass().getClassObject();
    }
    if (firstClassType != null && secondClassType != null) {
      if (secondClassType.isAssignableTo(firstClassType)) {
        return Relation.SUPERCLASS;
      }
      if (firstClassType.isAssignableTo(secondClassType)) {
        return Relation.SUBCLASS;
      }
      return Relation.NONE;
    }
    throw new RuntimeException("Inconsistent types for ApiElements: newApiElement "
        + firstApiElement + ", oldApiElement : " + secondApiElement);
  }

}
