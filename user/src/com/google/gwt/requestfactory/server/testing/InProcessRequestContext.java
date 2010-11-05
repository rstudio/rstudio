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

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.impl.AbstractRequest;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestContext;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestFactory;
import com.google.gwt.requestfactory.shared.impl.RequestData;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * An in-process implementation of RequestContext
 */
class InProcessRequestContext extends AbstractRequestContext {
  static final Object[] NO_ARGS = new Object[0];

  class RequestContextHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args)
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

      // Calculate request metadata
      final String operation = method.getDeclaringClass().getName() + "::"
          + method.getName();
      final Class<?> returnType = TypeUtils.ensureBaseType(returnGenericType);
      final Class<?> elementType = Collection.class.isAssignableFrom(returnType)
          ? TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
              Collection.class, returnGenericType)) : null;

      // Create the request, just filling in the RequestData details
      AbstractRequest<Object> req = new AbstractRequest<Object>(
          InProcessRequestContext.this) {
        @Override
        protected RequestData makeRequestData() {
          return new RequestData(operation, actualArgs, propertyRefs,
              returnType, elementType);
        }
      };

      if (!isInstance) {
        // Instance invocations are enqueued when using() is called
        addInvocation(req);
      }
      return req;
    }
  };

  protected InProcessRequestContext(AbstractRequestFactory factory) {
    super(factory);
  }
}
