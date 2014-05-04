/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.http.client.Request;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.MethodProvidedByServiceLayerTest;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.lang.reflect.Method;

/**
 * JRE version of {@link MethodProvidedByServiceLayerTest}.
 */
public class MethodProvidedByServiceLayerJreTest extends MethodProvidedByServiceLayerTest {

  static class Decorator extends ServiceLayerDecorator {
    private static final String MISSING_DOMAIN_METHOD;
    private static final String MISSING_DOMAIN_TYPE;
    private static final String MISSING_DOMAIN_TYPE_INSTANCE_METHOD;

    private static String getTypeDescriptor(Class<?> clazz) {
      return "L" + clazz.getName().replace('.', '/') + ";";
    }

    static {
      String proxyTypeDescriptor = getTypeDescriptor(Proxy.class);
      String requestTypeDescriptor = getTypeDescriptor(Request.class);
      MISSING_DOMAIN_METHOD =
          new OperationKey(Context.class.getName(), "missingDomainMethod", "("
              + getTypeDescriptor(String.class) + ")" + requestTypeDescriptor).get();
      MISSING_DOMAIN_TYPE =
          new OperationKey(Context.class.getName(), "missingDomainType", "(" + proxyTypeDescriptor
              + ")" + requestTypeDescriptor).get();
      MISSING_DOMAIN_TYPE_INSTANCE_METHOD =
          new OperationKey(Context.class.getName(), "missingDomainTypeInstanceMethod", "()"
              + getTypeDescriptor(InstanceRequest.class)).get();
    }

    @Override
    public Method resolveDomainMethod(String operation) {
      if (MISSING_DOMAIN_METHOD.equals(operation)) {
        try {
          return getClass().getDeclaredMethod("echo", String.class);
        } catch (NoSuchMethodException e) {
          return this.die(e, "Cannot find " + getClass().getCanonicalName() + "::echo method");
        }
      } else if (MISSING_DOMAIN_TYPE.equals(operation)) {
        try {
          return SimpleFoo.class.getDeclaredMethod("echo", SimpleFoo.class);
        } catch (NoSuchMethodException e) {
          return this.die(e, "Cannot find " + SimpleFoo.class.getCanonicalName() + "::echo method");
        }
      } else if (MISSING_DOMAIN_TYPE_INSTANCE_METHOD.equals(operation)) {
        try {
          return SimpleFoo.class.getDeclaredMethod("persistAndReturnSelf");
        } catch (NoSuchMethodException e) {
          return this.die(e, "Cannot find " + SimpleFoo.class.getCanonicalName()
              + "::persistAndReturnSelf method");
        }
      }
      return super.resolveDomainMethod(operation);
    }

    @Override
    public Class<?> resolveDomainClass(Class<?> clazz) {
      if (Proxy.class.equals(clazz)) {
        return SimpleFoo.class;
      }
      return super.resolveDomainClass(clazz);
    }

    @Override
    public <T> Class<? extends T> resolveClientType(Class<?> domainClass, Class<T> clientType,
        boolean required) {
      if (SimpleFoo.class.equals(domainClass) && Proxy.class.equals(clientType)) {
        return Proxy.class.asSubclass(clientType);
      }
      return super.resolveClientType(domainClass, clientType, required);
    }

    public static final String echo(String s) {
      return s;
    }
  }

  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected Factory createFactory() {
    return RequestFactoryJreTest.createInProcess(Factory.class);
  }
}
