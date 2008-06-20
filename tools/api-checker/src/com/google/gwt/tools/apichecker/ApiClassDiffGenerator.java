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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces the diff between the API of two apiClasses.
 */
final class ApiClassDiffGenerator implements Comparable<ApiClassDiffGenerator> {

  static final Collection<ApiChange> EMPTY_COLLECTION =
      new ArrayList<ApiChange>(0);

  static String printSetWithHashCode(Set<?> set, String identifier) {
    StringBuffer sb = new StringBuffer();
    sb.append(identifier + ", size = " + set.size());
    for (Object element : set) {
      sb.append(element + ", hashcode = " + element.hashCode());
    }
    sb.append("\n");
    return sb.toString();
  }

  private static List<ApiChange> checkExceptions(ApiAbstractMethod newMethod,
      ApiAbstractMethod oldMethod) {
    JType newExceptions[] = newMethod.getMethod().getThrows();
    JType oldExceptions[] = oldMethod.getMethod().getThrows();
    List<ApiChange> ret = new ArrayList<ApiChange>();
    for (JType newException : newExceptions) {
      boolean isSubclass = false;
      for (JType oldException : oldExceptions) {
        if (ApiDiffGenerator.isFirstTypeAssignableToSecond(newException,
            oldException)) {
          isSubclass = true;
          break;
        }
      }
      if (!isSubclass) {
        ret.add(new ApiChange(oldMethod, ApiChange.Status.EXCEPTION_TYPE_ERROR,
            "unhandled exception in new code " + newException));
      }
    }
    return ret;
  }

  private Set<ApiField> allIntersectingFields = new HashSet<ApiField>();
  /**
   * Find all constructors, methods, fields that are present in either
   * intersection or missing members of this class or any superclass. Total of 6
   * things to keep track of. These variables are useful for memoization.
   */
  private EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>> allIntersectingMethods =
      null;

  private Set<ApiField> allMissingFields = null;
  private EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>> allMissingMethods =
      null;

  private final ApiDiffGenerator apiDiffGenerator;
  private final String className;
  private HashMap<ApiField, Set<ApiChange.Status>> intersectingFields = null;

  /**
   * Map from methods and constructors in intersection to a string describing
   * how they have changed. The description could be the addition/removal of a
   * static/abstract/final keyword.
   */
  private EnumMap<ApiClass.MethodType, Map<ApiAbstractMethod, Set<ApiChange>>> intersectingMethods;
  private Set<ApiField> missingFields = null;
  /**
   * list of missing constructors and methods.
   */
  private EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>> missingMethods;
  private final ApiClass newClass;

  private final ApiClass oldClass;

