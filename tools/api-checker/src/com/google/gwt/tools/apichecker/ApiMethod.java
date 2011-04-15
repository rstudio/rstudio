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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates an API method. Useful for set-operations.
 */
final class ApiMethod extends ApiAbstractMethod {

  ApiMethod(JAbstractMethod method, ApiClass apiClass) {
    super(method, apiClass);
  }

  @Override
  public boolean isOverridable() {
    JMethod methodType = (JMethod) method;
    if (methodType.isStatic() || methodType.isFinal()) {
      return false;
    }
    return apiClass.isSubclassableApiClass();
  }

  @Override
  ApiChange checkReturnTypeCompatibility(ApiAbstractMethod newMethod)
      throws TypeNotPresentException {
    JType firstType, secondType;
    if (newMethod.getMethod() instanceof JMethod && method instanceof JMethod) {
      firstType = ((JMethod) method).getReturnType();
      secondType = ((JMethod) newMethod.getMethod()).getReturnType();
    } else {
      throw new AssertionError("Different types for method = " + method.getClass()
          + ", and newMethodObject = " + newMethod.getMethod().getClass() + ", signature = "
          + getApiSignature());
    }
    StringBuffer sb = new StringBuffer();
    if (firstType.getSimpleSourceName().indexOf("void") != -1) {
      return null;
    }
    boolean compatible = ApiDiffGenerator.isFirstTypeAssignableToSecond(secondType, firstType);
    if (compatible) {
      return null;
    }
    sb.append(" from ");
    sb.append(firstType.getQualifiedSourceName());
    sb.append(" to ");
    sb.append(secondType.getQualifiedSourceName());
    return new ApiChange(this, ApiChange.Status.RETURN_TYPE_ERROR, sb.toString());
  }

  /**
   * check for changes in: (i) argument types, (ii) return type, (iii)
   * exceptions thrown. use getJNISignature() for type equality, it does type
   * erasure
   */
  @Override
  List<ApiChange> getAllChangesInApi(ApiAbstractMethod newApiMethod) {
    if (!(newApiMethod.getMethod() instanceof JMethod)) {
      return Collections.emptyList();
    }
    List<ApiChange> changeApis = new ArrayList<ApiChange>();
    JMethod existingMethod = (JMethod) method;
    JMethod newMethod = (JMethod) newApiMethod.getMethod();
    // check return type
    if (!existingMethod.getReturnType().getJNISignature().equals(
        newMethod.getReturnType().getJNISignature())) {
      changeApis.add(new ApiChange(this, ApiChange.Status.OVERRIDABLE_METHOD_RETURN_TYPE_CHANGE,
          " from " + existingMethod.getReturnType() + " to " + newMethod.getReturnType()));
    }
    // check argument type
    JParameter[] newParametersList = newMethod.getParameters();
    JParameter[] existingParametersList = existingMethod.getParameters();
    if (newParametersList.length != existingParametersList.length) {
      changeApis.add(new ApiChange(this, ApiChange.Status.OVERRIDABLE_METHOD_ARGUMENT_TYPE_CHANGE,
          "number of parameters changed"));
    } else {
      int length = newParametersList.length;
      for (int i = 0; i < length; i++) {
        if (!existingParametersList[i].getType().getJNISignature().equals(
            newParametersList[i].getType().getJNISignature())) {
          changeApis.add(new ApiChange(this,
              ApiChange.Status.OVERRIDABLE_METHOD_ARGUMENT_TYPE_CHANGE, " at position " + i
                  + " from " + existingParametersList[i].getType() + " to "
                  + newParametersList[i].getType()));
        }
      }
    }

    // check exceptions
    Set<String> newExceptionsSet = new HashSet<String>();
    Map<String, JType> newExceptionsMap = new HashMap<String, JType>();
    for (JType newType : newMethod.getThrows()) {
      String jniSignature = newType.getJNISignature();
      newExceptionsMap.put(jniSignature, newType);
      newExceptionsSet.add(jniSignature);
    }

    Set<String> existingExceptionsSet = new HashSet<String>();
    Map<String, JType> existingExceptionsMap = new HashMap<String, JType>();
    for (JType existingType : existingMethod.getThrows()) {
      String jniSignature = existingType.getJNISignature();
      existingExceptionsMap.put(jniSignature, existingType);
      existingExceptionsSet.add(jniSignature);
    }
    ApiDiffGenerator.removeIntersection(existingExceptionsSet, newExceptionsSet);
    removeUncheckedExceptions(newMethod, newExceptionsSet, newExceptionsMap);
    removeUncheckedExceptions(existingMethod, existingExceptionsSet, existingExceptionsMap);
    if (existingExceptionsSet.size() > 0) {
      changeApis.add(new ApiChange(this, ApiChange.Status.OVERRIDABLE_METHOD_EXCEPTION_TYPE_CHANGE,
          "existing method had more exceptions: " + existingExceptionsSet));
    }
    if (newExceptionsSet.size() > 0) {
      changeApis.add(new ApiChange(this, ApiChange.Status.OVERRIDABLE_METHOD_EXCEPTION_TYPE_CHANGE,
          "new method has more exceptions: " + newExceptionsSet));
    }
    return changeApis;
  }

