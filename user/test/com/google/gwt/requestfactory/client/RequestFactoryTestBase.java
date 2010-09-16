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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Set;

/**
 * A base class for anything that makes use of the SimpleRequestFactory.
 * Subclasses must always use {@link #finishTestAndReset()} in order to allow
 * calls to the reset methods to complete before the next test starts.
 * 
 */
public abstract class RequestFactoryTestBase extends GWTTestCase {

  protected SimpleRequestFactory req;
  protected EventBus eventBus;

  @Override
  public void gwtSetUp() {
    eventBus = new SimpleEventBus();
    req = GWT.create(SimpleRequestFactory.class);
    req.init(eventBus);
  }

  protected void finishTestAndReset() {
    final boolean[] reallyDone = {false, false};
    req.simpleFooRequest().reset().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
        reallyDone[0] = true;
        if (reallyDone[0] && reallyDone[1]) {
          finishTest();
        }
      }
    });
    req.simpleBarRequest().reset().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
        reallyDone[1] = true;
        if (reallyDone[0] && reallyDone[1]) {
          finishTest();
        }
      }
    });
  }
}