  ApiClassDiffGenerator(String className,
      ApiPackageDiffGenerator apiPackageDiffGenerator) throws NotFoundException {
    this.className = className;
    apiDiffGenerator = apiPackageDiffGenerator.getApiDiffGenerator();
    this.newClass =
        apiPackageDiffGenerator.getNewApiPackage().getApiClass(className);
    this.oldClass =
        apiPackageDiffGenerator.getOldApiPackage().getApiClass(className);
    if (newClass == null || oldClass == null) {
      throw new NotFoundException("for class " + className
          + ", one of the class objects is null");
    }

    intersectingFields = new HashMap<ApiField, Set<ApiChange.Status>>();
    intersectingMethods =
        new EnumMap<ApiClass.MethodType, Map<ApiAbstractMethod, Set<ApiChange>>>(
            ApiClass.MethodType.class);
    missingMethods =
        new EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>>(
            ApiClass.MethodType.class);
    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      intersectingMethods.put(methodType,
          new HashMap<ApiAbstractMethod, Set<ApiChange>>());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(ApiClassDiffGenerator other) {
    return getName().compareTo(other.getName());
  }

  /**
   * 
   * cleanApiDiff: remove an ApiDiff message from this class, if it is present
   * in the apiDiffs of any of the super-classes.
   * 
   * Compute the union of apiDiffs of all superclasses as part of this method.
   * Must be invoked only after intersectingMembers et al. have been computed.
   * Algorithm: - Find the immediate superclass that has computed these
   * apiDiffs. - Compute the union of apiDiffs of superClass with own apiDiffs.
   */
  void cleanApiDiff() {
    // check if unions have already been computed.
    if (allMissingMethods != null) {
      return;
    }
    ApiClassDiffGenerator other = getSuperclassApiClassDiffGenerator();
    // compute 'all*' fields for the 'other' object.
    if (other != null) {
      other.cleanApiDiff();
    }
    allIntersectingMethods =
        new EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>>(
            ApiClass.MethodType.class);
    allMissingMethods =
        new EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>>(
            ApiClass.MethodType.class);

    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      // for methods/constructors: clean the current apiDiffs
      if (other != null) {
        removeAll(intersectingMethods.get(methodType),
            other.allIntersectingMethods.get(methodType));
        missingMethods.get(methodType).removeAll(
            other.allMissingMethods.get(methodType));
      }
      // for methods/constructors: compute the allIntersecting*, allMissing*
      HashSet<ApiAbstractMethod> tempSet1 =
          new HashSet<ApiAbstractMethod>(
              intersectingMethods.get(methodType).keySet());
      HashSet<ApiAbstractMethod> tempSet2 =
          new HashSet<ApiAbstractMethod>(missingMethods.get(methodType));
      if (other != null) {
        tempSet1.addAll(other.allIntersectingMethods.get(methodType));
        tempSet2.addAll(other.allMissingMethods.get(methodType));
      }
      allIntersectingMethods.put(methodType, tempSet1);
      allMissingMethods.put(methodType, tempSet2);
    }

    // for fields: clean the current apiDiffs
    if (other != null) {
      removeAll(intersectingFields, other.allIntersectingFields);
      missingFields.removeAll(other.allMissingFields);
    }
    // for fields: compute allIntersectingFields, allMissingFields
    allIntersectingFields = new HashSet<ApiField>(intersectingFields.keySet());
    allMissingFields = new HashSet<ApiField>(missingFields);
    if (other != null) {
      allIntersectingFields.addAll(other.allIntersectingFields);
      allMissingFields.addAll(other.allMissingFields);
    }
  }

