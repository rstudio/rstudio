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

import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.SimpleBarRequest;
import com.google.web.bindery.requestfactory.shared.SimpleFooRequest;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime test for RequestBatcher.
 */
public class RequestBatcherTest extends RequestFactoryTestBase {

  private class MyBatcher extends RequestBatcher<SimpleRequestFactory, SimpleFooRequest> {
    public MyBatcher() {
      super(req);
    }

    public SimpleBarRequest simpleBarRequest() {
      return get().append(getRequestFactory().simpleBarRequest());
    }

    @Override
    protected SimpleFooRequest createContext(SimpleRequestFactory requestFactory) {
      return requestFactory.simpleFooRequest();
    }
  }

  private static final int TEST_DELAY = 5000;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  /**
   * Check automatic firing, chaining, and Void callbacks.
   */
  public void test() {
    delayTestFinish(TEST_DELAY);
    final List<Boolean> ok = new ArrayList<Boolean>();
    MyBatcher batcher = new MyBatcher();
    batcher.get().add(3, 5).to(new Receiver<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals(8, response.intValue());
        ok.add(true);
      }
    });
    // Verify that Request.fire() only enqueues the Receiver
    batcher.simpleBarRequest().countSimpleBar().fire(new Receiver<Long>() {
      @Override
      public void onSuccess(Long response) {
        assertEquals(2, response.longValue());
        ok.add(true);
      }
    });
    // Same check for RequestContext.fire()
    batcher.get().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        ok.add(true);
      }
    });
    // Test final callbacks through RequestBatcher
    batcher.get(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        assertEquals(3, ok.size());
        finishTestAndReset();
      }
    });
    assertFalse(((AbstractRequestContext) batcher.get()).isLocked());
  }
}
