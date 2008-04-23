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

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Produces the diff between the API of two apiClasses.
 */
public class ApiClassDiffGenerator {
  abstract static class DuplicateDetector<E> {
    ApiDiffGenerator apiDiffGenerator;
    JClassType currentClass;
    int initialValue = 1;
    ApiClass.MethodType methodType;

    DuplicateDetector(JClassType currentClass,
        ApiDiffGenerator apiDiffGenerator, ApiClass.MethodType methodType) {
      this.currentClass = currentClass;
      this.apiDiffGenerator = apiDiffGenerator;
      initialValue++;
      this.methodType = methodType;
    }

    JClassType getClassType(E element) {
      if (element instanceof JAbstractMethod) {
        JAbstractMethod jam = (JAbstractMethod) element;
        return jam.getEnclosingType();
      }
      if (element instanceof JField) {
        JField field = (JField) element;
        return field.getEnclosingType();
      }
      return null;
    }

    abstract HashSet<E> getElements(ApiClassDiffGenerator other);

    boolean isDuplicate(E element) {
      JClassType classType = getClassType(element);
      if (classType == currentClass) {
        return false;
      }
      ApiClassDiffGenerator other = apiDiffGenerator.findApiClassDiffGenerator(classType);
      if (other == null) {
        return false;
      }
      return getElements(other).contains(element);
    }
  }

