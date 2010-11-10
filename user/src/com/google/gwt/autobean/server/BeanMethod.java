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
package com.google.gwt.autobean.server;

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.AutoBean;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Breakout of method types that an AutoBean shim interface can implement. The
 * order of the values of the enum is important.
 */
enum BeanMethod {
  /**
   * Methods defined in Object.
   */
  OBJECT {
    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args)
        throws Throwable {
      if (CALL.matches(handler, method)) {
        return CALL.invoke(handler, method, args);
      }
      return method.invoke(handler, args);
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      return method.getDeclaringClass().equals(Object.class);
    }
  },
  /**
   * Getters.
   */
  GET {
    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) {
      String propertyName;
      String name = method.getName();
      if (Boolean.TYPE.equals(method.getReturnType()) && name.startsWith("is")) {
        propertyName = name.substring(2);
      } else {
        // A regular getter or a boolean hasFoo()
        propertyName = name.substring(3);
      }
      Object toReturn = handler.getBean().getValues().get(propertyName);
      if (toReturn == null && method.getReturnType().isPrimitive()) {
        toReturn = TypeUtils.getDefaultPrimitiveValue(method.getReturnType());
      }
      return toReturn;
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      Class<?> returnType = method.getReturnType();
      if (method.getParameterTypes().length != 0
          || Void.TYPE.equals(returnType)) {
        return false;
      }

      String name = method.getName();
      if (Boolean.TYPE.equals(returnType)) {
        if (name.startsWith("is") && name.length() > 2
            || name.startsWith("has") && name.length() > 3) {
          return true;
        }
      }
      return name.startsWith("get") && name.length() > 3;
    }
  },
  /**
   * Setters.
   */
  SET {
    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) {
      handler.getBean().getValues().put(method.getName().substring(3), args[0]);
      return null;
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      String name = method.getName();
      return name.startsWith("set") && name.length() > 3
          && method.getParameterTypes().length == 1
          && method.getReturnType().equals(Void.TYPE);
    }
  },
  /**
   * Domain methods.
   */
  CALL {
    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args)
        throws Throwable {
      if (args == null) {
        args = EMPTY_OBJECT;
      }

      Method found = findMethod(handler, method);
      if (found != null) {
        Object[] realArgs = new Object[args.length + 1];
        realArgs[0] = handler.getBean();
        System.arraycopy(args, 0, realArgs, 1, args.length);
        return found.invoke(null, realArgs);
      }
      throw new RuntimeException("Could not find category implementation of "
          + method.toGenericString());
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      return handler.getBean().isWrapper()
          || !handler.getBean().getConfiguration().getCategories().isEmpty()
          && findMethod(handler, method) != null;
    }
  };

  private static final Object[] EMPTY_OBJECT = new Object[0];

  static Method findMethod(SimpleBeanHandler<?> handler, Method method) {
    Class<?>[] declaredParams = method.getParameterTypes();
    Class<?>[] searchParams = new Class<?>[declaredParams.length + 1];
    searchParams[0] = AutoBean.class;
    System.arraycopy(declaredParams, 0, searchParams, 1, declaredParams.length);

    for (Class<?> clazz : handler.getBean().getConfiguration().getCategories()) {
      try {
        Method found = clazz.getMethod(method.getName(), searchParams);
        if (Modifier.isStatic(found.getModifiers())) {
          return found;
        }
      } catch (NoSuchMethodException expected) {
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  /**
   * Invoke the method.
   */
  abstract Object invoke(SimpleBeanHandler<?> handler, Method method,
      Object[] args) throws Throwable;

  /**
   * Convenience method, not valid for {@link BeanMethod#CALL}.
   */
  boolean matches(Method method) {
    return matches(null, method);
  }

  /**
   * Determine if the method maches the given type.
   */
  abstract boolean matches(SimpleBeanHandler<?> handler, Method method);
}