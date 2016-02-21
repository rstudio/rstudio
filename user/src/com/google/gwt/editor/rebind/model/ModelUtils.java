/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.editor.rebind.model;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.web.bindery.autobean.shared.ValueCodex;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for common model methods.
 */
public class ModelUtils {

  static final Set<String> VALUE_TYPE_NAMES;

  static {
    Set<Class<?>> valueTypes = ValueCodex.getAllValueTypes();
    Set<String> names = new HashSet<String>(valueTypes.size());
    for (Class<?> clazz : valueTypes) {
      names.add(clazz.getName());
    }
    VALUE_TYPE_NAMES = Collections.unmodifiableSet(names);
  }

  @SuppressWarnings("unchecked")
  public static <T extends JType> T ensureBaseType(T maybeParameterized) {
    if (maybeParameterized.isArray() != null) {
      JArrayType array = maybeParameterized.isArray();
      return (T) array.getOracle().getArrayType(
          ensureBaseType(array.getComponentType()));
    }
    if (maybeParameterized.isTypeParameter() != null) {
      return (T) maybeParameterized.isTypeParameter().getBaseType();
    }
    if (maybeParameterized.isParameterized() != null) {
      return (T) maybeParameterized.isParameterized().getBaseType();
    }
    if (maybeParameterized.isRawType() != null) {
      return (T) maybeParameterized.isRawType().getBaseType();
    }
    if (maybeParameterized.isWildcard() != null) {
      return ensureBaseType((T) maybeParameterized.isWildcard().getBaseType());
    }
    return maybeParameterized;
  }

  public static JClassType[] findParameterizationOf(JClassType intfType,
      JClassType subType) {
    assert intfType.isAssignableFrom(subType) : subType.getParameterizedQualifiedSourceName()
        + " is not assignable to "
        + subType.getParameterizedQualifiedSourceName();

    for (JClassType supertype : subType.getFlattenedSupertypeHierarchy()) {
      JParameterizedType parameterized = supertype.isParameterized();
      if (parameterized != null) {
        // Found the desired supertype
        if (intfType.equals(parameterized.getBaseType())) {
          return parameterized.getTypeArgs();
        }
      }
    }
    return null;
  }

  /**
   * Given a JType, return the binary name of the class that is most proximately
   * assignable to the type. This method will resolve type parameters as well as
   * wildcard types.
   */
  public static String getQualifiedBaseBinaryName(JType type) {
    return ensureBaseType(type).getErasedType().getQualifiedBinaryName();
  }

  /**
   * Given a JType, return the source name of the class that is most proximately
   * assignable to the type. This method will resolve type parameters as well as
   * wildcard types.
   */
  public static String getQualifiedBaseSourceName(JType type) {
    return ensureBaseType(type).getErasedType().getQualifiedSourceName();
  }

  public static boolean isValueType(TypeOracle oracle, JType type) {
    JClassType classType = type.isClassOrInterface();
    if (classType == null) {
      return true;
    }
    if (type.isEnum() != null) {
      return true;
    }

    for (String valueType : VALUE_TYPE_NAMES) {
      JClassType found = oracle.findType(valueType);
      // null check to accommodate limited mock CompilationStates
      if (found != null && found.equals(classType)) {
        return true;
      }
    }
    return false;
  }

  private ModelUtils() {
  }
}
