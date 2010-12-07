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

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleBarRequest;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.SimpleFooRequest;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class FindServiceTest extends RequestFactoryTestBase {
  /*
   * DO NOT USE finishTest(). Instead, call finishTestAndReset();
   */

  private static final int TEST_DELAY = 5000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testFetchDeletedEntity() {
    delayTestFinish(TEST_DELAY);
    SimpleBarRequest context = req.simpleBarRequest();
    SimpleBarProxy willDelete = context.create(SimpleBarProxy.class);
    context.persistAndReturnSelf().using(willDelete).fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy response) {
            final EntityProxyId<SimpleBarProxy> id = response.stableId();

            // Make the entity behave as though it's been deleted
            SimpleBarRequest context = req.simpleBarRequest();
            Request<Void> persist = context.persist().using(response);
            context.edit(response).setFindFails(true);
            persist.fire(new Receiver<Void>() {

              @Override
              public void onSuccess(Void response) {
                // Now try fetching the deleted instance
                req.find(id).fire(new Receiver<SimpleBarProxy>() {
                  @Override
                  public void onSuccess(SimpleBarProxy response) {
                    assertNull(response);
                    finishTestAndReset();
                  }
                });
              }
            });
          }
        });
  }

  public void testFetchEntityWithLongId() {
    final boolean relationsAbsent = false;
    delayTestFinish(TEST_DELAY);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            checkReturnedProxy(response, relationsAbsent);

            final EntityProxyId<SimpleFooProxy> stableId = response.stableId();
            req.find(stableId).fire(new Receiver<SimpleFooProxy>() {

              @Override
              public void onSuccess(SimpleFooProxy returnedProxy) {
                assertEquals(stableId, returnedProxy.stableId());
                checkReturnedProxy(returnedProxy, relationsAbsent);
                finishTestAndReset();
              }
            });
          }
        });
  }

  public void testFetchEntityWithRelation() {
    final boolean relationsPresent = true;
    delayTestFinish(TEST_DELAY);
    req.simpleFooRequest().findSimpleFooById(999L).with("barField").fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            checkReturnedProxy(response, relationsPresent);

            final EntityProxyId<SimpleFooProxy> stableId = response.stableId();
            req.find(stableId).with("barField").fire(
                new Receiver<SimpleFooProxy>() {

                  @Override
                  public void onSuccess(SimpleFooProxy returnedProxy) {
                    assertEquals(stableId, returnedProxy.stableId());
                    checkReturnedProxy(returnedProxy, relationsPresent);
                    finishTestAndReset();
                  }
                });
          }
        });
  }

  public void testFetchEntityWithStringId() {
    delayTestFinish(TEST_DELAY);
    req.simpleBarRequest().findSimpleBarById("999L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy response) {
            final EntityProxyId<SimpleBarProxy> stableId = response.stableId();
            req.find(stableId).fire(new Receiver<SimpleBarProxy>() {

              @Override
              public void onSuccess(SimpleBarProxy returnedProxy) {
                assertEquals(stableId, returnedProxy.stableId());
                finishTestAndReset();
              }
            });
          }
        });
  }

  public void testFetchsAfterCreateDontUpdate() {
    final int[] count = {0};
    final HandlerRegistration registration = EntityProxyChange.registerForProxyType(
        req.getEventBus(), SimpleFooProxy.class,
        new EntityProxyChange.Handler<SimpleFooProxy>() {
          public void onProxyChange(EntityProxyChange<SimpleFooProxy> event) {
            count[0]++;
          }
        });
    delayTestFinish(TEST_DELAY);
    SimpleFooRequest context = req.simpleFooRequest();
    SimpleFooProxy proxy = context.create(SimpleFooProxy.class);
    context.persistAndReturnSelf().using(proxy).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            // Persist and Update events
            assertEquals(2, count[0]);
            req.find(response.stableId()).fire(new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                // No new events
                assertEquals(2, count[0]);
                registration.removeHandler();
                finishTestAndReset();
              }
            });
          }
        });
  }

  /**
   * Demonstrates behavior when fetching an unpersisted id. The setup is
   * analagous to saving a future id into a cookie and then trying to fetch it
   * later.
   */
  public void testFetchUnpersistedFutureId() {
    String historyToken;

    // Here's the factory from the "previous invocation" of the client
    {
      SimpleRequestFactory oldFactory = createFactory();
      EntityProxyId<SimpleBarProxy> id = oldFactory.simpleBarRequest().create(
          SimpleBarProxy.class).stableId();
      historyToken = oldFactory.getHistoryToken(id);
    }

    EntityProxyId<SimpleBarProxy> id = req.getProxyId(historyToken);
    assertNotNull(id);
    try {
      req.find(id);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * A second fetch for an unchanged object should not result in any update
   * event.
   */
  public void testMultipleFetchesDontUpdate() {
    final int[] count = {0};
    final HandlerRegistration registration = EntityProxyChange.registerForProxyType(
        req.getEventBus(), SimpleFooProxy.class,
        new EntityProxyChange.Handler<SimpleFooProxy>() {
          public void onProxyChange(EntityProxyChange<SimpleFooProxy> event) {
            count[0]++;
          }
        });
    delayTestFinish(TEST_DELAY);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            assertEquals(1, count[0]);

            final EntityProxyId<SimpleFooProxy> stableId = response.stableId();
            req.find(stableId).fire(new Receiver<SimpleFooProxy>() {

              @Override
              public void onSuccess(SimpleFooProxy returnedProxy) {
                assertEquals(1, count[0]);
                registration.removeHandler();
                finishTestAndReset();
              }
            });
          }
        });
  }

  private void checkReturnedProxy(SimpleFooProxy response,
      boolean checkForRelations) {
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
