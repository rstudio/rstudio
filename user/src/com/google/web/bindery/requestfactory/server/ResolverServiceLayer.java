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
package com.google.web.bindery.requestfactory.server;

import com.google.gwt.dev.asm.Type;
import com.google.web.bindery.autobean.vm.impl.TypeUtils;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceName;
import com.google.web.bindery.requestfactory.vm.impl.Deobfuscator;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Implements all of the resolution methods in ServiceLayer.
 */
final class ResolverServiceLayer extends ServiceLayerDecorator {

  private static Deobfuscator deobfuscator;

  private static synchronized void updateDeobfuscator(Class<? extends RequestFactory> clazz,
      ClassLoader resolveClassesWith) {
    Deobfuscator.Builder builder = Deobfuscator.Builder.load(clazz, resolveClassesWith);
    if (deobfuscator != null) {
      builder.merge(deobfuscator);
    }
    deobfuscator = builder.build();
  }

  @Override
  public ClassLoader getDomainClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public Class<? extends BaseProxy> resolveClass(String typeToken) {
    String deobfuscated = deobfuscator.getTypeFromToken(typeToken);
    if (deobfuscated == null) {
      die(null, "No type for token %s", typeToken);
    }

    return forName(deobfuscated).asSubclass(BaseProxy.class);
  }

  @Override
  public <T> Class<? extends T> resolveClientType(Class<?> domainClass, Class<T> clientClass,
      boolean required) {
    if (List.class.isAssignableFrom(domainClass)) {
      return List.class.asSubclass(clientClass);
    }
    if (Set.class.isAssignableFrom(domainClass)) {
      return Set.class.asSubclass(clientClass);
    }
    if (TypeUtils.isValueType(domainClass)) {
      return domainClass.asSubclass(clientClass);
    }
    Class<?> toSearch = domainClass;
    while (toSearch != null) {
      List<String> clientTypes = deobfuscator.getClientProxies(toSearch.getName());
      if (clientTypes != null) {
        for (String clientType : clientTypes) {
          Class<?> proxy = forName(clientType);
          if (clientClass.isAssignableFrom(proxy)) {
            return proxy.asSubclass(clientClass);
          }
        }
      }
      toSearch = toSearch.getSuperclass();
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
    return die(null, "Could not resolve a domain type for client type %s", clazz.getCanonicalName());
  }

  @Override
  public Method resolveDomainMethod(String operation) {
    /*
     * The validator has already determined the mapping from the RequsetContext
     * method to a domain method signature. We'll reuse this calculation instead
     * of iterating over all methods.
     */
    String domainDescriptor = deobfuscator.getDomainMethodDescriptor(operation);

    if (domainDescriptor == null) {
      return die(null, "No domain method descriptor is mapped to operation %s", operation);
    }

    Class<?>[] domainArgs = getArgumentTypes(domainDescriptor);
    Class<? extends RequestContext> requestContext = getTop().resolveRequestContext(operation);
    Class<?> serviceImplementation = getTop().resolveServiceClass(requestContext);

    // Request<FooProxy> someMethod(int a, double b, FooProxy c);
    Method requestContextMethod = getTop().resolveRequestContextMethod(operation);

    Throwable ex;
    try {
      return serviceImplementation.getMethod(requestContextMethod.getName(), domainArgs);
    } catch (SecurityException e) {
      ex = e;
    } catch (NoSuchMethodException e) {
      ex = e;
    }

    return die(ex,
        "Could not find method in implementation %s matching descriptor %s for operation %s",
        serviceImplementation.getCanonicalName(), domainDescriptor, operation);
  }

  @Override
  public Class<? extends RequestContext> resolveRequestContext(String operation) {
    String requestContextClass = deobfuscator.getRequestContext(operation);
    if (requestContextClass == null) {
      die(null, "No RequestContext for operation %s", operation);
    }
    return forName(requestContextClass).asSubclass(RequestContext.class);
  }

  @Override
  public Method resolveRequestContextMethod(String operation) {
    Class<?> searchIn = getTop().resolveRequestContext(operation);
    String methodName = deobfuscator.getRequestContextMethodName(operation);
    String descriptor = deobfuscator.getRequestContextMethodDescriptor(operation);
    Class<?>[] params = getArgumentTypes(descriptor);
    try {
      return searchIn.getMethod(methodName, params);
    } catch (NoSuchMethodException ex) {
      return report("Could not locate %s operation %s", RequestContext.class.getSimpleName(),
          operation);
    }
  }

  @Override
  public Class<? extends RequestFactory> resolveRequestFactory(String binaryName) {
    Class<? extends RequestFactory> toReturn = forName(binaryName).asSubclass(RequestFactory.class);
    updateDeobfuscator(toReturn, getTop().getDomainClassLoader());
    return toReturn;
  }

  @Override
  public Class<?> resolveServiceClass(Class<? extends RequestContext> requestContextClass) {
    Class<?> searchIn = null;
    Service s = requestContextClass.getAnnotation(Service.class);
    if (s != null) {
      searchIn = s.value();
    }
    ServiceName sn = requestContextClass.getAnnotation(ServiceName.class);
    if (sn != null) {
      searchIn = forName(sn.value());
    }
    if (searchIn == null) {
      die(null, "The %s type %s did not specify a service type", RequestContext.class
          .getSimpleName(), requestContextClass.getCanonicalName());
    }
    return searchIn;
  }

  @Override
  public String resolveTypeToken(Class<? extends BaseProxy> clazz) {
    return OperationKey.hash(clazz.getName());
  }

  /**
   * Call {@link Class#forName(String)} and report any errors through
   * {@link #die()}.
   */
  private Class<?> forName(String name) {
    try {
      return Class.forName(name, false, getTop().getDomainClassLoader());
    } catch (ClassNotFoundException e) {
      return die(e, "Could not locate class %s", name);
    }
  }

  private Class<?>[] getArgumentTypes(String descriptor) {
    Type[] types = Type.getArgumentTypes(descriptor);
    Class<?>[] params = new Class<?>[types.length];
    for (int i = 0, j = types.length; i < j; i++) {
      params[i] = getClass(types[i]);
    }
    return params;
  }

  private Class<?> getClass(Type type) {
    switch (type.getSort()) {
      case Type.BOOLEAN:
        return boolean.class;
      case Type.BYTE:
        return byte.class;
      case Type.CHAR:
        return char.class;
      case Type.DOUBLE:
        return double.class;
      case Type.FLOAT:
        return float.class;
      case Type.INT:
        return int.class;
      case Type.LONG:
        return long.class;
      case Type.OBJECT:
        return forName(type.getClassName());
      case Type.SHORT:
        return short.class;
      case Type.VOID:
        return void.class;
      case Type.ARRAY:
        return die(null, "Unsupported Type used in operation descriptor %s", type.getDescriptor());
      default:
        // Error in this switch statement
        return die(null, "Unhandled Type: %s", type.getDescriptor());
    }
  }
}
