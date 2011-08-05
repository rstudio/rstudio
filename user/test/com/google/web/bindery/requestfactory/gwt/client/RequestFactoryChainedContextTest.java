/*
 * Copyright 2011 Google Inc.
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
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.SimpleBarProxy;
import com.google.web.bindery.requestfactory.shared.SimpleBarRequest;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooRequest;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/**
 * Tests various aspects of how {@code RequestContext.append()} behaves.
 */
public class RequestFactoryChainedContextTest extends RequestFactoryTestBase {
  /**
   * A RequestFactory where the contained RequestContext types have disjoint
   * reachable proxy types.
   */
  protected interface Factory extends RequestFactory {
    ACtx a();

    BCtx b();
  }

  /**
   * Mandatory javadoc comment.
   */
  public static class A {
    public static A a() {
      return new A();
    }
  }

  @Service(A.class)
  interface ACtx extends RequestContext {
    Request<AProxy> a();
  }

  @ProxyFor(A.class)
  interface AProxy extends ValueProxy {
  }

  /**
   * Mandatory javadoc comment.
   */
  public static class B {
    public static B b() {
      return new B();
    }
  }

  @Service(B.class)
  interface BCtx extends RequestContext {
    Request<BProxy> b();
  }

  @ProxyFor(B.class)
  interface BProxy extends ValueProxy {
  }

  private static final int DELAY_TEST_FINISH = 5000;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  /**
   * Basic functional test of the append method.
   */
  public void testAppend() {
    delayTestFinish(DELAY_TEST_FINISH);
    SimpleFooRequest fooCtx1 = req.simpleFooRequest();
    SimpleFooProxy foo1 = fooCtx1.create(SimpleFooProxy.class);
    SimpleBarRequest barCtx = fooCtx1.append(req.simpleBarRequest());
    SimpleFooRequest fooCtx2 = barCtx.append(req.simpleFooRequest());

    assertNotSame(fooCtx1, fooCtx2);
    assertSame(foo1, barCtx.edit(foo1));
    assertSame(foo1, fooCtx2.edit(foo1));

    SimpleBarProxy foo2 = barCtx.create(SimpleBarProxy.class);
    assertSame(foo2, fooCtx1.edit(foo2));
    assertSame(foo2, fooCtx2.edit(foo2));

    SimpleFooProxy foo3 = fooCtx2.create(SimpleFooProxy.class);
    assertSame(foo3, fooCtx1.edit(foo3));
    assertSame(foo3, barCtx.edit(foo3));

    try {
      // Throws exception because c3 has already accumulated some state
      req.simpleValueContext().append(fooCtx2);
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException expected) {
    }

    try {
      // Throws exception because a different RequestFactory instance is used
      fooCtx2.append(createFactory().simpleFooRequest());
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException expected) {
    }

    // Queue up two invocations, and test that both Receivers are called
    final boolean[] seen = {false, false};
    fooCtx1.add(1, 2).to(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        seen[0] = true;
        assertEquals(3, response.intValue());
      }
    });
    barCtx.countSimpleBar().to(new Receiver<Long>() {
      @Override
      public void onSuccess(Long response) {
        seen[1] = true;
        assertEquals(2, response.longValue());
      }
    });

    // It doesn't matter which context instance is fired
    barCtx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        assertTrue(seen[0]);
        assertTrue(seen[1]);
        finishTestAndReset();
      }
    });

    /*
     * Since the common State has been locked, calling any other
     * context-mutation methods should fail.
     */
    try {
      fooCtx1.fire();
      fail("Should have thrown exception");
    } catch (IllegalStateException expected) {
    }
    try {
      fooCtx2.fire();
      fail("Should have thrown exception");
    } catch (IllegalStateException expected) {
    }
    try {
      fooCtx2.create(SimpleFooProxy.class);
      fail("Should have thrown exception");
    } catch (IllegalStateException expected) {
    }
  }

  /**
   * Ensure that a method invoked on an appended context can result in the
   * creation of a proxy not reachable from canonical context.
   */
  public void testChainedProxyInstantiation() {
    delayTestFinish(DELAY_TEST_FINISH);
    Factory f = createChainedFactory();

    ACtx aCtx = f.a();
    checkReachableTypes(aCtx, AProxy.class, BProxy.class);

    RequestContext ctx = aCtx.a().to(new Receiver<AProxy>() {
      @Override
      public void onSuccess(AProxy response) {
        assertNotNull(response);
      }
    });

    BCtx bCtx = ctx.append(f.b());
    checkReachableTypes(aCtx, AProxy.class, BProxy.class);
    checkReachableTypes(bCtx, BProxy.class, AProxy.class);

    bCtx.b().to(new Receiver<BProxy>() {
      @Override
      public void onSuccess(BProxy response) {
        assertNotNull(response);
      }
    });
    ctx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  protected Factory createChainedFactory() {
    Factory f = GWT.create(Factory.class);
    f.initialize(new SimpleEventBus());
    return f;
  }
}
