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
package com.google.gwt.valuestore.client;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.valuestore.shared.SimpleFooRecord;
import com.google.gwt.valuestore.shared.SimpleRequestFactory;
import com.google.gwt.valuestore.shared.SyncResult;

import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class RequestFactoryTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.valuestore.ValueStoreSuite";
  }

  public void testFetchEntity() {
    SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooRecord>() {
          @Override
          public void onSuccess(SimpleFooRecord response,
              Set<SyncResult> syncResult) {
            assertEquals((int) 42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.valuestore.shared.SimpleEnum.FOO,
                response.getEnumField());
            finishTest();
          }
        });
  }
}