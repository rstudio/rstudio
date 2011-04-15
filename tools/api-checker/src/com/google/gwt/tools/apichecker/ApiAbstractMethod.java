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
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * abstract super-class for ApiMethod and ApiConstructor.
 */
abstract class ApiAbstractMethod implements Comparable<ApiAbstractMethod>, ApiElement {

  static String computeApiSignature(JAbstractMethod method) {
    String className = method.getEnclosingType().getQualifiedSourceName();
    StringBuffer sb = new StringBuffer();
    sb.append(className);
    sb.append("::");
    sb.append(computeInternalSignature(method));
    return sb.toString();
  }

  static String computeInternalSignature(JAbstractMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.append(method.getName());
    sb.append("(");
    JParameter[] params = method.getParameters();
    for (int j = 0; j < params.length; j++) {
      JParameter param = params[j];
      String typeSig = param.getType().getJNISignature();
      sb.append(typeSig);
    }
    sb.append(")");
    return sb.toString();
  }

  /*
   * 4 different signatures for a method: (a) internalSignature: just the
   * JniSignature of the arguments. does not include any class name. (b)
   * apiSignature: internalSignature, plus the className in which it was
   * declared. (c) relativeSignature: internalSignature + the current class from
   * which the Api method can be accessed. (d) coarseSignature: instead of
   * jniSignature of each parameter, just record whether the parameter is a
   * class type or a primitive type.
   */
  final ApiClass apiClass;
  String apiSignature = null;
  String internalSignature = null;
  final JAbstractMethod method;
  String relativeSignature = null;

  public ApiAbstractMethod(JAbstractMethod method, ApiClass apiClass) {
    this.method = method;
    this.apiClass = apiClass;
  }

  public int compareTo(ApiAbstractMethod other) {
    return getRelativeSignature().compareTo(other.getRelativeSignature());
  }

  /**
   * Used in set comparisons.
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof ApiAbstractMethod) {
      ApiAbstractMethod other = (ApiAbstractMethod) o;
      return getApiSignature().equals(other.getApiSignature());
    }
    return false;
  }

  public ApiClass getApiClass() {
    return apiClass;
  }

  public JAbstractMethod getMethod() {
    return method;
  }

  public String getRelativeSignature() {
    if (relativeSignature == null) {
      relativeSignature = computeRelativeSignature();
    }
    return relativeSignature;
  }

  @Override
  public int hashCode() {
    return getApiSignature().hashCode();
  }

  public boolean isCompatible(ApiAbstractMethod methodInNew) {
    JParameter[] parametersInNew = methodInNew.getMethod().getParameters();
    int length = parametersInNew.length;
    if (length != method.getParameters().length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (!ApiDiffGenerator.isFirstTypeAssignableToSecond(method.getParameters()[i].getType(),
          parametersInNew[i].getType())) {
        return false;
      }
    }
    // Control reaches here iff methods are compatible with respect to
    // parameters and return type. For source compatibility, I do not need to
    // check in which classes the methods are declared.
    return true;
  }

  public abstract boolean isOverridable();

  @Override
  public String toString() {
    return method.toString();
  }

  List<ApiChange> checkExceptionsAndReturnType(ApiAbstractMethod newMethod) {
    List<ApiChange> apiChanges = checkExceptions(newMethod);
    ApiChange returnApiChange = checkReturnTypeCompatibility(newMethod);
    if (returnApiChange == null) {
      return apiChanges;
    }
    apiChanges.add(returnApiChange);
    return apiChanges;
  }

  abstract ApiChange checkReturnTypeCompatibility(ApiAbstractMethod newMethod);

  abstract List<ApiChange> getAllChangesInApi(ApiAbstractMethod newMethod);

  String getApiSignature() {
    if (apiSignature == null) {
      apiSignature = computeApiSignature(method);
    }
    return apiSignature;
  }

  /**
   * Gets the signature of a method distinguishing just the non-primitive types
   * from the primitive types.
   * <p>
   * Useful to determine the method overloading because a null could be passed
   * for a primitive type.
   * <p>
   * Not sure if the implementation is sufficient. If need be, look at the
   * implementation below.
   * 
   * <pre>
   * public String getCoarseSignature() {
   *   StringBuffer returnStr = new StringBuffer();
   *   JParameter[] parameters = method.getParameters();
   *   JArrayType jat = null;
   *   JType type = parameter.getType();
   *   for (JParameter parameter : parameters) {
   *     while ((jat = type.isArray()) != null) {
   *       returnStr.append("a");
   *       type = jat.getComponentType();
   *     }
   *     if (type.isPrimitive() != null) {
   *       returnStr.append("p");
   *     } else {
   *       returnStr.append("c");
   *     }
   *     returnStr.append(";"); // to mark the end of a type
   *   }
   *   return returnStr.toString();
   * }
   * </pre>
   * 
   * @return the coarse signature as a String
   */
  String getCoarseSignature() {
    StringBuffer returnStr = new StringBuffer();
    JParameter[] parameters = method.getParameters();
    for (JParameter parameter : parameters) {
      JType type = parameter.getType();
      if (type.isPrimitive() != null) {
        returnStr.append(type.getJNISignature());
      } else {
        returnStr.append("c");
      }
      returnStr.append(";"); // to mark the end of a type
    }
    return returnStr.toString();
  }

  String getInternalSignature() {
    if (internalSignature == null) {
      internalSignature = computeInternalSignature(method);
    }
    return internalSignature;
  }

  /**
   * Find changes in modifiers. returns a possibly immutable list.
   * 
   */
  abstract List<ApiChange.Status> getModifierChanges(ApiAbstractMethod newMethod);

  private List<ApiChange> checkExceptions(ApiAbstractMethod newMethod) {
    ArrayList<JType> legalTypes = new ArrayList<JType>();

    // A throw declaration for an unchecked exception does not change the API.
    TypeOracle newTypeOracle = newMethod.getMethod().getEnclosingType().getOracle();
    JClassType errorType = newTypeOracle.findType(Error.class.getName());
    if (errorType != null) {
      legalTypes.add(errorType);
    }
    JClassType rteType = newTypeOracle.findType(RuntimeException.class.getName());
    if (rteType != null) {
      legalTypes.add(rteType);
    }

    legalTypes.addAll(Arrays.asList(getMethod().getThrows()));
    List<ApiChange> ret = new ArrayList<ApiChange>();
    for (JType newException : newMethod.getMethod().getThrows()) {
      boolean isSubclass = false;
      for (JType legalType : legalTypes) {
        if (ApiDiffGenerator.isFirstTypeAssignableToSecond(newException, legalType)) {
          isSubclass = true;
          break;
        }
      }
      if (!isSubclass) {
        ret.add(new ApiChange(this, ApiChange.Status.EXCEPTION_TYPE_ERROR,
            "unhandled exception in new code " + newException));
      }
    }
    return ret;
  }

  private String computeRelativeSignature() {
    String signature = computeInternalSignature(method);
    if (ApiCompatibilityChecker.DEBUG) {
      JClassType enclosingType = method.getEnclosingType();
      return apiClass.getClassObject().getQualifiedSourceName()
          + "::"
          + signature
          + " defined in "
          + (enclosingType == null ? "null enclosing type " : enclosingType
              .getQualifiedSourceName());
    }
    return apiClass.getClassObject().getQualifiedSourceName() + "::" + signature;
  }

}
