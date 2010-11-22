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

import com.google.gwt.autobean.server.AutoBeanFactoryMagic;
import com.google.gwt.autobean.shared.AutoBeanFactory;
import com.google.gwt.autobean.shared.AutoBeanFactory.Category;
import com.google.gwt.autobean.shared.AutoBeanFactory.NoWrap;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.server.testing.InProcessRequestContext.RequestContextHandler;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.ValueProxy;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestFactory;
import com.google.gwt.requestfactory.shared.impl.BaseProxyCategory;
import com.google.gwt.requestfactory.shared.impl.EntityProxyCategory;
import com.google.gwt.requestfactory.shared.impl.ValueProxyCategory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A JRE-compatible implementation of RequestFactory.
 */
class InProcessRequestFactory extends AbstractRequestFactory {
  @Category(value = {
      EntityProxyCategory.class, ValueProxyCategory.class,
      BaseProxyCategory.class})
  @NoWrap(EntityProxyId.class)
  interface Factory extends AutoBeanFactory {
  }

  class RequestFactoryHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
      if (Object.class.equals(method.getDeclaringClass())
          || RequestFactory.class.equals(method.getDeclaringClass())) {
        try {
          return method.invoke(InProcessRequestFactory.this, args);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }

      Class<? extends RequestContext> context = method.getReturnType().asSubclass(
          RequestContext.class);
      RequestContextHandler handler = new InProcessRequestContext(
          InProcessRequestFactory.this).new RequestContextHandler();
      return context.cast(Proxy.newProxyInstance(
          Thread.currentThread().getContextClassLoader(),
          new Class<?>[] {context}, handler));
    }
  }

  @Override
  public void initialize(EventBus eventBus) {
    throw new UnsupportedOperationException(
        "An explicit RequestTransport must be provided");
  }

  @Override
  public boolean isEntityType(Class<?> clazz) {
    return EntityProxy.class.isAssignableFrom(clazz);
  }

  @Override
  public boolean isValueType(Class<?> clazz) {
    return ValueProxy.class.isAssignableFrom(clazz);
  }

  @Override
  protected AutoBeanFactory getAutoBeanFactory() {
    return AutoBeanFactoryMagic.create(Factory.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <P extends BaseProxy> Class<P> getTypeFromToken(String typeToken) {
    try {
      Class<? extends BaseProxy> found = Class.forName(typeToken, false,
          Thread.currentThread().getContextClassLoader()).asSubclass(
          BaseProxy.class);
      return (Class<P>) found;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  protected String getTypeToken(Class<? extends BaseProxy> clazz) {
    return isEntityType(clazz) || isValueType(clazz) ? clazz.getName() : null;
  }
}
