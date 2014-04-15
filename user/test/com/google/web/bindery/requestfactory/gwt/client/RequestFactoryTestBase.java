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
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.DefaultProxyStore;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyChange;
import com.google.web.bindery.requestfactory.shared.ProxySerializer;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooRequest;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;
import com.google.web.bindery.requestfactory.shared.impl.BaseProxyCategory;
import com.google.web.bindery.requestfactory.shared.impl.Constants;
import com.google.web.bindery.requestfactory.shared.impl.SimpleProxyId;

import java.util.Set;

/**
 * A base class for anything that makes use of the SimpleRequestFactory.
 * Subclasses must always use {@link #finishTestAndReset()} in order to allow
 * calls to the reset methods to complete before the next test starts.
 * 
 */
public abstract class RequestFactoryTestBase extends GWTTestCase {

  /**
   * A helper Receiver to test onFailure callbacks.
   */
  protected class SimpleFooFailureReceiver extends Receiver<SimpleFooProxy> {
    private SimpleFooProxy mutableFoo;
    private Request<SimpleFooProxy> persistRequest;
    private String expectedException;

    public SimpleFooFailureReceiver(SimpleFooProxy mutableFoo,
        Request<SimpleFooProxy> persistRequest, String exception) {
      this.mutableFoo = mutableFoo;
      this.persistRequest = persistRequest;
      this.expectedException = exception;
    }

    @Override
    public void onFailure(ServerFailure error) {
      assertSame(persistRequest.getRequestContext(), error.getRequestContext());
      assertEquals(expectedException, error.getExceptionType());
      if (expectedException != null) {
        assertFalse(error.getStackTraceString().length() == 0);
        assertEquals("THIS EXCEPTION IS EXPECTED BY A TEST", error.getMessage());
      } else {
        assertEquals(null, error.getStackTraceString());
        assertEquals("Server Error: THIS EXCEPTION IS EXPECTED BY A TEST", error.getMessage());
      }

      // Now show that we can fix the error and try again with the same
      // request

      mutableFoo.setPleaseCrash(24); // Only 42 and 43 crash
      persistRequest.fire(new Receiver<SimpleFooProxy>() {
        @Override
        public void onSuccess(SimpleFooProxy response) {
          response = checkSerialization(response);
          finishTestAndReset();
        }
      });
    }

    @Override
    public void onSuccess(SimpleFooProxy response) {
      fail("Failure expected but onSuccess() was called");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onViolation(Set<com.google.web.bindery.requestfactory.shared.Violation> errors) {
      fail("Failure expected but onViolation() was called");
    }
  }

  /**
   * Class for counting events.
   */
  protected class SimpleFooEventHandler<P extends EntityProxy> implements
      EntityProxyChange.Handler<P> {
    int persistEventCount = 0;
    int deleteEventCount = 0;
    int totalEventCount = 0;
    int updateEventCount = 0;

    @Override
    public void onProxyChange(EntityProxyChange<P> event) {
      totalEventCount++;
      switch (event.getWriteOperation()) {
        case PERSIST:
          persistEventCount++;
          break;
        case DELETE:
          deleteEventCount++;
          break;
        case UPDATE:
          updateEventCount++;
          break;
        default:
          break;
      }
    }
  }

  protected EventBus eventBus;
  protected SimpleRequestFactory req;

  @Override
  public void gwtSetUp() {
    req = createFactory();
    eventBus = req.getEventBus();
  }

  protected void checkEqualityAndHashcode(Object a, Object b) {
    assertNotNull(a);
    assertNotNull(b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, b);
    assertEquals(b, a);
  }

  /**
   * Check that some proxy type can be created by the given context and that
   * some other proxy type can't.
   */
  protected void checkReachableTypes(RequestContext ctx, Class<? extends BaseProxy> shouldWork,
      Class<? extends BaseProxy> shouldFail) {
    assertNotNull(ctx.create(shouldWork));
    try {
      // Metadata computation has only RequestFactory resolution
      // http://code.google.com/p/google-web-toolkit/issues/detail?id=6658
      if (GWT.isClient()) {
        ctx.create(shouldFail);
        fail();
      } else {
        assertNotNull(ctx.create(shouldFail));
      }
    } catch (IllegalArgumentException expected) {
      if (!GWT.isClient()) {
        fail("Expect the create call to always work in RFSource implementation");
      }
    }
  }

  /**
   * Run the given proxy through a ProxySerializer and verify that the
   * before-and-after values match.
   */
  protected <T extends BaseProxy> T checkSerialization(T proxy) {
    AutoBean<T> originalBean = AutoBeanUtils.getAutoBean(proxy);
    SimpleProxyId<T> id = BaseProxyCategory.stableId(originalBean);
    DefaultProxyStore store = new DefaultProxyStore();
    ProxySerializer s = req.getSerializer(store);

    String key = s.serialize(proxy);
    assertNotNull(key);

    // Use a new instance
    store = new DefaultProxyStore(store.encode());
    s = req.getSerializer(store);
    T restored = s.deserialize(id.getProxyClass(), key);
    AutoBean<? extends BaseProxy> restoredBean = AutoBeanUtils.getAutoBean(restored);
    assertNotSame(proxy, restored);
    /*
     * Performing a regular assertEquals() or even an AutoBeanUtils.diff() here
     * is wrong. If any of the objects in the graph are unpersisted, it's
     * expected that the stable ids would change. Instead, we do a value-based
     * check.
     */
    assertTrue(AutoBeanUtils.deepEquals(originalBean, restoredBean));

    if (proxy instanceof EntityProxy && !id.isEphemeral()) {
      assertEquals(((EntityProxy) proxy).stableId(), ((EntityProxy) restored).stableId());
    }

    // In deference to testing stable ids, copy the original id into the clone
    restoredBean.setTag(Constants.STABLE_ID, originalBean.getTag(Constants.STABLE_ID));
    return restored;
  }

  protected void checkStableIdEquals(EntityProxy expected, EntityProxy actual) {
    assertEquals(expected.stableId(), actual.stableId());
    assertEquals(expected.stableId().hashCode(), actual.stableId().hashCode());
    assertSame(expected.stableId(), actual.stableId());

    // No assumptions about the proxy objects (being proxies and all)
    assertNotSame(expected, actual);
    assertFalse(expected.equals(actual));
  }

  /**
   * Create and initialize a new {@link SimpleRequestFactory}.
   */
  protected SimpleRequestFactory createFactory() {
    SimpleRequestFactory toReturn = GWT.create(SimpleRequestFactory.class);
    toReturn.initialize(new SimpleEventBus());
    return toReturn;
  }

  protected void finishTestAndReset() {
    SimpleFooRequest ctx = req.simpleFooRequest();
    ctx.reset();
    ctx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }
}
