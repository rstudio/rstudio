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

import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.Instance;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * OperationRegistry which uses the operation name as a convention for
 * reflection to a method on a class, and returns an appropriate {@link
 * com.google.gwt.requestfactory.server.RequestDefinition}.
 */
public class ReflectionBasedOperationRegistry implements OperationRegistry {

  class ReflectiveRequestDefinition
      implements RequestDefinition {

    private Class<?> requestClass;

    private Method requestMethod;

    private Class<?> domainClass;

    private Method domainMethod;

    private boolean isInstance;

    public ReflectiveRequestDefinition(Class<?> requestClass,
        Method requestMethod, Class<?> domainClass, Method domainMethod, boolean isInstance) {
      this.requestClass = requestClass;
      this.requestMethod = requestMethod;
      this.domainClass = domainClass;
      this.domainMethod = domainMethod;
      this.isInstance = isInstance;
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

    public Type[] getRequestParameterTypes() {
      return requestMethod.getGenericParameterTypes();
    }

    public Class<?> getReturnType() {
      Class<?> domainReturnType = getReturnTypeFromParameter(domainMethod,
          domainMethod.getGenericReturnType());
      Class<?> requestReturnType = getReturnTypeFromParameter(requestMethod,
          requestMethod.getGenericReturnType());
      if (EntityProxy.class.isAssignableFrom(requestReturnType)) {
        ProxyFor annotation =
            requestReturnType.getAnnotation(ProxyFor.class);
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

    public boolean isInstance() {
      return isInstance;
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
            || Request.class.isAssignableFrom(rawType)) {
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
        } else if (Set.class.isAssignableFrom(rawType)
            || Request.class.isAssignableFrom(rawType)) {
          Class<?> rType = getTypeArgument(pType);
          if (rType != null) {
            if (Set.class.isAssignableFrom(rType)) {
              return getReturnTypeFromParameter(method, rType);
            }
            return rType;
          }
          throw new IllegalArgumentException(
              "Bad or missing type arguments for "
                  + "Set return type on method " + method);
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
        if (params[0] instanceof ParameterizedType) {
          // if type is for example, RequestObject<List<T>> we return T
          return (Class<?>) ((ParameterizedType) params[0]).getRawType();
        }
        // else, it might be a case like List<T> in which case we return T
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
  public RequestDefinition getOperation(String operationName) {
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
          boolean isInstance = (requestMethod.getAnnotation(Instance.class) != null);
          if (isInstance == Modifier.isStatic(domainMethod.getModifiers())) {
            throw new IllegalArgumentException("domain method " + domainMethod
                + " and interface method " + requestMethod
                + " don't match wrt instance/static");
          }
          return new ReflectiveRequestDefinition(requestClass, requestMethod,
              domainClass, domainMethod, isInstance);
        }
      }
      return null;
    } catch (ClassNotFoundException e) {
      throw new SecurityException(
          "Access to non-existent class " + reqClassName);
    }
  }

  public RequestSecurityProvider getSecurityProvider() {
    return securityProvider;
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
