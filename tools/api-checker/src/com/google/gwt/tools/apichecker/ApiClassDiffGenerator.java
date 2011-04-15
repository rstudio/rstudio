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

  static final Collection<ApiChange> EMPTY_COLLECTION = new ArrayList<ApiChange>(0);

  static String printSetWithHashCode(Set<?> set, String identifier) {
    StringBuffer sb = new StringBuffer();
    sb.append(identifier + ", size = " + set.size());
    for (Object element : set) {
      sb.append(element + ", hashcode = " + element.hashCode());
    }
    sb.append("\n");
    return sb.toString();
  }

  // TODO: variable never read, remove?
  private final ApiDiffGenerator apiDiffGenerator;
  private final String className;
  private HashMap<ApiField, Set<ApiChange>> intersectingFields = null;

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

  ApiClassDiffGenerator(String className, ApiPackageDiffGenerator apiPackageDiffGenerator)
      throws NotFoundException {
    this.className = className;
    apiDiffGenerator = apiPackageDiffGenerator.getApiDiffGenerator();
    this.newClass = apiPackageDiffGenerator.getNewApiPackage().getApiClass(className);
    this.oldClass = apiPackageDiffGenerator.getOldApiPackage().getApiClass(className);
    if (newClass == null || oldClass == null) {
      throw new NotFoundException("for class " + className + ", one of the class objects is null");
    }

    intersectingFields = new HashMap<ApiField, Set<ApiChange>>();
    intersectingMethods =
        new EnumMap<ApiClass.MethodType, Map<ApiAbstractMethod, Set<ApiChange>>>(
            ApiClass.MethodType.class);
    missingMethods =
        new EnumMap<ApiClass.MethodType, Set<ApiAbstractMethod>>(ApiClass.MethodType.class);
    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      intersectingMethods.put(methodType, new HashMap<ApiAbstractMethod, Set<ApiChange>>());
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ApiClassDiffGenerator)) {
      return false;
    }
    return this.getName().equals(((ApiClassDiffGenerator) o).getName());
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  // TODO(amitmanjhi): handle methods with variable length arguments
  void computeApiDiff() {
    Set<String> newFieldNames = newClass.getApiFieldNames();
    Set<String> oldFieldNames = oldClass.getApiFieldNames();
    Set<String> intersection = ApiDiffGenerator.removeIntersection(newFieldNames, oldFieldNames);
    missingFields = oldClass.getApiFieldsBySet(oldFieldNames);
    processFieldsInIntersection(intersection);

    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      Set<String> newMethodNames = newClass.getApiMemberNames(methodType);
      Set<String> oldMethodNames = oldClass.getApiMemberNames(methodType);
      intersection = ApiDiffGenerator.removeIntersection(newMethodNames, oldMethodNames);
      missingMethods.put(methodType, oldClass.getApiMembersBySet(oldMethodNames, methodType));
      processElementsInIntersection(intersection, methodType);
    }
  }

  Collection<ApiChange> getApiDiff() {
    Collection<ApiChange.Status> apiStatusChanges = oldClass.getModifierChanges(newClass);
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
  private <T> void addProperty(Map<T, Set<ApiChange>> hashMap, T key, ApiChange property) {
    Set<ApiChange> value = hashMap.get(key);
    if (value == null) {
      value = new HashSet<ApiChange>();
    }
    value.add(property);
    hashMap.put(key, value);
  }

  private Collection<ApiChange> getIntersectingFields() {
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    List<ApiField> intersectingFieldsList = new ArrayList<ApiField>(intersectingFields.keySet());
    Collections.sort(intersectingFieldsList);
    for (ApiField apiField : intersectingFieldsList) {
      for (ApiChange apiChange : intersectingFields.get(apiField)) {
        collection.add(apiChange);
      }
    }
    return collection;
  }

  private Collection<ApiChange> getIntersectingMethods(ApiClass.MethodType methodType) {
    Collection<ApiChange> collection = new ArrayList<ApiChange>();
    List<ApiAbstractMethod> apiMethodsList =
        new ArrayList<ApiAbstractMethod>(intersectingMethods.get(methodType).keySet());
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
   * Attempts to find out if a methodName(null) call previously succeeded, and
   * would fail with the new Api. Currently, this method is simple.
   * TODO(amitmanjhi): generalize this method.
   * 
   * @param methodsInNew Candidate methods in the new Api
   * @param methodsInExisting Candidate methods in the existing Api.
   * @return the possible incompatibilities due to method overloading.
   */
  private Map<ApiAbstractMethod, ApiChange> getOverloadedMethodIncompatibility(
      Set<ApiAbstractMethod> methodsInNew, Set<ApiAbstractMethod> methodsInExisting) {
    if (!ApiCompatibilityChecker.API_SOURCE_COMPATIBILITY || methodsInExisting.size() != 1
        || methodsInNew.size() <= 1) {
      return Collections.emptyMap();
    }
    ApiAbstractMethod existingMethod = methodsInExisting.toArray(new ApiAbstractMethod[0])[0];
    String signature = existingMethod.getCoarseSignature();
    List<ApiAbstractMethod> matchingMethods = new ArrayList<ApiAbstractMethod>();
    for (ApiAbstractMethod current : methodsInNew) {
      if (current.getCoarseSignature().equals(signature)) {
        matchingMethods.add(current);
      }
    }
    if (isPairwiseCompatible(matchingMethods)) {
      return Collections.emptyMap();
    }
    Map<ApiAbstractMethod, ApiChange> incompatibilities =
        new HashMap<ApiAbstractMethod, ApiChange>();
    incompatibilities.put(existingMethod, new ApiChange(existingMethod,
        ApiChange.Status.OVERLOADED_METHOD_CALL,
        "Many methods in the new API with similar signatures. Methods = " + methodsInNew
            + " This might break API source compatibility"));
    return incompatibilities;
  }

  /**
   * @return true if each pair of methods within the list is compatibile.
   */
  private boolean isPairwiseCompatible(List<ApiAbstractMethod> methods) {
    int length = methods.size();
    for (int i = 0; i < length - 1; i++) {
      for (int j = i + 1; j < length; j++) {
        ApiAbstractMethod firstMethod = methods.get(i);
        ApiAbstractMethod secondMethod = methods.get(j);
        if (!firstMethod.isCompatible(secondMethod) && !secondMethod.isCompatible(firstMethod)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Processes elements in intersection, checking for incompatibilities.
   * 
   * @param intersection
   * @param methodType
   */
  private void processElementsInIntersection(Set<String> intersection,
      ApiClass.MethodType methodType) {

    Set<ApiAbstractMethod> missingElements = missingMethods.get(methodType);
    Map<ApiAbstractMethod, Set<ApiChange>> intersectingElements =
        intersectingMethods.get(methodType);

    Set<ApiAbstractMethod> onlyInExisting = new HashSet<ApiAbstractMethod>();
    Set<ApiAbstractMethod> onlyInNew = new HashSet<ApiAbstractMethod>();
    Set<String> commonSignature = new HashSet<String>();

    for (String elementName : intersection) {
      Set<ApiAbstractMethod> methodsInNew = newClass.getApiMethodsByName(elementName, methodType);
      Set<ApiAbstractMethod> methodsInExisting =
          oldClass.getApiMethodsByName(elementName, methodType);
      onlyInNew.addAll(methodsInNew);
      onlyInExisting.addAll(methodsInExisting);
      Map<ApiAbstractMethod, ApiChange> incompatibilityMap =
          getOverloadedMethodIncompatibility(methodsInNew, methodsInExisting);
      for (Map.Entry<ApiAbstractMethod, ApiChange> entry : incompatibilityMap.entrySet()) {
        addProperty(intersectingElements, entry.getKey(), entry.getValue());
      }

      /*
       * We want to find out which method calls that the current API supports
       * will succeed even with the new API. Determine this by iterating over
       * the methods of the current API. Keep track of a method that has the
       * same exact argument types as the old method. If such a method exists,
       * check Api compatibility with just that method. Otherwise, check api
       * compatibility with ALL methods that might be compatible. (This
       * conservative estimate will work as long as we do not change the Api in
       * pathological ways.)
       */
      for (ApiAbstractMethod methodInExisting : methodsInExisting) {
        Set<ApiChange> allPossibleApiChanges = new HashSet<ApiChange>();
        ApiAbstractMethod sameSignatureMethod = null;
        for (ApiAbstractMethod methodInNew : methodsInNew) {
          Set<ApiChange> currentApiChange = new HashSet<ApiChange>();
          boolean hasSameSignature = false;
          if (methodInExisting.isCompatible(methodInNew)) {
            if (methodInExisting.isOverridable()) {
              // check if the new method's api is exactly the same
              currentApiChange.addAll(methodInExisting.getAllChangesInApi(methodInNew));
            } else {
              // check for changes to return type and exceptions
              currentApiChange.addAll(methodInExisting.checkExceptionsAndReturnType(methodInNew));
            }
            for (ApiChange.Status status : methodInExisting.getModifierChanges(methodInNew)) {
              currentApiChange.add(new ApiChange(methodInExisting, status));
            }
            if (methodInNew.getInternalSignature().equals(methodInExisting.getInternalSignature())) {
              currentApiChange.add(new ApiChange(methodInExisting, ApiChange.Status.COMPATIBLE));
              hasSameSignature = true;
            } else {
              currentApiChange.add(new ApiChange(methodInExisting,
                  ApiChange.Status.COMPATIBLE_WITH, methodInNew.getApiSignature()));
            }
          }

          if (currentApiChange.size() > 0) {
            if (hasSameSignature) {
              allPossibleApiChanges = currentApiChange;
              sameSignatureMethod = methodInNew;
            } else if (sameSignatureMethod == null) {
              allPossibleApiChanges.addAll(currentApiChange);
            }
          }
        }
        // put the best Api match
        if (allPossibleApiChanges.size() > 0) {
          onlyInExisting.remove(methodInExisting);
          String signatureInExisting = methodInExisting.getInternalSignature();
          if (sameSignatureMethod != null
              && signatureInExisting.equals(sameSignatureMethod.getInternalSignature())) {
            commonSignature.add(signatureInExisting);
          }
          for (ApiChange apiChange : allPossibleApiChanges) {
            addProperty(intersectingElements, methodInExisting, apiChange);
          }
        }
      }

      /**
       * Look for incompatiblities that might result due to new methods
       * over-loading existing methods. Instead of applying JLS to determine the
       * best match, just be conservative and report all possible
       * incompatibilities if there is no old method with the exact same
       * signature.
       * 
       * <pre>
       * class A { // old version 
       *   final void foo(Set<String> p1, Set<String> p2); 
       * }
       * 
       * class A { // new version 
       *   final void foo(Set<String> p1, Set<String> p2); 
       *   void foo(HashSet<String> p1, Set<String> p2) throws ...; 
       * }
       * </pre>
       */
      for (ApiAbstractMethod methodInNew : methodsInNew) {
        ApiAbstractMethod sameSignatureMethod = null;
        for (ApiAbstractMethod methodInExisting : methodsInExisting) {
          if (methodInNew.getInternalSignature().equals(methodInExisting.getInternalSignature())) {
            sameSignatureMethod = methodInExisting;
            break;
          }
        }

        // do not look for incompatibilities with overloaded methods, if exact
        // match exists.
        if (sameSignatureMethod != null) {
          continue;
        }
        for (ApiAbstractMethod methodInExisting : methodsInExisting) {
          if (methodInNew.isCompatible(methodInExisting)) {
            // new method is going to be called instead of existing method,
            // determine incompatibilities
            for (ApiChange apiChange : methodInExisting.checkExceptionsAndReturnType(methodInNew)) {
              addProperty(intersectingElements, methodInExisting, apiChange);
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
      Set<ApiChange> apiChanges = oldField.getModifierChanges(newField);
      if (apiChanges.size() > 0) {
        intersectingFields.put(oldField, apiChanges);
      }
    }
  }

}
