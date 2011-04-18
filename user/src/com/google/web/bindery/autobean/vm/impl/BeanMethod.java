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

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Breakout of method types that an AutoBean shim interface can implement. The
 * order of the values of the enum is important.
 * 
 * @see com.google.web.bindery.autobean.gwt.rebind.model.JBeanMethod
 */
public enum BeanMethod {
  /**
   * Methods defined in Object.
   */
  OBJECT {

    @Override
    public String inferName(Method method) {
      throw new UnsupportedOperationException();
    }

    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) throws Throwable {
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
    public String inferName(Method method) {
      String name = method.getName();
      if (name.startsWith(IS_PREFIX) && !method.isAnnotationPresent(PropertyName.class)) {
        Class<?> returnType = method.getReturnType();
        if (Boolean.TYPE.equals(returnType) || Boolean.class.equals(returnType)) {
          return decapitalize(name.substring(2));
        }
      }
      return super.inferName(method);
    }

    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) {
      String propertyName = inferName(method);
      Object toReturn = handler.getBean().getOrReify(propertyName);
      if (toReturn == null && method.getReturnType().isPrimitive()) {
        toReturn = TypeUtils.getDefaultPrimitiveValue(method.getReturnType());
      }
      return toReturn;
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      Class<?> returnType = method.getReturnType();
      if (method.getParameterTypes().length != 0 || Void.TYPE.equals(returnType)) {
        return false;
      }

      String name = method.getName();
      if (Boolean.TYPE.equals(returnType) || Boolean.class.equals(returnType)) {
        if (name.startsWith(IS_PREFIX) && name.length() > 2 || name.startsWith(HAS_PREFIX)
            && name.length() > 3) {
          return true;
        }
      }
      return name.startsWith(GET_PREFIX) && name.length() > 3;
    }
  },
  /**
   * Setters.
   */
  SET {
    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) {
      handler.getBean().setProperty(inferName(method), args[0]);
      return null;
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      String name = method.getName();
      return name.startsWith(SET_PREFIX) && name.length() > 3
          && method.getParameterTypes().length == 1 && method.getReturnType().equals(Void.TYPE);
    }
  },
  /**
   * A setter that returns a type assignable from the interface in which the
   * method is declared to support chained, builder-pattern setters. For
   * example, {@code foo.setBar(1).setBaz(42)}.
   */
  SET_BUILDER {
    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) {
      ProxyAutoBean<?> bean = handler.getBean();
      bean.setProperty(inferName(method), args[0]);
      return bean.as();
    }

    @Override
    boolean matches(SimpleBeanHandler<?> handler, Method method) {
      String name = method.getName();
      return name.startsWith(SET_PREFIX) && name.length() > 3
          && method.getParameterTypes().length == 1
          && method.getReturnType().isAssignableFrom(method.getDeclaringClass());
    }
  },
  /**
   * Domain methods.
   */
  CALL {
    @Override
    public String inferName(Method method) {
      throw new UnsupportedOperationException();
    }

    @Override
    Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args) throws Throwable {
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

  public static final String GET_PREFIX = "get";
  public static final String HAS_PREFIX = "has";
  public static final String IS_PREFIX = "is";
  public static final String SET_PREFIX = "set";

  private static final Object[] EMPTY_OBJECT = new Object[0];

  static Method findMethod(SimpleBeanHandler<?> handler, Method method) {
    Class<?>[] declaredParams = method.getParameterTypes();
    Class<?>[] searchParams = new Class<?>[declaredParams.length + 1];
    searchParams[0] = AutoBean.class;
    System.arraycopy(declaredParams, 0, searchParams, 1, declaredParams.length);
    Class<?> autoBeanType = handler.getBean().getType();

    for (Class<?> clazz : handler.getBean().getConfiguration().getCategories()) {
      try {
        Method found = clazz.getMethod(method.getName(), searchParams);
        if (!Modifier.isStatic(found.getModifiers())) {
          continue;
        }
        // Check the AutoBean parameterization of the 0th argument
        Class<?> foundAutoBean =
            TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(AutoBean.class, found
                .getGenericParameterTypes()[0]));
        if (!foundAutoBean.isAssignableFrom(autoBeanType)) {
          continue;
        }
        return found;
      } catch (NoSuchMethodException expected) {
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  /**
   * Private equivalent of Introspector.decapitalize(String) since
   * java.beans.Introspector is not available in Android 2.2.
   */
  private static String decapitalize(String name) {
    if (name == null) {
      return null;
    }
    int length = name.length();
    if (length == 0 || (length > 1 && Character.isUpperCase(name.charAt(1)))) {
      return name;
    }
    StringBuilder sb = new StringBuilder(length);
    sb.append(Character.toLowerCase(name.charAt(0)));
    sb.append(name.substring(1));
    return sb.toString();
  }

  public String inferName(Method method) {
    PropertyName prop = method.getAnnotation(PropertyName.class);
    if (prop != null) {
      return prop.value();
    }
    return decapitalize(method.getName().substring(3));
  }

  /**
   * Convenience method, not valid for {@link BeanMethod#CALL}.
   */
  public boolean matches(Method method) {
    return matches(null, method);
  }

  /**
   * Invoke the method.
   */
  abstract Object invoke(SimpleBeanHandler<?> handler, Method method, Object[] args)
      throws Throwable;

  /**
   * Determine if the method maches the given type.
   */
  abstract boolean matches(SimpleBeanHandler<?> handler, Method method);
}