  private static ArrayList<ApiChange> checkExceptions(
      JAbstractMethod newMethod, JAbstractMethod oldMethod) {
    JType oldExceptions[] = oldMethod.getThrows();
    JType newExceptions[] = newMethod.getThrows();
    ArrayList<ApiChange> ret = new ArrayList<ApiChange>();
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
        ret.add(new ApiChange(ApiChange.Status.EXCEPTIONS_ERROR,
            "unhandled exception in new code " + newException.toString()));
      }
    }
    return ret;
  }

  HashSet<JField> allIntersectingFields = new HashSet<JField>();

  /**
   * Find all constructors, methods, fields that are present in either
   * intersection or missing members of this class or any superclass. Total of 6
   * things to keep track of. These variables are useful for memoization.
   */
  ArrayList<HashSet<JAbstractMethod>> allIntersectingMethods = new ArrayList<HashSet<JAbstractMethod>>(
      2);

  ArrayList<HashSet<JAbstractMethod>> allMissingMethods = new ArrayList<HashSet<JAbstractMethod>>(
      2);
  ApiDiffGenerator apiDiffGenerator = null;
  ApiPackageDiffGenerator apiPackageDiffGenerator = null;
  String className = null;

  HashMap<JField, HashSet<ApiChange.Status>> intersectingFields = null;

  /**
   * Map from methods and constructors in intersection to a string describing
   * how they have changed. The description could be the addition/removal of a
   * static/abstract/final keyword.
   */
  ArrayList<HashMap<JAbstractMethod, HashSet<ApiChange>>> intersectingMethods;
  HashSet<JField> missingFields = null;
  /**
   * list of missing constructors and methods.
   */
  ArrayList<HashSet<JAbstractMethod>> missingMethods;
  ApiClass newClass = null;

  ApiClass oldClass = null;
  private HashSet<JField> allMissingFields = new HashSet<JField>();

  public ApiClassDiffGenerator(String className,
      ApiPackageDiffGenerator apiPackageDiffGenerator) throws NotFoundException {
    this.className = className;
    this.apiPackageDiffGenerator = apiPackageDiffGenerator;
    apiDiffGenerator = apiPackageDiffGenerator.getApiDiffGenerator();
    this.newClass = apiPackageDiffGenerator.getNewApiPackage().getApiClass(
        className);
    this.oldClass = apiPackageDiffGenerator.getOldApiPackage().getApiClass(
        className);
    if (newClass == null || oldClass == null) {
      throw new NotFoundException("for class " + className
          + ", one of the class objects is null");
    }
    intersectingFields = new HashMap<JField, HashSet<ApiChange.Status>>();
    intersectingMethods = new ArrayList<HashMap<JAbstractMethod, HashSet<ApiChange>>>(
        ApiClass.MethodType.values().length);
    missingMethods = new ArrayList<HashSet<JAbstractMethod>>(
        ApiClass.MethodType.values().length);
    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      intersectingMethods.add(methodType.getId(),
          new HashMap<JAbstractMethod, HashSet<ApiChange>>());
    }
  }

  public void cleanApiDiff() {
    /**
     * Two different ways of eliminating duplicates from apiDiffs. 1.
     * computeUnionsAndCleandApiDiff: remove an ApiDiff message from this class,
     * if it is present in the apiDiffs of any of the superclasses. (Not sure if
     * the implementation always yield the correct result. Therefore, I have not
     * removed the cleanApiDiff2 implementation.)
     * 
     * 2. cleanApiDiff2: If the ApiDiff message is about member 'x', remove the
     * ApiDiff message from this class, if the class defining 'x' also contains
     * this message.
     */
    if (true) {
      computeUnionsAndCleanApiDiff();
    } else {
      cleanApiDiff2();
    }
  }

  public void cleanApiDiff2() {
    DuplicateDetector<JField> fieldRemover = new DuplicateDetector<JField>(
        oldClass.getClassObject(), apiDiffGenerator,
        ApiClass.MethodType.CONSTRUCTOR) {
      @Override
      HashSet<JField> getElements(ApiClassDiffGenerator other) {
        return other.getMissingFields();
      }
    };
    missingFields.removeAll(getDuplicateElements(missingFields.iterator(),
        fieldRemover));

    DuplicateDetector<JField> intersectingFieldRemover = new DuplicateDetector<JField>(
        oldClass.getClassObject(), apiDiffGenerator,
        ApiClass.MethodType.CONSTRUCTOR) {
      @Override
      HashSet<JField> getElements(ApiClassDiffGenerator other) {
        return other.getIntersectingFields();
      }
    };
    removeAll(intersectingFields, getDuplicateElements(
        intersectingFields.keySet().iterator(), intersectingFieldRemover));

    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      DuplicateDetector<JAbstractMethod> missingMemberRemover = new DuplicateDetector<JAbstractMethod>(
          oldClass.getClassObject(), apiDiffGenerator, methodType) {
        @Override
        HashSet<JAbstractMethod> getElements(final ApiClassDiffGenerator other) {
          return other.getMissingMethods(methodType);
        }
      };
      missingMethods.get(methodType.getId()).removeAll(
          getDuplicateElements(
              missingMethods.get(methodType.getId()).iterator(),
              missingMemberRemover));

      DuplicateDetector<JAbstractMethod> intersectingMemberRemover = new DuplicateDetector<JAbstractMethod>(
          oldClass.getClassObject(), apiDiffGenerator, methodType) {
        @Override
        HashSet<JAbstractMethod> getElements(final ApiClassDiffGenerator other) {
          return other.getIntersectingMethods(methodType);
        }
      };
      removeAll(intersectingMethods.get(methodType.getId()),
          getDuplicateElements(
              intersectingMethods.get(methodType.getId()).keySet().iterator(),
              intersectingMemberRemover));
    }
  }

  public void computeApiDiff() {
    HashSet<String> newFieldNames = newClass.getApiFieldNames();
    HashSet<String> oldFieldNames = oldClass.getApiFieldNames();
    HashSet<String> intersection = ApiDiffGenerator.extractCommonElements(
        newFieldNames, oldFieldNames);
    missingFields = oldClass.getApiFieldsBySet(oldFieldNames);
    processFieldsInIntersection(intersection);

    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      HashSet<String> newMethodNames = newClass.getApiMemberNames(methodType);
      HashSet<String> oldMethodNames = oldClass.getApiMemberNames(methodType);
      intersection = ApiDiffGenerator.extractCommonElements(newMethodNames,
          oldMethodNames);
      missingMethods.add(methodType.getId(),
          getAbstractMethodObjects(oldClass.getApiMembersBySet(oldMethodNames,
              methodType)));
      processElementsInIntersection(intersection, methodType);
    }
  }

  /**
   * Compute the union of apiDiffs of all superclasses. Must be invoked only
   * after intersectingMembers et al. have been computed. Algorithm: - Find the
   * immediate superclass that has computed these apiDiffs. - Compute the union
   * of apiDiffs of superClass with own apiDiffs.
   */
  public void computeUnionsAndCleanApiDiff() {
    // check if unions have already been computed.
    if (allMissingMethods.size() > 0) {
      return;
    }
    ApiClassDiffGenerator other = getSuperclassApiClassDiffGenerator();
    // compute 'all*' fields for the 'other' object.
    if (other != null) {
      other.computeUnionsAndCleanApiDiff();
    }

    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      // clean the current apiDiffs
      if (other != null) {
        removeAll(intersectingMethods.get(methodType.getId()),
            other.allIntersectingMethods.get(methodType.getId()));
        missingMethods.get(methodType.getId()).removeAll(
            other.allMissingMethods.get(methodType.getId()));
      }
      // compute the union
      HashSet<JAbstractMethod> tempSet1 = new HashSet<JAbstractMethod>(
          intersectingMethods.get(methodType.getId()).keySet());
      HashSet<JAbstractMethod> tempSet2 = new HashSet<JAbstractMethod>(
          missingMethods.get(methodType.getId()));
      if (other != null) {
        tempSet1.addAll(other.allIntersectingMethods.get(methodType.getId()));
        tempSet2.addAll(other.allMissingMethods.get(methodType.getId()));
      }
      allIntersectingMethods.add(methodType.getId(), tempSet1);
      allMissingMethods.add(methodType.getId(), tempSet2);
    }
    // clean the current apiDiffs
    if (other != null) {
      removeAll(intersectingFields, other.allIntersectingFields);
      missingFields.removeAll(other.allMissingFields);
    }
    // compute the union
    allIntersectingFields = new HashSet<JField>(intersectingFields.keySet());
    allMissingFields = new HashSet<JField>(missingFields);
    if (other != null) {
      allIntersectingFields.addAll(other.allIntersectingFields);
      allMissingFields.addAll(other.allMissingFields);
    }
  }

  public HashSet<JAbstractMethod> getAbstractMethodObjects(
      HashSet<? extends ApiAbstractMethod> temp) {
    Iterator<? extends ApiAbstractMethod> apiMethodsIterator = temp.iterator();
    HashSet<JAbstractMethod> returnSet = new HashSet<JAbstractMethod>();
    while (apiMethodsIterator.hasNext()) {
      returnSet.add(apiMethodsIterator.next().getMethodObject());
    }
    return returnSet;
  }

  public <T> HashSet<T> getDuplicateElements(Iterator<T> iterator,
      DuplicateDetector<T> detector) {
    HashSet<T> returnSet = new HashSet<T>();
    while (iterator.hasNext()) {
      T element = iterator.next();
      if (detector.isDuplicate(element)) {
        returnSet.add(element);
      }
    }
    return returnSet;
  }

  public HashSet<JField> getIntersectingFields() {
    return new HashSet<JField>(intersectingFields.keySet());
  }

  public HashSet<JAbstractMethod> getIntersectingMethods(
      ApiClass.MethodType methodType) {
    return new HashSet<JAbstractMethod>(intersectingMethods.get(
        methodType.getId()).keySet());
  }

  public HashSet<JField> getMissingFields() {
    return missingFields;
  }

  public HashSet<JAbstractMethod> getMissingMethods(
      ApiClass.MethodType methodType) {
    return missingMethods.get(methodType.getId());
  }

  public HashSet<ApiChange.Status> getModifierChangesForField(JField newField,
      JField oldField) {
    HashSet<ApiChange.Status> statuses = new HashSet<ApiChange.Status>();
    if (!oldField.isFinal() && newField.isFinal()) {
      statuses.add(ApiChange.Status.FINAL_ADDED);
    }
    if ((oldField.isStatic() && !newField.isStatic())) {
      statuses.add(ApiChange.Status.STATIC_REMOVED);
    }
    return statuses;
  }

  public String printApiDiff() {
    ArrayList<ApiChange.Status> apiChanges = oldClass.getModifierChanges(newClass);
    int totalSize = missingFields.size() + intersectingFields.size()
        + apiChanges.size();
    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      totalSize += (missingMethods.get(methodType.getId()).size() + intersectingMethods.get(
          methodType.getId()).size());
    }
    if (totalSize == 0) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    Iterator<ApiChange.Status> apiChangeIterator = apiChanges.iterator();
    while (apiChangeIterator.hasNext()) {
      sb.append("\t\t" + oldClass.getFullName() + " "
          + apiChangeIterator.next() + "\n");
    }
    if (apiChanges.size() == 0) {
      sb.append("\t\tclass " + oldClass.getFullName() + "\n");
    }
    sb.append(printCollectionElements(missingFields.iterator()));
    sb.append(printCollectionElements2(intersectingFields));
    for (ApiClass.MethodType methodType : ApiClass.MethodType.values()) {
      sb.append(printCollectionElements(missingMethods.get(methodType.getId()).iterator()));
      sb.append(printCollectionElements(intersectingMethods.get(methodType.getId())));
    }
    sb.append("\n");
    return sb.toString();
  }

  public <E, V> void removeAll(HashMap<E, HashSet<V>> tempMap,
      HashSet<E> removeKeys) {
    Iterator<E> keyIterator = removeKeys.iterator();
    while (keyIterator.hasNext()) {
      tempMap.remove(keyIterator.next());
    }
  }

  private <T> void addProperty(HashMap<T, HashSet<ApiChange>> hashMap, T key,
      ApiChange property) {
    if (!ApiCompatibilityChecker.PRINT_INTERSECTIONS
        && (property.getStatus() == ApiChange.Status.COMPATIBLE)) {
      return;
    }
    if (!ApiCompatibilityChecker.PRINT_COMPATIBLE_WITH
        && property.getStatus() == ApiChange.Status.COMPATIBLE_WITH) {
      return;
    }
    HashSet<ApiChange> value = hashMap.get(key);
    if (value == null) {
      value = new HashSet<ApiChange>();
    }
    value.add(property);
    hashMap.put(key, value);
  }

  /**
   * return the ApiClassDiffGenerator object for the "closest" ancestor of
   * oldClass. return null if no ancestor of oldClass has ApiClassDiffGenerator
   */
  private ApiClassDiffGenerator getSuperclassApiClassDiffGenerator() {
    ApiClassDiffGenerator other = null;
    JClassType classType = oldClass.getClassObject();
    while ((classType = classType.getSuperclass()) != null) {
      other = apiDiffGenerator.findApiClassDiffGenerator(classType);
      if (other != null) {
        return other;
      }
    }
    return null;
  }

  private boolean isIncompatibileDueToMethodOverloading(
      HashSet<ApiAbstractMethod> methodsInNew,
      HashSet<ApiAbstractMethod> methodsInExisting) {
    if (!ApiCompatibilityChecker.API_SOURCE_COMPATIBILITY
        || methodsInExisting.size() != 1 || methodsInNew.size() <= 1) {
      return false;
    }
    String signature = methodsInExisting.toArray(new ApiAbstractMethod[0])[0].getCoarseSignature();
    Iterator<ApiAbstractMethod> newMethodsIterator = methodsInNew.iterator();
    int numMatchingSignature = 0;
    while (newMethodsIterator.hasNext() && numMatchingSignature < 2) {
      ApiAbstractMethod current = newMethodsIterator.next();
      if (current.getCoarseSignature().equals(signature)) {
        ++numMatchingSignature;
      }
    }
    return numMatchingSignature > 1;
  }

  private <V> String printCollectionElements(
      HashMap<JAbstractMethod, HashSet<V>> tempHashMap) {
    StringBuffer sb = new StringBuffer();
    Iterator<JAbstractMethod> tempIterator = tempHashMap.keySet().iterator();
    while (tempIterator.hasNext()) {
      JAbstractMethod element = tempIterator.next();
      String identifier = oldClass.computeRelativeSignature(element);
      Iterator<V> tempIterator2 = tempHashMap.get(element).iterator();
      while (tempIterator2.hasNext()) {
        sb.append("\t\t\t" + identifier + ApiDiffGenerator.DELIMITER
            + tempIterator2.next() + "\n");
      }
    }
    return sb.toString();
  }

  private <E> String printCollectionElements(Iterator<E> temp) {
    StringBuffer sb = new StringBuffer();
    while (temp.hasNext()) {
      sb.append("\t\t\t" + oldClass.computeRelativeSignature(temp.next())
          + ApiDiffGenerator.DELIMITER + ApiChange.Status.MISSING + "\n");
    }
    return sb.toString();
  }

  private <V> String printCollectionElements2(
      HashMap<JField, HashSet<V>> tempHashMap) {
    StringBuffer sb = new StringBuffer();
    Iterator<JField> tempIterator = tempHashMap.keySet().iterator();
    while (tempIterator.hasNext()) {
      JField element = tempIterator.next();
      String identifier = oldClass.computeRelativeSignature(element);
      Iterator<V> tempIterator2 = tempHashMap.get(element).iterator();
      while (tempIterator2.hasNext()) {
        sb.append("\t\t\t" + identifier + ApiDiffGenerator.DELIMITER
            + tempIterator2.next() + "\n");
      }
    }
    return sb.toString();
  }

  private void processElementsInIntersection(HashSet<String> intersection,
      ApiClass.MethodType methodType) {
    if (intersection.size() == 0) {
      return;
    }
    HashSet<JAbstractMethod> missingElements = missingMethods.get(methodType.getId());
    HashMap<JAbstractMethod, HashSet<ApiChange>> intersectingElements = intersectingMethods.get(methodType.getId());

    HashSet<ApiAbstractMethod> onlyInExisting = new HashSet<ApiAbstractMethod>();
    HashSet<ApiAbstractMethod> onlyInNew = new HashSet<ApiAbstractMethod>();
    HashSet<String> commonSignature = new HashSet<String>();
    Iterator<String> intersectionNames = intersection.iterator();

    while (intersectionNames.hasNext()) {
      String tempName = intersectionNames.next();
      HashSet<ApiAbstractMethod> methodsInNew = newClass.getApiMethodsByName(
          tempName, methodType);
      HashSet<ApiAbstractMethod> methodsInExisting = oldClass.getApiMethodsByName(
          tempName, methodType);
      onlyInNew.addAll(methodsInNew);
      onlyInExisting.addAll(methodsInExisting);
      if (isIncompatibileDueToMethodOverloading(methodsInNew, methodsInExisting)) {
        addProperty(
            intersectingElements,
            methodsInExisting.toArray(new ApiAbstractMethod[0])[0].getMethodObject(),
            new ApiChange(ApiChange.Status.OVERLOADED,
                "Many methods in the new API with similar signatures. Methods = "
                    + methodsInNew
                    + " This might break API source compatibility"));
      }
      Iterator<ApiAbstractMethod> iterator1 = methodsInExisting.iterator();
      // We want to find out which method calls that the current API supports
      // will succeed even with the new API. Determine this by iterating over
      // the methods of the current API
      while (iterator1.hasNext()) {
        ApiAbstractMethod methodInExisting = iterator1.next();
        Iterator<ApiAbstractMethod> iterator2 = methodsInNew.iterator();
        while (iterator2.hasNext()) {
          ApiAbstractMethod methodInNew = iterator2.next();
          if (methodInExisting.isCompatible(methodInNew)) {
            ApiChange returnType = methodInExisting.checkReturnTypeCompatibility(methodInNew);
            if (returnType != null) {
              addProperty(intersectingElements,
                  methodInExisting.getMethodObject(), returnType);
            }
            Iterator<ApiChange> apiChangeIterator = checkExceptions(
                methodInNew.getMethodObject(),
                methodInExisting.getMethodObject()).iterator();
            while (apiChangeIterator.hasNext()) {
              addProperty(intersectingElements,
                  methodInExisting.getMethodObject(), apiChangeIterator.next());
            }
            Iterator<ApiChange.Status> apiChanges = methodInExisting.getModifierChanges(
                methodInNew).iterator();
            while (apiChanges.hasNext()) {
              addProperty(intersectingElements,
                  methodInExisting.getMethodObject(), new ApiChange(
                      apiChanges.next()));
            }
            onlyInNew.remove(methodInNew);
            onlyInExisting.remove(methodInExisting);
            String signatureInNew = methodInNew.getApiSignature();
            String signatureInExisting = methodInExisting.getApiSignature();
            if (signatureInNew.equals(signatureInExisting)) {
              commonSignature.add(signatureInNew);
              addProperty(intersectingElements,
                  methodInExisting.getMethodObject(), new ApiChange(
                      ApiChange.Status.COMPATIBLE));
            } else {
              addProperty(intersectingElements,
                  methodInExisting.getMethodObject(), new ApiChange(
                      ApiChange.Status.COMPATIBLE_WITH, " compatible with "
                          + signatureInNew));
            }
          }
        }
      }
      // printOutput(commonSignature, onlyInExisting, onlyInNew);
    }
    missingElements.addAll(getAbstractMethodObjects(onlyInExisting));
  }

  private void processFieldsInIntersection(HashSet<String> intersection) {
    if (intersection.size() == 0) {
      return;
    }
    Iterator<String> intersectionNames = intersection.iterator();

    while (intersectionNames.hasNext()) {
      String tempName = intersectionNames.next();
      JField newField = newClass.getApiFieldByName(tempName);
      JField oldField = oldClass.getApiFieldByName(tempName);
      HashSet<ApiChange.Status> apiChanges = getModifierChangesForField(
          newField, oldField);
      if (apiChanges.size() > 0) {
        intersectingFields.put(oldField, getModifierChangesForField(newField,
            oldField));
      }
    }
  }

}
