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
package com.google.web.bindery.autobean.vm.impl;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared code for answering question about Class objects. This is a
 * server-compatible analog to ModelUtils.
 */
public class TypeUtils {
  static final Map<Class<?>, Class<?>> AUTOBOX_MAP;
  static final Map<Class<?>, Object> DEFAULT_PRIMITIVE_VALUES;
  @SuppressWarnings("unchecked")
  static final Set<Class<?>> VALUE_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(
      Arrays.asList(Boolean.class, Character.class, Class.class, Date.class,
          Enum.class, Number.class, String.class, Void.class)));

  static {
    Map<Class<?>, Object> temp = new HashMap<Class<?>, Object>();
    temp.put(boolean.class, false);
    temp.put(byte.class, (byte) 0);
    temp.put(char.class, (char) 0);
    temp.put(double.class, (double) 0);
    temp.put(float.class, (float) 0);
    temp.put(int.class, 0);
    temp.put(long.class, (long) 0);
    temp.put(short.class, (short) 0);
    temp.put(void.class, null);

    DEFAULT_PRIMITIVE_VALUES = Collections.unmodifiableMap(temp);
  }

  static {
    Map<Class<?>, Class<?>> autoBoxMap = new HashMap<Class<?>, Class<?>>();
    autoBoxMap.put(boolean.class, Boolean.class);
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

  /**
   * Similar to ModelUtils#ensureBaseType(JType) but for the reflection API.
   */
  public static Class<?> ensureBaseType(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    }
    if (type instanceof GenericArrayType) {
      return Array.newInstance(
          ensureBaseType(((GenericArrayType) type).getGenericComponentType()),
          0).getClass();
    }
    if (type instanceof ParameterizedType) {
      return ensureBaseType(((ParameterizedType) type).getRawType());
    }
    if (type instanceof TypeVariable<?>) {
      return ensureBaseType(((TypeVariable<?>) type).getBounds()[0]);
    }
    if (type instanceof WildcardType) {
      WildcardType wild = (WildcardType) type;
      return ensureBaseType(wild.getUpperBounds()[0]);
    }
    throw new RuntimeException("Cannot handle " + type.getClass().getName());
  }

  /**
   * Given a primitive Class type, return a default value.
   */
  public static Object getDefaultPrimitiveValue(Class<?> clazz) {
    assert clazz.isPrimitive() : "Expecting primitive type";
    return DEFAULT_PRIMITIVE_VALUES.get(clazz);
  }

  public static Type[] getParameterization(Class<?> intf, Type... types) {
    for (Type type : types) {
      if (type == null) {
        continue;
      } else if (type instanceof ParameterizedType) {
        ParameterizedType param = (ParameterizedType) type;
        Type[] actualTypeArguments = param.getActualTypeArguments();
        Class<?> base = ensureBaseType(param.getRawType());
        Type[] typeParameters = base.getTypeParameters();

        Map<Type, Type> map = new HashMap<Type, Type>();
        for (int i = 0, j = typeParameters.length; i < j; i++) {
          map.put(typeParameters[i], actualTypeArguments[i]);
        }
        Type[] lookFor = intf.equals(base) ? intf.getTypeParameters()
            : getParameterization(intf, base.getGenericInterfaces());
        List<Type> toReturn = new ArrayList<Type>();
        for (int i = 0, j = lookFor.length; i < j; i++) {
          Type found = map.get(lookFor[i]);
          if (found != null) {
            toReturn.add(found);
          }
        }
        return toReturn.toArray(new Type[toReturn.size()]);
      } else if (type instanceof Class<?>) {
        Class<?> clazz = (Class<?>) type;
        if (intf.equals(clazz)) {
          return intf.getTypeParameters();
        }
        Type[] found = getParameterization(intf, clazz.getGenericSuperclass());
        if (found != null) {
          return found;
        }
        found = getParameterization(intf, clazz.getGenericInterfaces());
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  public static Type getSingleParameterization(Class<?> intf, Type... types) {
    Type[] found = getParameterization(intf, types);
    return found == null ? null : found[0];
  }

  public static boolean isValueType(Class<?> clazz) {
    if (clazz.isPrimitive() || VALUE_TYPES.contains(clazz)) {
      return true;
    }
    for (Class<?> c : VALUE_TYPES) {
      if (c.isAssignableFrom(clazz)) {
        return true;
      }
    }
    return false;
  }

  public static <V> Class<V> maybeAutobox(Class<V> domainType) {
    @SuppressWarnings("unchecked")
    Class<V> autoBoxType = (Class<V>) AUTOBOX_MAP.get(domainType);
    return autoBoxType == null ? domainType : autoBoxType;
  }

  private TypeUtils() {
  }
}
