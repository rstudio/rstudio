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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;

/**
 * A base class for anything that makes use of the SimpleRequestFactory.
 * Subclasses must always use {@link #finishTestAndReset()} in order to allow
 * calls to the reset methods to complete before the next test starts.
 * 
 */
public abstract class RequestFactoryTestBase extends GWTTestCase {

  protected SimpleRequestFactory req;
  protected EventBus eventBus;

  /**
   *  Class for counting events.
   */
  protected class SimpleFooEventHandler<P extends EntityProxy>
      implements EntityProxyChange.Handler<P> {
    int persistEventCount = 0;
    int deleteEventCount = 0;
    int totalEventCount = 0;
    int updateEventCount = 0;

    public void onProxyChange(EntityProxyChange<P> event) {
      totalEventCount++;
      switch (event.getWriteOperation()) {
        case PERSIST: persistEventCount++; break;
        case DELETE: deleteEventCount++; break;
        case UPDATE: updateEventCount++; break;
        default: break;
      }
    }
  }

  @Override
  public void gwtSetUp() {
    eventBus = new SimpleEventBus();
    req = GWT.create(SimpleRequestFactory.class);
    req.initialize(eventBus);
  }

  protected void finishTestAndReset() {
    final boolean[] reallyDone = {false, false, false};
    req.simpleFooRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        reallyDone[0] = true;
        if (reallyDone[0] && reallyDone[1] && reallyDone[2]) {
          finishTest();
        }
      }
    });
    req.simpleFooStringRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        reallyDone[1] = true;
        if (reallyDone[0] && reallyDone[1] && reallyDone[2]) {
          finishTest();
        }
      }
    });
    req.simpleBarRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        reallyDone[2] = true;
        if (reallyDone[0] && reallyDone[1] && reallyDone[2]) {
          finishTest();
        }
      }
    });
  }
  
  protected void checkStableIdEquals(EntityProxy expected,
      EntityProxy actual) {
    assertNotSame(expected.stableId(), actual.stableId());
    assertEquals(expected.stableId(), actual.stableId());
    assertEquals(expected.stableId().hashCode(), actual.stableId().hashCode());

    // No assumptions about the proxy objects (being proxies and all)
    assertNotSame(expected, actual);
    // TODO: uncomment after ProxyImpl equality is rehashed out
    //    assertFalse(expected.equals(actual));
  }
}
