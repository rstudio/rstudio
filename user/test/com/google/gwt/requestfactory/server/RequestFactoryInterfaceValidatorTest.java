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

import com.google.gwt.requestfactory.server.RequestFactoryInterfaceValidator.ClassLoaderLoader;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.InstanceRequest;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;

import junit.framework.TestCase;

import java.util.logging.Logger;

/**
 * JRE tests for {@link RequestFactoryInterfaceValidator}.
 */
public class RequestFactoryInterfaceValidatorTest extends TestCase {
  static class Domain {
    static int fooStatic(int a) {
      return 0;
    }

    int foo(int a) {
      return 0;
    }
  }

  @ProxyFor(Domain.class)
  interface DomainProxy extends EntityProxy {
  }

  interface DomainProxyMissingAnnotation extends EntityProxy {
  }

  static class DomainWithOverloads {
    void foo() {
    }

    void foo(int a) {
    }

    String getId() {
      return null;
    }

    int getVersion() {
      return 0;
    }
  }

  @ProxyFor(DomainWithOverloads.class)
  interface DomainWithOverloadsProxy extends EntityProxy {
    void foo();
  }

  class Foo {
  }

  interface RequestContextMissingAnnotation extends RequestContext {
  }

  @Service(Domain.class)
  interface ServiceRequestMismatchedArity extends RequestContext {
    InstanceRequest<DomainProxy, Integer> foo(int a, int b);

    Request<Integer> fooStatic(int a, int b);
  }

  @Service(Domain.class)
  interface ServiceRequestMismatchedParam extends RequestContext {
    Request<Integer> foo(long a);
  }

  @Service(Domain.class)
  interface ServiceRequestMismatchedReturn extends RequestContext {
    Request<Long> foo(int a);
  }

  @Service(Domain.class)
  interface ServiceRequestMismatchedStatic extends RequestContext {
    Request<Integer> foo(int a);

    InstanceRequest<DomainProxy, Integer> fooStatic(int a);
  }

  @Service(Domain.class)
  interface ServiceRequestMissingMethod extends RequestContext {
    Request<Integer> doesNotExist(int a);
  }

  RequestFactoryInterfaceValidator v;

  public void testMismatchedArity() {
    v.validateRequestContext(ServiceRequestMismatchedArity.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMismatchedParamType() {
    v.validateRequestContext(ServiceRequestMismatchedParam.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMismatchedReturnType() {
    v.validateRequestContext(ServiceRequestMismatchedReturn.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMismatchedStatic() {
    v.validateRequestContext(ServiceRequestMismatchedStatic.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMissingDomainAnnotations() {
    v.validateEntityProxy(DomainProxyMissingAnnotation.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMissingServiceAnnotations() {
    v.validateRequestContext(RequestContextMissingAnnotation.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMissingIdAndVersion() {
    v.validateEntityProxy(DomainProxy.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testMissingMethod() {
    v.validateRequestContext(ServiceRequestMissingMethod.class.getName());
    assertTrue(v.isPoisoned());
  }

  public void testOverloadedMethod() {
    v.validateEntityProxy(DomainWithOverloadsProxy.class.getName());
    assertTrue(v.isPoisoned());
  }

  /**
   * Perform a full test of the RequestFactory used for most tests.
   */
  public void testTestCodeFactories() {
    v.validateRequestFactory(SimpleRequestFactory.class.getName());
    assertFalse(v.isPoisoned());
  }

  @Override
  protected void setUp() throws Exception {
    Logger logger = Logger.getLogger("");
    v = new RequestFactoryInterfaceValidator(logger, new ClassLoaderLoader(
        Thread.currentThread().getContextClassLoader()));
  }
}
