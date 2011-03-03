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
package com.google.gwt.requestfactory.server.testing;

import com.google.gwt.autobean.server.impl.BeanMethod;
import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.autobean.shared.AutoBean.PropertyName;
import com.google.gwt.autobean.shared.AutoBeanFactory;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.JsonRpcContent;
import com.google.gwt.requestfactory.shared.JsonRpcWireName;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.impl.AbstractRequest;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestContext;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestFactory;
import com.google.gwt.requestfactory.shared.impl.RequestData;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * An in-process implementation of RequestContext
 */
class InProcessRequestContext extends AbstractRequestContext {
  class RequestContextHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, final Object[] args)
        throws Throwable {
      // Maybe delegate to superclass
      Class<?> owner = method.getDeclaringClass();
      if (Object.class.equals(owner) || RequestContext.class.equals(owner)
          || AbstractRequestContext.class.equals(owner)) {
        try {
          return method.invoke(InProcessRequestContext.this, args);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }

      /*
       * Instance methods treat the 0-th argument as the instance on which to
       * invoke the method.
       */
      final Object[] actualArgs;
      Type returnGenericType;
      boolean isInstance = InstanceRequest.class.isAssignableFrom(method.getReturnType());
      if (isInstance) {
        returnGenericType = TypeUtils.getParameterization(
            InstanceRequest.class, method.getGenericReturnType(),
            method.getReturnType())[1];
        if (args == null) {
          actualArgs = new Object[1];
        } else {
          // Save a slot for the this argument
          actualArgs = new Object[args.length + 1];
          System.arraycopy(args, 0, actualArgs, 1, args.length);
        }
      } else {
        returnGenericType = TypeUtils.getSingleParameterization(Request.class,
            method.getGenericReturnType(), method.getReturnType());
        if (args == null) {
          actualArgs = NO_ARGS;
        } else {
          actualArgs = args;
        }
      }

      Class<?> returnType = TypeUtils.ensureBaseType(returnGenericType);
      Class<?> elementType = Collection.class.isAssignableFrom(returnType)
          ? TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
              Collection.class, returnGenericType)) : null;

      final RequestData data;
      if (dialect.equals(Dialect.STANDARD)) {
        String operation = method.getDeclaringClass().getName() + "::"
            + method.getName();

        data = new RequestData(operation, actualArgs, returnType, elementType);
      } else {
        // Calculate request metadata
        JsonRpcWireName wireInfo = method.getReturnType().getAnnotation(
            JsonRpcWireName.class);
        String apiVersion = wireInfo.version();
        String operation = wireInfo.value();

        int foundContent = -1;
        final String[] parameterNames = args == null ? new String[0]
            : new String[args.length];
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        parameter : for (int i = 0, j = parameterAnnotations.length; i < j; i++) {
          for (Annotation annotation : parameterAnnotations[i]) {
            if (PropertyName.class.equals(annotation.annotationType())) {
              parameterNames[i] = ((PropertyName) annotation).value();
              continue parameter;
            } else if (JsonRpcContent.class.equals(annotation.annotationType())) {
              foundContent = i;
              continue parameter;
            }
          }
          throw new UnsupportedOperationException("No "
              + PropertyName.class.getCanonicalName()
              + " annotation on parameter " + i + " of method "
              + method.toString());
        }
        final int contentIdx = foundContent;

        data = new RequestData(operation, actualArgs, returnType, elementType);
        for (int i = 0, j = args.length; i < j; i++) {
          if (i != contentIdx) {
            data.setNamedParameter(parameterNames[i], args[i]);
          } else {
            data.setRequestContent(args[i]);
          }
          data.setApiVersion(apiVersion);
        }
      }

      // Create the request, just filling in the RequestData details
      final AbstractRequest<Object> req = new AbstractRequest<Object>(
          InProcessRequestContext.this) {
        @Override
        protected RequestData makeRequestData() {
          data.setPropertyRefs(propertyRefs);
          return data;
        }
      };

      if (!isInstance) {
        // Instance invocations are enqueued when using() is called
        addInvocation(req);
      }

      if (dialect.equals(Dialect.STANDARD)) {
        return req;
      } else if (dialect.equals(Dialect.JSON_RPC)) {
        // Support optional parameters for JSON-RPC payloads
        Class<?> requestType = method.getReturnType().asSubclass(Request.class);
        return Proxy.newProxyInstance(requestType.getClassLoader(),
            new Class<?>[] {requestType}, new InvocationHandler() {
              public Object invoke(Object proxy, Method method, Object[] args)
                  throws Throwable {
                if (Object.class.equals(method.getDeclaringClass())
                    || Request.class.equals(method.getDeclaringClass())) {
                  return method.invoke(req, args);
                } else if (BeanMethod.SET.matches(method)
                    || BeanMethod.SET_BUILDER.matches(method)) {
                  req.getRequestData().setNamedParameter(
                      BeanMethod.SET.inferName(method), args[0]);
                  return Void.TYPE.equals(method.getReturnType()) ? null
                      : proxy;
                }
                throw new UnsupportedOperationException(method.toString());
              }
            });
      } else {
        throw new RuntimeException("Should not reach here");
      }
    }
  }

  static final Object[] NO_ARGS = new Object[0];
  private final Dialect dialect;

  protected InProcessRequestContext(AbstractRequestFactory factory,
      Dialect dialect) {
    super(factory, dialect);
    this.dialect = dialect;
  }

  @Override
  protected AutoBeanFactory getAutoBeanFactory() {
    return ((InProcessRequestFactory) getRequestFactory()).getAutoBeanFactory();
  }
}
