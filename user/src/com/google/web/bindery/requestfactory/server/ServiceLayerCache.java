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

import com.google.gwt.rpc.server.Pair;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache for idempotent methods in {@link ServiceLayer}. The caching is
 * separate from {@link ReflectiveServiceLayer} so that the cache can be applied
 * to any decorators injected by the user.
 */
class ServiceLayerCache extends ServiceLayerDecorator {

  /**
   * ConcurrentHashMaps don't allow null keys or values, but sometimes we want
   * to cache a null value.
   */
  private static final Object NULL_MARKER = new Object();

  private static SoftReference<Map<Method, Map<Object, Object>>> methodCache;

  private static final Method createLocator;
  private static final Method createServiceInstance;
  private static final Method getDomainClassLoader;
  private static final Method getGetter;
  private static final Method getIdType;
  private static final Method getRequestReturnType;
  private static final Method getSetter;
  private static final Method requiresServiceLocator;
  private static final Method resolveClass;
  private static final Method resolveClientType;
  private static final Method resolveDomainClass;
  private static final Method resolveDomainMethod;
  private static final Method resolveLocator;
  private static final Method resolveRequestContext;
  private static final Method resolveRequestContextMethod;
  private static final Method resolveRequestFactory;
  private static final Method resolveServiceClass;
  private static final Method resolveServiceLocator;
  private static final Method resolveTypeToken;

  static {
    createLocator = getMethod("createLocator", Class.class);
    createServiceInstance = getMethod("createServiceInstance", Class.class);
    getDomainClassLoader = getMethod("getDomainClassLoader");
    getGetter = getMethod("getGetter", Class.class, String.class);
    getIdType = getMethod("getIdType", Class.class);
    getRequestReturnType = getMethod("getRequestReturnType", Method.class);
    getSetter = getMethod("getSetter", Class.class, String.class);
    requiresServiceLocator = getMethod("requiresServiceLocator", Method.class, Method.class);
    resolveClass = getMethod("resolveClass", String.class);
    resolveClientType = getMethod("resolveClientType", Class.class, Class.class, boolean.class);
    resolveDomainClass = getMethod("resolveDomainClass", Class.class);
    resolveDomainMethod = getMethod("resolveDomainMethod", String.class);
    resolveLocator = getMethod("resolveLocator", Class.class);
    resolveRequestContext = getMethod("resolveRequestContext", String.class);
    resolveRequestContextMethod = getMethod("resolveRequestContextMethod", String.class);
    resolveRequestFactory = getMethod("resolveRequestFactory", String.class);
    resolveServiceClass = getMethod("resolveServiceClass", Class.class);
    resolveServiceLocator = getMethod("resolveServiceLocator", Class.class);
    resolveTypeToken = getMethod("resolveTypeToken", Class.class);
  }

  private static Map<Method, Map<Object, Object>> getCache() {
    Map<Method, Map<Object, Object>> toReturn = methodCache == null ? null : methodCache.get();
    if (toReturn == null) {
      toReturn = new ConcurrentHashMap<Method, Map<Object, Object>>();
      methodCache = new SoftReference<Map<Method, Map<Object, Object>>>(toReturn);
    }
    return toReturn;
  }

