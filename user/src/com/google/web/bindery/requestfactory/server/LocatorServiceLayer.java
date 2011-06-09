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

import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;
import com.google.web.bindery.requestfactory.shared.ServiceName;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Adds support to the ServiceLayer chain for using {@link Locator} and
 * {@link ServiceLocator} helper objects.
 */
final class LocatorServiceLayer extends ServiceLayerDecorator {

  @Override
  public <T> T createDomainObject(Class<T> clazz) {
    Locator<T, ?> l = getLocator(clazz);
    if (l == null) {
      return super.createDomainObject(clazz);
    }
    return l.create(clazz);
  }

  @Override
  public <T extends Locator<?, ?>> T createLocator(Class<T> clazz) {
    return newInstance(clazz, Locator.class);
  }

  @Override
  public Object createServiceInstance(Class<? extends RequestContext> requestContext) {
    Class<? extends ServiceLocator> locatorType = getTop().resolveServiceLocator(requestContext);
    ServiceLocator locator = getTop().createServiceLocator(locatorType);
    Class<?> serviceClass = getTop().resolveServiceClass(requestContext);
    return locator.getInstance(serviceClass);
  }

  @Override
  public <T extends ServiceLocator> T createServiceLocator(Class<T> serviceLocatorType) {
    return newInstance(serviceLocatorType, ServiceLocator.class);
  }

  @Override
  public Object getId(Object domainObject) {
    return doGetId(domainObject);
  }

  @Override
  public Class<?> getIdType(Class<?> domainType) {
    Locator<?, ?> l = getLocator(domainType);
    if (l == null) {
      return super.getIdType(domainType);
    }
    return l.getIdType();
  }

  @Override
  public Object getVersion(Object domainObject) {
    return doGetVersion(domainObject);
  }

  @Override
  public boolean isLive(Object domainObject) {
    return doIsLive(domainObject);
  }

  @Override
  public <T> T loadDomainObject(Class<T> clazz, Object domainId) {
    return doLoadDomainObject(clazz, domainId);
  }

  /**
   * Returns true if the context method returns a {@link Request} and the domain
   * method is non-static.
   */
  @Override
  public boolean requiresServiceLocator(Method contextMethod, Method domainMethod) {
    return Request.class.isAssignableFrom(contextMethod.getReturnType())
        && !Modifier.isStatic(domainMethod.getModifiers());
  }

  @Override
  public Class<? extends Locator<?, ?>> resolveLocator(Class<?> domainType) {
    // Find the matching BaseProxy
    Class<?> proxyType = getTop().resolveClientType(domainType, BaseProxy.class, false);
    if (proxyType == null) {
      return null;
    }

    // Check it for annotations
    Class<? extends Locator<?, ?>> locatorType;
    ProxyFor l = proxyType.getAnnotation(ProxyFor.class);
    ProxyForName ln = proxyType.getAnnotation(ProxyForName.class);
    if (l != null && !Locator.class.equals(l.locator())) {
      @SuppressWarnings("unchecked")
      Class<? extends Locator<?, ?>> found = (Class<? extends Locator<?, ?>>) l.locator();
      locatorType = found;
    } else if (ln != null && ln.locator().length() > 0) {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends Locator<?, ?>> found =
            (Class<? extends Locator<?, ?>>) Class.forName(ln.locator(), false,
                getTop().getDomainClassLoader()).asSubclass(Locator.class);
        locatorType = found;
      } catch (ClassNotFoundException e) {
        return die(e, "Could not find the locator type specified in the @%s annotation %s",
            ProxyForName.class.getCanonicalName(), ln.value());
      }
    } else {
      // No locator annotation
      locatorType = null;
    }
    return locatorType;
  }

  @Override
  public Class<? extends ServiceLocator> resolveServiceLocator(
      Class<? extends RequestContext> requestContext) {
    Class<? extends ServiceLocator> locatorType;

    Service l = requestContext.getAnnotation(Service.class);
    ServiceName ln = requestContext.getAnnotation(ServiceName.class);
    if (l != null && !ServiceLocator.class.equals(l.locator())) {
      locatorType = l.locator();
    } else if (ln != null && ln.locator().length() > 0) {
      try {
        locatorType =
            Class.forName(ln.locator(), false, getTop().getDomainClassLoader()).asSubclass(
                ServiceLocator.class);
      } catch (ClassNotFoundException e) {
        return die(e, "Could not find the locator type specified in the @%s annotation %s",
            ServiceName.class.getCanonicalName(), ln.value());
      }
    } else {
      locatorType = null;
    }
    return locatorType;
  }

  private <T> Object doGetId(T domainObject) {
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) domainObject.getClass();
    Locator<T, ?> l = getLocator(clazz);
    if (l == null) {
      return super.getId(domainObject);
    }
    return l.getId(domainObject);
  }

  private <T> Object doGetVersion(T domainObject) {
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) domainObject.getClass();
    Locator<T, ?> l = getLocator(clazz);
    if (l == null) {
      return super.getVersion(domainObject);
    }
    return l.getVersion(domainObject);
  }

  private <T> boolean doIsLive(T domainObject) {
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) domainObject.getClass();
    Locator<T, ?> l = getLocator(clazz);
    if (l == null) {
      return super.isLive(domainObject);
    }
    return l.isLive(domainObject);
  }

  private <T, I> T doLoadDomainObject(Class<T> clazz, Object domainId) {
    @SuppressWarnings("unchecked")
    Locator<T, I> l = (Locator<T, I>) getLocator(clazz);
    if (l == null) {
      return super.loadDomainObject(clazz, domainId);
    }
    I id = l.getIdType().cast(domainId);
    return l.find(clazz, id);
  }

  @SuppressWarnings("unchecked")
  private <T, I> Locator<T, I> getLocator(Class<T> domainType) {
    Class<? extends Locator<?, ?>> locatorType = getTop().resolveLocator(domainType);
    if (locatorType == null) {
      return null;
    }
    return (Locator<T, I>) getTop().createLocator(locatorType);
  }

  private <T> T newInstance(Class<T> clazz, Class<? super T> base) {
    Throwable ex;
    try {
      return clazz.newInstance();
    } catch (InstantiationException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    }
    return this.<T> die(ex, "Could not instantiate %s %s. Is it default-instantiable?", base
        .getSimpleName(), clazz.getCanonicalName());
  }
}