  /*
   * check for: (i) added 'final' or 'abstract', (ii) removed 'static', adding
   * the 'static' keyword is fine.
   * 
   * A private, static, or final method can't be made 'abstract' (Java language
   * specification).
   */
  @Override
  List<ApiChange.Status> getModifierChanges(final ApiAbstractMethod newMethod) {
    JMethod newjmethod = null;
    JMethod oldjmethod = null;

    if (newMethod.getMethod() instanceof JMethod && method instanceof JMethod) {
      newjmethod = (JMethod) newMethod.getMethod();
      oldjmethod = (JMethod) method;
    } else {
      throw new AssertionError("Different types for method = " + method.getClass()
          + " and newMethod = " + newMethod.getMethod().getClass() + ", signature = "
          + getApiSignature());
    }
    List<ApiChange.Status> statuses = new ArrayList<ApiChange.Status>();
    if (!oldjmethod.isFinal() && !apiClass.getClassObject().isFinal() && newjmethod.isFinal()) {
      statuses.add(ApiChange.Status.FINAL_ADDED);
    }
    if (!oldjmethod.isAbstract() && newjmethod.isAbstract()) {
      statuses.add(ApiChange.Status.ABSTRACT_ADDED);
    }
    if ((oldjmethod.isStatic() && !newjmethod.isStatic())) {
      statuses.add(ApiChange.Status.STATIC_REMOVED);
    }
    return statuses;
  }

  // remove Error.class, RuntimeException.class, and their sub-classes
  private void removeUncheckedExceptions(JMethod method, Set<String> exceptionsSet,
      Map<String, JType> exceptionsMap) {
    if (exceptionsSet.size() == 0) {
      return;
    }
    TypeOracle typeOracle = method.getEnclosingType().getOracle();
    JClassType errorType = typeOracle.findType(Error.class.getName());
    JClassType rteType = typeOracle.findType(RuntimeException.class.getName());
    Set<String> exceptionsToRemove = new HashSet<String>();
    for (String exceptionString : exceptionsSet) {
      JType exception = exceptionsMap.get(exceptionString);
      assert (exception != null);
      boolean remove =
          (errorType != null && ApiDiffGenerator
              .isFirstTypeAssignableToSecond(exception, errorType))
              || (rteType != null && ApiDiffGenerator.isFirstTypeAssignableToSecond(exception,
                  rteType));
      if (remove) {
        exceptionsToRemove.add(exceptionString);
      }
    }
    exceptionsSet.removeAll(exceptionsToRemove);
  }
}
/*
 * final class TestB { static protected int i = 5; }
 * 
 * class TestC { public int j = TestB.i + 10; }
 */
