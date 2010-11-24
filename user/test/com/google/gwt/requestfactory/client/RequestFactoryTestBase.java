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
package com.google.gwt.requestfactory.client;

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.BaseProxy;
import com.google.gwt.requestfactory.shared.DefaultProxyStore;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.ProxySerializer;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;
import com.google.gwt.requestfactory.shared.impl.BaseProxyCategory;
import com.google.gwt.requestfactory.shared.impl.Constants;
import com.google.gwt.requestfactory.shared.impl.SimpleProxyId;

/**
 * A base class for anything that makes use of the SimpleRequestFactory.
 * Subclasses must always use {@link #finishTestAndReset()} in order to allow
 * calls to the reset methods to complete before the next test starts.
 * 
 */
public abstract class RequestFactoryTestBase extends GWTTestCase {

  /**
   * Class for counting events.
   */
  protected class SimpleFooEventHandler<P extends EntityProxy> implements
      EntityProxyChange.Handler<P> {
    int persistEventCount = 0;
    int deleteEventCount = 0;
    int totalEventCount = 0;
    int updateEventCount = 0;

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
    AutoBean<BaseProxy> restoredBean = AutoBeanUtils.getAutoBean(restored);
    assertNotSame(proxy, restored);
    /*
     * Performing a regular assertEquals() or even an AutoBeanUtils.diff() here
     * is wrong. If any of the objects in the graph are unpersisted, it's
     * expected that the stable ids would change. Instead, we do a value-based
     * check.
     */
    assertTrue(AutoBeanUtils.deepEquals(originalBean, restoredBean));

    if (proxy instanceof EntityProxy && !id.isEphemeral()) {
      assertEquals(((EntityProxy) proxy).stableId(),
          ((EntityProxy) restored).stableId());
    }

    // In deference to testing stable ids, copy the original id into the clone
    restoredBean.setTag(Constants.STABLE_ID,
        originalBean.getTag(Constants.STABLE_ID));
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
    final boolean[] reallyDone = {false, false};
    req.simpleFooRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        reallyDone[0] = true;
        if (reallyDone[0] && reallyDone[1]) {
          finishTest();
        }
      }
    });
    req.simpleBarRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        reallyDone[1] = true;
        if (reallyDone[0] && reallyDone[1]) {
          finishTest();
        }
      }
    });
  }
}