  private static Method getMethod(String name, Class<?>... argTypes) {
    try {
      return ServiceLayer.class.getMethod(name, argTypes);
    } catch (SecurityException e) {
      throw new RuntimeException("Could not set up ServiceLayerCache Methods", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Could not set up ServiceLayerCache Methods", e);
    }
  }

  private final Map<Method, Map<Object, Object>> methodMap = getCache();

  @Override
  public <T extends Locator<?, ?>> T createLocator(Class<T> clazz) {
    return getOrCache(createLocator, clazz, clazz, clazz);
  }

  @Override
  public Object createServiceInstance(Class<? extends RequestContext> requestContext) {
    return getOrCache(createServiceInstance, requestContext, Object.class, requestContext);
  }

  @Override
  public ClassLoader getDomainClassLoader() {
    return getOrCache(getDomainClassLoader, NULL_MARKER, ClassLoader.class);
  }

  @Override
  public Method getGetter(Class<?> domainType, String property) {
    return getOrCache(getGetter, new Pair<Class<?>, String>(domainType, property), Method.class,
        domainType, property);
  }

  @Override
  public Class<?> getIdType(Class<?> domainType) {
    return getOrCache(getIdType, domainType, Class.class, domainType);
  }

  @Override
  public Type getRequestReturnType(Method contextMethod) {
    return getOrCache(getRequestReturnType, contextMethod, Type.class, contextMethod);
  }

  @Override
  public Method getSetter(Class<?> domainType, String property) {
    return getOrCache(getSetter, new Pair<Class<?>, String>(domainType, property), Method.class,
        domainType, property);
  }

  @Override
  public boolean requiresServiceLocator(Method contextMethod, Method domainMethod) {
    return getOrCache(requiresServiceLocator,
        new Pair<Method, Method>(contextMethod, domainMethod), Boolean.class, contextMethod,
        domainMethod);
  }

  @Override
  public Class<? extends BaseProxy> resolveClass(String typeToken) {
    Class<?> found = getOrCache(resolveClass, typeToken, Class.class, typeToken);
    return found.asSubclass(BaseProxy.class);
  }

  @Override
  public <T> Class<? extends T> resolveClientType(Class<?> domainClass, Class<T> clientType,
      boolean required) {
    Class<?> clazz =
        getOrCache(resolveClientType, new Pair<Class<?>, Class<?>>(domainClass, clientType),
            Class.class, domainClass, clientType, required);
    return clazz == null ? null : clazz.asSubclass(clientType);
  }

  @Override
  public Class<?> resolveDomainClass(Class<?> clazz) {
    return getOrCache(resolveDomainClass, clazz, Class.class, clazz);
  }

  @Override
  public Method resolveDomainMethod(String operation) {
    return getOrCache(resolveDomainMethod, operation, Method.class, operation);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends Locator<?, ?>> resolveLocator(Class<?> domainType) {
    return getOrCache(resolveLocator, domainType, Class.class, domainType);
  }

  @Override
  public Class<? extends RequestContext> resolveRequestContext(String operation) {
    Class<?> clazz = getOrCache(resolveRequestContext, operation, Class.class, operation);
    return clazz.asSubclass(RequestContext.class);
  }

  @Override
  public Method resolveRequestContextMethod(String operation) {
    return getOrCache(resolveRequestContextMethod, operation, Method.class, operation);
  }

  @Override
  public Class<? extends RequestFactory> resolveRequestFactory(String binaryName) {
    Class<?> clazz = getOrCache(resolveRequestFactory, binaryName, Class.class, binaryName);
    return clazz.asSubclass(RequestFactory.class);
  }

  @Override
  public Class<?> resolveServiceClass(Class<? extends RequestContext> requestContextClass) {
    return getOrCache(resolveServiceClass, requestContextClass, Class.class, requestContextClass);
  }

  @Override
  public Class<? extends ServiceLocator> resolveServiceLocator(
      Class<? extends RequestContext> requestContext) {
    Class<?> clazz = getOrCache(resolveServiceLocator, requestContext, Class.class, requestContext);
    return clazz == null ? null : clazz.asSubclass(ServiceLocator.class);
  }

  @Override
  public String resolveTypeToken(Class<? extends BaseProxy> domainClass) {
    return getOrCache(resolveTypeToken, domainClass, String.class, domainClass);
  }

  private <K, T> T getOrCache(Method method, K key, Class<T> valueType, Object... args) {
    Map<Object, Object> map = methodMap.get(method);
    if (map == null) {
      map = new ConcurrentHashMap<Object, Object>();
      methodMap.put(method, map);
    }
    Object raw = map.get(key);
    if (raw == NULL_MARKER) {
      return null;
    }
    T toReturn = valueType.cast(raw);
    if (toReturn == null) {
      Throwable ex = null;
      try {
        toReturn = valueType.cast(method.invoke(getNext(), args));
        map.put(key, toReturn == null ? NULL_MARKER : toReturn);
      } catch (InvocationTargetException e) {
        // The next layer threw an exception
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          // Re-throw RuntimeExceptions, which likely originate from die()
          throw ((RuntimeException) cause);
        }
        die(cause, "Unexpected checked exception");
      } catch (IllegalArgumentException e) {
        ex = e;
      } catch (IllegalAccessException e) {
        ex = e;
      }
      if (ex != null) {
        die(ex, "Bad method invocation");
      }
    }
    return toReturn;
  }
}