  // TODO(amitmanjhi): for methods, think about variable length arguments
  void computeApiDiff() {
    Set<String> newFieldNames = newClass.getApiFieldNames();
    Set<String> oldFieldNames = oldClass.getApiFieldNames();
    Set<String> intersection =
        ApiDiffGenerator.removeIntersection(newFieldNames, oldFieldNames);
    missingFields = oldClass.getApiFieldsBySet(oldFieldNames);
    processFieldsInIntersection(intersection);

    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      Set<String> newMethodNames = newClass.getApiMemberNames(methodType);
      Set<String> oldMethodNames = oldClass.getApiMemberNames(methodType);
      intersection =
          ApiDiffGenerator.removeIntersection(newMethodNames, oldMethodNames);
      missingMethods.put(methodType, oldClass.getApiMembersBySet(
          oldMethodNames, methodType));
      processElementsInIntersection(intersection, methodType);
    }
  }

  Collection<ApiChange> getApiDiff() {
    Collection<ApiChange.Status> apiStatusChanges =
        oldClass.getModifierChanges(newClass);
    /*
     * int totalSize = missingFields.size() + intersectingFields.size() +
     * apiStatusChanges.size(); for (ApiClass.MethodType methodType :
     * ApiClass.MethodType.values()) { totalSize +=
     * (missingMethods.get(methodType).size() + intersectingMethods.get(
     * methodType).size()); } if (totalSize == 0) { return EMPTY_COLLECTION; }
     */
    Collection<ApiChange> apiChangeCollection = new ArrayList<ApiChange>();
    for (ApiChange.Status apiStatus : apiStatusChanges) {
      apiChangeCollection.add(new ApiChange(oldClass, apiStatus));
    }
    // missing fields
    for (ApiElement element : missingFields) {
      apiChangeCollection.add(new ApiChange(element, ApiChange.Status.MISSING));
    }
    apiChangeCollection.addAll(getIntersectingFields());
    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      apiChangeCollection.addAll(getMissingMethods(methodType));
      apiChangeCollection.addAll(getIntersectingMethods(methodType));
    }
    return apiChangeCollection;
  }

  String getName() {
    return className;
  }

  /*
   * Even though the method name is contained in the "property" parameter, the
   * type information is lost. TODO (amitmanjhi): fix this issue later.
   */
  private <T> void addProperty(Map<T, Set<ApiChange>> hashMap, T key,
      ApiChange property) {
    /*
     * if (!ApiCompatibilityChecker.PRINT_COMPATIBLE && (property.getStatus() ==
     * ApiChange.Status.COMPATIBLE)) { return; } if
     * (!ApiCompatibilityChecker.PRINT_COMPATIBLE_WITH && property.getStatus() ==
     * ApiChange.Status.COMPATIBLE_WITH) { return; }
     */
    Set<ApiChange> value = hashMap.get(key);
    if (value == null) {
      value = new HashSet<ApiChange>();
    }
    value.add(property);
    hashMap.put(key, value);
  }

  private Collection<ApiChange> getIntersectingFields() {
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    List<ApiField> intersectingFieldsList =
        new ArrayList<ApiField>(intersectingFields.keySet());
    Collections.sort(intersectingFieldsList);
    for (ApiField apiField : intersectingFieldsList) {
      for (ApiChange.Status status : intersectingFields.get(apiField)) {
        collection.add(new ApiChange(apiField, status));
      }
    }
    return collection;
  }

  private Collection<ApiChange> getIntersectingMethods(
      ApiClass.MethodType methodType) {
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    List<ApiAbstractMethod> apiMethodsList =
        new ArrayList<ApiAbstractMethod>(
            intersectingMethods.get(methodType).keySet());
    Collections.sort(apiMethodsList);
    for (ApiAbstractMethod apiMethod : apiMethodsList) {
      collection.addAll(intersectingMethods.get(methodType).get(apiMethod));
    }
    return collection;
  }

  private Collection<ApiChange> getMissingMethods(ApiClass.MethodType methodType) {
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    List<ApiAbstractMethod> apiMethodsList =
        new ArrayList<ApiAbstractMethod>(missingMethods.get(methodType));
    Collections.sort(apiMethodsList);
    for (ApiAbstractMethod apiMethod : apiMethodsList) {
      collection.add(new ApiChange(apiMethod, ApiChange.Status.MISSING));
    }
    return collection;
  }

  /**
   * return the ApiClassDiffGenerator object for the "closest" ancestor of
   * oldClass. return null if no ancestor of oldClass has ApiClassDiffGenerator
   */
  private ApiClassDiffGenerator getSuperclassApiClassDiffGenerator() {
    ApiClassDiffGenerator other = null;
    JClassType classType = oldClass.getClassObject();
    while ((classType = classType.getSuperclass()) != null) {
      other =
          apiDiffGenerator.findApiClassDiffGenerator(classType.getQualifiedSourceName());
      if (other != null) {
        return other;
      }
    }
    return null;
  }

  private boolean isIncompatibileDueToMethodOverloading(
      Set<ApiAbstractMethod> methodsInNew,
      Set<ApiAbstractMethod> methodsInExisting) {
    if (!ApiCompatibilityChecker.API_SOURCE_COMPATIBILITY
        || methodsInExisting.size() != 1 || methodsInNew.size() <= 1) {
      return false;
    }
    String signature =
        methodsInExisting.toArray(new ApiAbstractMethod[0])[0].getCoarseSignature();
    int numMatchingSignature = 0;
    for (ApiAbstractMethod current : methodsInNew) {
      if (current.getCoarseSignature().equals(signature)) {
        ++numMatchingSignature;
      }
    }
    return numMatchingSignature > 1;
  }

  private void processElementsInIntersection(Set<String> intersection,
      ApiClass.MethodType methodType) {

    Set<ApiAbstractMethod> missingElements = missingMethods.get(methodType);
    Map<ApiAbstractMethod, Set<ApiChange>> intersectingElements =
        intersectingMethods.get(methodType);

    Set<ApiAbstractMethod> onlyInExisting = new HashSet<ApiAbstractMethod>();
    Set<ApiAbstractMethod> onlyInNew = new HashSet<ApiAbstractMethod>();
    Set<String> commonSignature = new HashSet<String>();

    for (String elementName : intersection) {
      Set<ApiAbstractMethod> methodsInNew =
          newClass.getApiMethodsByName(elementName, methodType);
      Set<ApiAbstractMethod> methodsInExisting =
          oldClass.getApiMethodsByName(elementName, methodType);
      onlyInNew.addAll(methodsInNew);
      onlyInExisting.addAll(methodsInExisting);
      if (isIncompatibileDueToMethodOverloading(methodsInNew, methodsInExisting)) {
        ApiAbstractMethod methodInExisting =
            methodsInExisting.toArray(new ApiAbstractMethod[0])[0];
        addProperty(intersectingElements, methodInExisting, new ApiChange(
            methodInExisting, ApiChange.Status.OVERLOADED_METHOD_CALL,
            "Many methods in the new API with similar signatures. Methods = "
                + methodsInNew + " This might break API source compatibility"));
      }
      // We want to find out which method calls that the current API supports
      // will succeed even with the new API. Determine this by iterating over
      // the methods of the current API
      for (ApiAbstractMethod methodInExisting : methodsInExisting) {
        for (ApiAbstractMethod methodInNew : methodsInNew) {
          if (methodInExisting.isCompatible(methodInNew)) {
            ApiChange returnType =
                methodInExisting.checkReturnTypeCompatibility(methodInNew);
            if (returnType != null) {
              addProperty(intersectingElements, methodInExisting, returnType);
            }
            for (ApiChange apiChange : checkExceptions(methodInNew,
                methodInExisting)) {
              addProperty(intersectingElements, methodInExisting, apiChange);
            }
            for (ApiChange.Status status : methodInExisting.getModifierChanges(methodInNew)) {
              addProperty(intersectingElements, methodInExisting,
                  new ApiChange(methodInExisting, status));
            }
            onlyInNew.remove(methodInNew);
            onlyInExisting.remove(methodInExisting);
            String signatureInNew = methodInNew.getApiSignature();
            String signatureInExisting = methodInExisting.getApiSignature();
            if (signatureInNew.equals(signatureInExisting)) {
              commonSignature.add(signatureInNew);
              addProperty(intersectingElements, methodInExisting,
                  new ApiChange(methodInExisting, ApiChange.Status.COMPATIBLE));
            } else {
              addProperty(intersectingElements, methodInExisting,
                  new ApiChange(methodInExisting,
                      ApiChange.Status.COMPATIBLE_WITH, signatureInNew));
            }
          }
        }
      }
      // printOutput(commonSignature, onlyInExisting, onlyInNew);
    }
    missingElements.addAll(onlyInExisting);
  }

  private void processFieldsInIntersection(Set<String> intersection) {
    for (String fieldName : intersection) {
      ApiField newField = newClass.getApiFieldByName(fieldName);
      ApiField oldField = oldClass.getApiFieldByName(fieldName);
      Set<ApiChange.Status> apiChanges = oldField.getModifierChanges(newField);
      if (apiChanges.size() > 0) {
        intersectingFields.put(oldField, apiChanges);
      }
    }
  }

  private <E, V> void removeAll(Map<E, Set<V>> tempMap, Set<E> removeKeys) {
    for (E element : removeKeys) {
      tempMap.remove(element);
    }
  }

}
