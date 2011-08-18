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
package com.google.web.bindery.requestfactory.vm;

import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.JsonRpcService;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext.Dialect;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestFactory;
import com.google.web.bindery.requestfactory.shared.impl.BaseProxyCategory;
import com.google.web.bindery.requestfactory.shared.impl.EntityProxyCategory;
import com.google.web.bindery.requestfactory.shared.impl.ValueProxyCategory;
import com.google.web.bindery.requestfactory.vm.InProcessRequestContext.RequestContextHandler;
import com.google.web.bindery.requestfactory.vm.impl.Deobfuscator;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A JRE-compatible implementation of RequestFactory.
 */
class InProcessRequestFactory extends AbstractRequestFactory {
  @AutoBeanFactory.Category(value = {
      EntityProxyCategory.class, ValueProxyCategory.class, BaseProxyCategory.class})
  @AutoBeanFactory.NoWrap(EntityProxyId.class)
  interface Factory extends AutoBeanFactory {
  }

  class RequestFactoryHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (Object.class.equals(method.getDeclaringClass())
          || RequestFactory.class.equals(method.getDeclaringClass())) {
        try {
          return method.invoke(InProcessRequestFactory.this, args);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }

      Class<? extends RequestContext> context =
          method.getReturnType().asSubclass(RequestContext.class);
      Dialect dialect =
          method.getReturnType().isAnnotationPresent(JsonRpcService.class) ? Dialect.JSON_RPC
              : Dialect.STANDARD;
      RequestContextHandler handler =
          new InProcessRequestContext(InProcessRequestFactory.this, dialect, context).new RequestContextHandler();
      return context.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
          new Class<?>[] {context}, handler));
    }
  }

  private final Class<? extends RequestFactory> requestFactoryInterface;
  private final Deobfuscator deobfuscator;

  public InProcessRequestFactory(Class<? extends RequestFactory> requestFactoryInterface) {
    this.requestFactoryInterface = requestFactoryInterface;
    deobfuscator =
        Deobfuscator.Builder.load(requestFactoryInterface,
            Thread.currentThread().getContextClassLoader()).build();
  }

  public Deobfuscator getDeobfuscator() {
    return deobfuscator;
  }

  @Override
  public String getFactoryTypeToken() {
    return requestFactoryInterface.getName();
  }

  @Override
  public void initialize(EventBus eventBus) {
    throw new UnsupportedOperationException("An explicit RequestTransport must be provided");
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
    return AutoBeanFactorySource.create(Factory.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <P extends BaseProxy> Class<P> getTypeFromToken(String typeToken) {
    String deobfuscated = deobfuscator.getTypeFromToken(typeToken);
    if (deobfuscated == null) {
      throw new RuntimeException("Did not have deobfuscation data for " + typeToken);
    }
    try {
      Class<? extends BaseProxy> found =
          Class.forName(deobfuscated, false, Thread.currentThread().getContextClassLoader())
              .asSubclass(BaseProxy.class);
      return (Class<P>) found;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  protected String getTypeToken(Class<? extends BaseProxy> clazz) {
    return isEntityType(clazz) || isValueType(clazz) ? OperationKey.hash(clazz.getName()) : null;
  }
}
