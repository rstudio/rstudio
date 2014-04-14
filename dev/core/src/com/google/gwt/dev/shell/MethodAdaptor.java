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
package com.google.gwt.dev.shell;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Provides a common adaptor interface for Methods and Constructors.
 */
public class MethodAdaptor {
  private final Constructor<?> constructor;
  private final Class<?> declaringClass;
  private final int hashCode;
  private final Method method;
  private final String name;
  private final boolean needsThis;
  private final Class<?>[] paramTypes;
  private final Class<?> returnType;

  public MethodAdaptor(Constructor<?> c) {
    constructor = c;
    method = null;
    name = c.getName();
    needsThis = false;
    paramTypes = c.getParameterTypes();
    returnType = declaringClass = c.getDeclaringClass();
    hashCode = c.hashCode();
  }

  public MethodAdaptor(Method m) {
    constructor = null;
    declaringClass = m.getDeclaringClass();
    method = m;
    name = m.getName();
    needsThis = !Modifier.isStatic(m.getModifiers());
    paramTypes = m.getParameterTypes();
    returnType = m.getReturnType();
    hashCode = method.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MethodAdaptor)) {
      return false;
    }

    MethodAdaptor other = (MethodAdaptor) o;
    if (constructor != null) {
      return constructor.equals(other.constructor);
    } else if (method != null) {
      return method.equals(other.method);
    } else {
      throw new RuntimeException("constructor and method are null");
    }
  }

  public Class<?> getDeclaringClass() {
    return declaringClass;
  }

  public String getName() {
    return name;
  }

  public Class<?>[] getParameterTypes() {
    return paramTypes;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public AccessibleObject getUnderlyingObject() {
    return (method != null) ? method : constructor;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public Object invoke(Object obj, Object... args)
      throws InstantiationException, InvocationTargetException,
      IllegalAccessException {
    if (method != null) {
      return method.invoke(obj, args);
    } else if (constructor != null) {
      return constructor.newInstance(args);
    } else {
      throw new RuntimeException("Nothing to invoke");
    }
  }

  /**
   * Indicates whether or not a "this" object is required to invoke the Method
   * or Constructor.
   */
  public boolean needsThis() {
    return needsThis;
  }

  @Override
  public final String toString() {
    return (method != null) ? method.toString() : constructor.toString();
  }
}
