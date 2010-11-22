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

import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.Locator;
import com.google.gwt.requestfactory.shared.LocatorFor;
import com.google.gwt.requestfactory.shared.LocatorForName;

/**
 * Adds support to the ServiceLayer chain for using {@link Locator} helper
 * objects.
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
    Throwable ex;
    try {
      return clazz.newInstance();
    } catch (InstantiationException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    }
    return this.<T> die(ex,
        "Could not instantiate Locator %s. It is default-instantiable?",
        clazz.getCanonicalName());
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

  @Override
  public Class<? extends Locator<?, ?>> resolveLocator(Class<?> domainType) {
    // Find the matching BaseProxy
    Class<?> proxyType = getTop().resolveClientType(domainType,
        BaseProxy.class, false);
    if (proxyType == null) {
      return null;
    }

    // Check it for annotations
    Class<? extends Locator<?, ?>> locatorType;
    LocatorFor l = proxyType.getAnnotation(LocatorFor.class);
    LocatorForName ln = proxyType.getAnnotation(LocatorForName.class);
    if (l != null) {
      locatorType = l.value();
    } else if (ln != null) {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends Locator<?, ?>> found = (Class<? extends Locator<?, ?>>) Class.forName(
            ln.value(), false, domainType.getClassLoader()).asSubclass(
            Locator.class);
        locatorType = found;
      } catch (ClassNotFoundException e) {
        return die(e,
            "Could not find the type specified in the @%s annotation %s",
            LocatorForName.class.getCanonicalName(), ln.value());
      }
    } else {
      // No locator annotation
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
    Class<? extends Locator<?, ?>> locatorType = getTop().resolveLocator(
        domainType);
    if (locatorType == null) {
      return null;
    }
    return (Locator<T, I>) getTop().createLocator(locatorType);
  }
}
