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
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class FindServiceTest extends GWTTestCase {

  private SimpleRequestFactory req;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  @Override
  public void gwtSetUp() {
    req = GWT.create(SimpleRequestFactory.class);
    req.init(new SimpleEventBus());
  }

  @Override
  public void gwtTearDown() {
    req.simpleFooRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
    req.simpleBarRequest().reset().fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
  }

  public void testFetchEntity() {
    final boolean relationsAbsent = false;
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResult) {
            checkReturnedProxy(response, relationsAbsent);
            
            final EntityProxyId stableId = response.stableId();
            req.find(stableId).fire(
                new Receiver<EntityProxy>() {

                  @Override
                  public void onSuccess(EntityProxy returnedProxy,
                      Set<SyncResult> syncResults) {
                    assertEquals(stableId, returnedProxy.stableId());
                    checkReturnedProxy((SimpleFooProxy) returnedProxy, relationsAbsent);
                    finishTest();
                  }
                });
          }
        });
  }
  
  public void testFetchEntityWithRelation() {
    final boolean relationsPresent = true;
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).with("barField").fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResult) {
            checkReturnedProxy(response, relationsPresent);
            
            final EntityProxyId stableId = response.stableId();
            req.find(stableId).with("barField").fire(
                new Receiver<EntityProxy>() {

                  @Override
                  public void onSuccess(EntityProxy returnedProxy,
                      Set<SyncResult> syncResults) {
                    assertEquals(stableId, returnedProxy.stableId());
                    checkReturnedProxy((SimpleFooProxy) returnedProxy, relationsPresent);
                    finishTest();
                  }
                });
          }
        });
  }

  private void checkReturnedProxy(SimpleFooProxy response, boolean checkForRelations) {
    assertEquals(42, (int) response.getIntId());
    assertEquals("GWT", response.getUserName());
    assertEquals(8L, (long) response.getLongField());
    assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
        response.getEnumField());
    if (checkForRelations) {
      assertNotNull(response.getBarField());
    } else {
      assertEquals(null, response.getBarField());
    }
  }
}
