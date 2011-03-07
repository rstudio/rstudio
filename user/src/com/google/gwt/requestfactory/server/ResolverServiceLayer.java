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

import com.google.gwt.autobean.server.impl.TypeUtils;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.ProxyForName;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.requestfactory.shared.ServiceName;
import com.google.gwt.requestfactory.shared.ValueProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implements all of the resolution methods in ServiceLayer.
 */
final class ResolverServiceLayer extends ServiceLayerDecorator {

  private static final Logger log = Logger.getLogger(ServiceLayer.class.getName());

  /**
   * All instances of the service layer that are loaded by the same classloader
   * can use a shared validator. The use of the validator should be
   * synchronized, since it is stateful.
   */
  private static final RequestFactoryInterfaceValidator validator =
      new RequestFactoryInterfaceValidator(log,
          new RequestFactoryInterfaceValidator.ClassLoaderLoader(
              ServiceLayer.class.getClassLoader()));

  @Override
  public Class<? extends BaseProxy> resolveClass(String typeToken) {
    Class<?> found = forName(typeToken);
    if (!EntityProxy.class.isAssignableFrom(found) && !ValueProxy.class.isAssignableFrom(found)) {
      die(null, "The requested type %s is not assignable to %s or %s", typeToken,
          EntityProxy.class.getCanonicalName(), ValueProxy.class.getCanonicalName());
    }
    synchronized (validator) {
      validator.antidote();
      validator.validateProxy(found.getName());
      if (validator.isPoisoned()) {
        die(null, "The type %s did not pass RequestFactory validation", found.getCanonicalName());
      }
    }
    return found.asSubclass(BaseProxy.class);
  }

  @Override
  public <T> Class<? extends T> resolveClientType(
      Class<?> domainClass, Class<T> clientClass, boolean required) {
    String name;
    synchronized (validator) {
      name = validator.getEntityProxyTypeName(domainClass.getName(), clientClass.getName());
    }
    if (name != null) {
      return forName(name).asSubclass(clientClass);
    }
    if (List.class.isAssignableFrom(domainClass)) {
      return List.class.asSubclass(clientClass);
    }
    if (Set.class.isAssignableFrom(domainClass)) {
      return Set.class.asSubclass(clientClass);
    }
    if (TypeUtils.isValueType(domainClass)) {
      return domainClass.asSubclass(clientClass);
    }
    if (required) {
      die(null, "The domain type %s cannot be sent to the client", domainClass.getCanonicalName());
    }
    return null;
  }

  @Override
  public Class<?> resolveDomainClass(Class<?> clazz) {
    if (List.class.equals(clazz)) {
      return List.class;
    } else if (Set.class.equals(clazz)) {
      return Set.class;
    } else if (BaseProxy.class.isAssignableFrom(clazz)) {
      ProxyFor pf = clazz.getAnnotation(ProxyFor.class);
      if (pf != null) {
        return pf.value();
      }
      ProxyForName pfn = clazz.getAnnotation(ProxyForName.class);
      if (pfn != null) {
        Class<?> toReturn = forName(pfn.value());
        return toReturn;
      }
    }
    return die(
        null, "Could not resolve a domain type for client type %s", clazz.getCanonicalName());
  }

  @Override
  public Method resolveDomainMethod(Method requestContextMethod) {
    Class<?> enclosing = requestContextMethod.getDeclaringClass();

    Class<?> searchIn = null;
    Service s = enclosing.getAnnotation(Service.class);
    if (s != null) {
      searchIn = s.value();
    }
    ServiceName sn = enclosing.getAnnotation(ServiceName.class);
    if (sn != null) {
      searchIn = forName(sn.value());
    }
    if (searchIn == null) {
      die(null, "The %s type %s did not specify a service type",
          RequestContext.class.getSimpleName(), enclosing.getCanonicalName());
    }

    Class<?>[] parameterTypes = requestContextMethod.getParameterTypes();
    Class<?>[] domainArgs = new Class<?>[parameterTypes.length];
    for (int i = 0, j = domainArgs.length; i < j; i++) {
      if (BaseProxy.class.isAssignableFrom(parameterTypes[i])) {
        domainArgs[i] = getTop().resolveDomainClass(parameterTypes[i].asSubclass(BaseProxy.class));
      } else if (EntityProxyId.class.isAssignableFrom(parameterTypes[i])) {
        domainArgs[i] = TypeUtils.ensureBaseType(TypeUtils.getSingleParameterization(
            EntityProxyId.class, requestContextMethod.getGenericParameterTypes()[i]));
      } else {
        domainArgs[i] = parameterTypes[i];
      }
    }

    Throwable ex;
    try {
      return searchIn.getMethod(requestContextMethod.getName(), domainArgs);
    } catch (SecurityException e) {
      ex = e;
    } catch (NoSuchMethodException e) {
      return report("Could not locate domain method %s", requestContextMethod.getName());
    }
    return die(ex, "Could not get domain method %s in type %s", requestContextMethod.getName(),
        searchIn.getCanonicalName());
  }

  @Override
  public Method resolveRequestContextMethod(String requestContextClass, String methodName) {
    synchronized (validator) {
      validator.antidote();
      validator.validateRequestContext(requestContextClass);
      if (validator.isPoisoned()) {
        die(null, "The RequestContext type %s did not pass validation", requestContextClass);
      }
    }
    Class<?> searchIn = forName(requestContextClass);
    for (Method method : searchIn.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return report("Could not locate %s method %s::%s", RequestContext.class.getSimpleName(),
        requestContextClass, methodName);
  }

  @Override
  public String resolveTypeToken(Class<? extends BaseProxy> clazz) {
    return clazz.getName();
  }

  /**
   * Call {@link Class#forName(String)} and report any errors through
   * {@link #die()}.
   */
  private Class<?> forName(String name) {
    try {
      return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    } catch (ClassNotFoundException e) {
      return die(e, "Could not locate class %s", name);
    }
  }
}
