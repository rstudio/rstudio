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
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.valuestore.shared.SimpleBarRecord;
import com.google.gwt.valuestore.shared.SimpleFooRecord;
import com.google.gwt.valuestore.shared.SimpleRequestFactory;
import com.google.gwt.valuestore.shared.SyncResult;

import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class RequestFactoryTest extends GWTTestCase {

  public void disabled_testPersistRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);
    SimpleFooRecord newFoo = (SimpleFooRecord) req.create(SimpleFooRecord.class);
    SimpleBarRecord newBar = (SimpleBarRecord) req.create(SimpleBarRecord.class);

    final RequestObject<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final RequestObject<Void> barReq = req.simpleBarRequest().persist(newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    final SimpleBarRecord finalNewBar = newBar;
    final SimpleFooRecord finalNewFoo = newFoo;
    fooReq.fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResultSet1) {
        for (SyncResult syncResult1 : syncResultSet1) {
          // update id.
        }
        barReq.fire(new Receiver<Void>() {
          public void onSuccess(Void response, Set<SyncResult> syncResultSet2) {
            final RequestObject<Void> fooReq2 = req.simpleFooRequest().persist(
                finalNewFoo);
            SimpleFooRecord newRec = fooReq2.edit(finalNewFoo);
            newRec.setBarField(finalNewBar);

            fooReq2.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResultSet3) {
                finishTest();
              }
            });
          }
        });
      }
    });
  }

  @Override
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
          public void onSuccess(SimpleFooRecord response,
              Set<SyncResult> syncResult) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.valuestore.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertEquals(null, response.getBarField());
            finishTest();
          }
        });
  }

  public void testFetchEntityWithRelation() {
    SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).with("barField").fire(
        new Receiver<SimpleFooRecord>() {
          public void onSuccess(SimpleFooRecord response,
              Set<SyncResult> syncResult) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.valuestore.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertNotNull(response.getBarField());
            finishTest();
          }
        });
  }
}