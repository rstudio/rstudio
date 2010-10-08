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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.collect.HashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for common model methods.
 */
public class ModelUtils {

  static final Map<Class<?>, Class<?>> AUTOBOX_MAP;

  static final Set<String> VALUE_TYPES = Collections.unmodifiableSet(new HashSet<String>(
      Arrays.asList(Boolean.class.getName(), Character.class.getName(),
          Class.class.getName(), Date.class.getName(), Enum.class.getName(),
          Number.class.getName(), String.class.getName(), Void.class.getName())));

  static {
    Map<Class<?>, Class<?>> autoBoxMap = new HashMap<Class<?>, Class<?>>();
    autoBoxMap.put(byte.class, Byte.class);
    autoBoxMap.put(char.class, Character.class);
    autoBoxMap.put(double.class, Double.class);
    autoBoxMap.put(float.class, Float.class);
    autoBoxMap.put(int.class, Integer.class);
    autoBoxMap.put(long.class, Long.class);
    autoBoxMap.put(short.class, Short.class);
    autoBoxMap.put(void.class, Void.class);
    AUTOBOX_MAP = Collections.unmodifiableMap(autoBoxMap);
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

  public static boolean isValueType(TypeOracle oracle, JType type) {
    JClassType classType = type.isClassOrInterface();
    if (classType == null) {
      return true;
    }

    for (String valueType : VALUE_TYPES) {
      JClassType found = oracle.findType(valueType);
      // null check to accommodate limited mock CompilationStates
      if (found != null && found.isAssignableFrom(classType)) {
        return true;
      }
    }
    return false;
  }

  public static Class<?> maybeAutobox(Class<?> domainType) {
    Class<?> autoBoxType = AUTOBOX_MAP.get(domainType);
    return autoBoxType == null ? domainType : autoBoxType;
  }

  private ModelUtils() {
  }
}
