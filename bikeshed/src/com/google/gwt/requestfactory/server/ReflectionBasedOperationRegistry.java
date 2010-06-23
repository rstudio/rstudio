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
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.DataTransferObject;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.valuestore.shared.Record;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * OperationRegistry which uses the operation name as a convention for
 * reflection to a method on a class, and returns an appropriate {@link
 * com.google.gwt.requestfactory.shared.RequestFactory.RequestDefinition}.
 */
public class ReflectionBasedOperationRegistry implements OperationRegistry {

  class ReflectiveRequestDefinition
      implements RequestFactory.RequestDefinition {

    private Class<?> requestClass;

    private Method requestMethod;

    private Class<?> domainClass;

    private Method domainMethod;

    public ReflectiveRequestDefinition(Class<?> requestClass,
        Method requestMethod, Class<?> domainClass, Method domainMethod) {
      this.requestClass = requestClass;
      this.requestMethod = requestMethod;
      this.domainClass = domainClass;
      this.domainMethod = domainMethod;
    }

    public String getDomainClassName() {
      return domainClass.getCanonicalName();
    }

    public String getDomainMethodName() {
      return domainMethod.getName();
    }

    public Class<?>[] getParameterTypes() {
      return domainMethod.getParameterTypes();
    }

    public Class<?> getReturnType() {
      Class<?> domainReturnType = getReturnTypeFromParameter(domainMethod,
          domainMethod.getGenericReturnType());
      Class<?> requestReturnType = getReturnTypeFromParameter(requestMethod,
          requestMethod.getGenericReturnType());
      if (Record.class.isAssignableFrom(requestReturnType)) {
        DataTransferObject annotation =
            requestReturnType.getAnnotation(DataTransferObject.class);
        if (annotation != null) {
          Class<?> dtoClass = annotation.value();
          if (!dtoClass.equals(domainReturnType)) {
            throw new IllegalArgumentException(
                "Type mismatch between " + domainMethod + " return type, and "
                    + requestReturnType + "'s DataTransferObject annotation "
                    + dtoClass);
          }
        } else {
          throw new IllegalArgumentException(
              "Missing DataTransferObject " + "annotation on record type "
                  + requestReturnType);
        }
        return requestReturnType;
      }
      // primitive ?
      return requestReturnType;
    }

    public boolean isReturnTypeList() {
      return List.class.isAssignableFrom(domainMethod.getReturnType());
    }

    public String name() {
      return requestClass.getCanonicalName() + SCOPE_SEPARATOR
          + getDomainMethodName();
    }

    private Class<?> getReturnTypeFromParameter(Method method, Type type) {
      if (type instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) type;
        Class<?> rawType = (Class<?>) pType.getRawType();
        if (List.class.isAssignableFrom(rawType)
            || RequestFactory.RequestObject.class.isAssignableFrom(rawType)) {
          Class<?> rType = getTypeArgument(pType);
          if (rType != null) {
            if (List.class.isAssignableFrom(rType)) {
              return getReturnTypeFromParameter(method, rType);
            }
            return rType;
          }
          throw new IllegalArgumentException(
              "Bad or missing type arguments for "
                  + "List return type on method " + method);
        }

      } else {
        // Primitive?
        return (Class<?>) type;
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    private Class<?> getTypeArgument(ParameterizedType type) {
      Type[] params = type.getActualTypeArguments();
      if (params.length == 1) {
        return (Class<Object>) params[0];
      }

      return null;
    }
  }

  public static final String SCOPE_SEPARATOR = "::";

  private RequestSecurityProvider securityProvider;

  public ReflectionBasedOperationRegistry(
      RequestSecurityProvider securityProvider) {
    this.securityProvider = securityProvider;
  }

  /**
   * Turns an operation in the form of package.requestClass::method into a
   * RequestDefinition via reflection.
   */
  public RequestFactory.RequestDefinition getOperation(String operationName) {
    String decodedOperationName = securityProvider.mapOperation(operationName);
    String parts[] = decodedOperationName.split(SCOPE_SEPARATOR);
    final String reqClassName = parts[0];
    final String domainMethodName = parts[1];
    try {
      // Do not invoke static initializer before checking if this class can be
      // legally invoked
      Class<?> requestClass = Class.forName(reqClassName, false,
          this.getClass().getClassLoader());
      securityProvider.checkClass(requestClass);
      Service domainClassAnnotation = requestClass.getAnnotation(Service.class);
      if (domainClassAnnotation != null) {
        Class<?> domainClass = domainClassAnnotation.value();
        Method requestMethod = findMethod(requestClass, domainMethodName);
        Method domainMethod = findMethod(domainClass, domainMethodName);
        if (requestMethod != null && domainMethod != null) {
          return new ReflectiveRequestDefinition(requestClass, requestMethod,
              domainClass, domainMethod);
        }
      }
      return null;
    } catch (ClassNotFoundException e) {
      throw new SecurityException(
          "Access to non-existent class " + reqClassName);
    }
  }

  private Method findMethod(Class<?> clazz, String methodName) {
    for (Method method : clazz.getDeclaredMethods()) {
      if ((method.getModifiers() & Modifier.PUBLIC) != 0) {
        if (method.getName().equals(methodName)) {
          return method;
        }
      }
    }
    return null;
  }
}
