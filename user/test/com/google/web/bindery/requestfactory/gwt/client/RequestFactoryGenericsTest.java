/*
 * Copyright 2013 Google Inc.
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
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

import java.util.Collections;
import java.util.List;

/**
 * Tests usage of generics in RequestFactory.
 */
public class RequestFactoryGenericsTest extends GWTTestCase {

  /** Service under test. */
  public static class Impl {
    public static Domain echoDomain(Domain domainObject) {
      return domainObject;
    }

    public static BaseDomain<?, ?> getBase() {
      return new BaseDomain<Integer, Float>() {
        {
          setListOfStrings(Collections.singletonList("foo"));
        }

        @Override
        public void setA(Integer a) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Integer getA() {
          throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> getListOfA() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setListOfA(List<Integer> list) {
          throw new UnsupportedOperationException();
        }
      };
    }

    public static Container echoContainer(Container container) {
      return container;
    }
  }

  /** Generic interface implemented by both proxy and domain object. */
  public interface HasA<A> {
    A getA();

    void setA(A a);

    List<A> getListOfA();

    void setListOfA(List<A> list);
  }

  /** Base generic domain object. */
  public static abstract class BaseDomain<A, B> implements HasA<A> {
    private B b;
    private List<String> listOfStrings;

    public B getB() {
      return b;
    }

    public void setB(B b) {
      this.b = b;
    }

    public List<String> getListOfStrings() {
      return this.listOfStrings;
    }

    public void setListOfStrings(List<String> list) {
      this.listOfStrings = list;
    }
  }

  /** Domain object that "locks" generic type params from base class. */
  public static class Domain extends BaseDomain<String, Boolean> {
    private String str;
    private List<String> list;

    @Override
    public String getA() {
      return str;
    }

    @Override
    public void setA(String a) {
      str = a;
    }

    @Override
    public List<String> getListOfA() {
      return list;
    }

    @Override
    public void setListOfA(List<String> list) {
      this.list = list;
    }
  }

  /** Domain object containing another object with inherited generics. */
  public static class Container {
    private Domain domain;

    public Domain getDomain() {
      return domain;
    }

    public void setDomain(Domain domain) {
      this.domain = domain;
    }
  }

  /** Proxy testing that the domain type can be parameterized. */
  @ProxyFor(BaseDomain.class)
  public interface BaseDomainProxy extends ValueProxy {
    List<String> getListOfStrings();

    void setListOfStrings(List<String> listOfStrings);
  }

  /** Proxy implementing generic interface (locking the type param). */
  @ProxyFor(Domain.class)
  public interface DomainProxy extends BaseDomainProxy, HasA<String> {

    Boolean getB();

    void setB(Boolean b);
  }

  /** Base interface references proxy "with generics". */
  public interface BaseContainerProxy {
    BaseDomainProxy getDomain();
  }

  /**
   * Child-interface overrides method with co-variant return type (with
   * "locked" type params).
   */
  @ProxyFor(Container.class)
  public interface ContainerProxy extends ValueProxy, BaseContainerProxy {

    @Override
    DomainProxy getDomain();

    void setDomain(DomainProxy domain);
  }

  /** Request context under test. */
  @Service(Impl.class)
  public interface Context extends RequestContext {
    Request<DomainProxy> echoDomain(DomainProxy proxy);

    Request<BaseDomainProxy> getBase();

    Request<ContainerProxy> echoContainer(ContainerProxy container);
  }

  /** Factory under test. */
  public interface Factory extends RequestFactory {
    Context ctx();
  }

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  private static final int TEST_DELAY = 5000;

  protected Factory factory;

  public void testEchoDomain() throws Exception {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    DomainProxy proxy = ctx.create(DomainProxy.class);
    proxy.setA("foo");
    proxy.setB(true);
    proxy.setListOfA(Collections.singletonList("bar"));
    proxy.setListOfStrings(Collections.singletonList("baz"));
    ctx.echoDomain(proxy).fire(new Receiver<DomainProxy>() {
      @Override
      public void onSuccess(DomainProxy response) {
        assertEquals("foo", response.getA());
        assertEquals(Boolean.TRUE, response.getB());
        assertEquals(Collections.singletonList("bar"), response.getListOfA());
        assertEquals(Collections.singletonList("baz"), response.getListOfStrings());
        finishTest();
      }
    });
  }

  public void testGetBase() throws Exception {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.getBase().fire(new Receiver<BaseDomainProxy>() {
      @Override
      public void onSuccess(BaseDomainProxy response) {
        assertEquals(Collections.singletonList("foo"), response.getListOfStrings());
        finishTest();
      }
    });
  }

  public void testEchoContainer() throws Exception {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    DomainProxy proxy = ctx.create(DomainProxy.class);
    proxy.setA("42");
    ContainerProxy container = ctx.create(ContainerProxy.class);
    container.setDomain(proxy);
    ctx.echoContainer(container).fire(new Receiver<ContainerProxy>() {
      @Override
      public void onSuccess(ContainerProxy response) {
        assertEquals("42", response.getDomain().getA());
        finishTest();
      }
    });
  }

  @Override
  protected void gwtSetUp() throws Exception {
    factory = createFactory();
  }

  protected Factory createFactory() {
    Factory factory = GWT.create(Factory.class);
    factory.initialize(new SimpleEventBus());
    return factory;
  }
}
