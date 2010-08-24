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
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.SimpleBarRecord;
import com.google.gwt.valuestore.shared.SimpleFooRecord;
import com.google.gwt.valuestore.shared.SimpleRequestFactory;
import com.google.gwt.valuestore.shared.SyncResult;

import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class RequestFactoryTest extends GWTTestCase {

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistExistingEntityExistingRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById(999L).fire(
        new Receiver<SimpleBarRecord>() {
          public void onSuccess(final SimpleBarRecord barRecord,
              Set<SyncResult> syncResults) {
            req.simpleFooRequest().findSimpleFooById(999L).fire(
                new Receiver<SimpleFooRecord>() {
                  public void onSuccess(SimpleFooRecord fooRecord,
                      Set<SyncResult> syncResults) {
                    RequestObject<Void> updReq = req.simpleFooRequest().persist(
                        fooRecord);
                    fooRecord = updReq.edit(fooRecord);
                    fooRecord.setBarField(barRecord);
                    updReq.fire(new Receiver<Void>() {
                      public void onSuccess(Void response,
                          Set<SyncResult> syncResults) {
                        finishTest();
                      }
                    });
                  }
                });
          }
        });
  }

  /*
   * Find Entity Create Entity2 Relate Entity2 to Entity Persist Entity
   */
  public void testPersistExistingEntityNewRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);

    SimpleBarRecord newBar = (SimpleBarRecord) req.create(SimpleBarRecord.class);

    final RequestObject<Void> barReq = req.simpleBarRequest().persist(newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    final SimpleBarRecord finalNewBar = newBar;
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooRecord>() {
          public void onSuccess(SimpleFooRecord response,
              Set<SyncResult> syncResults) {
            RequestObject<Void> fooReq = req.simpleFooRequest().persist(
                response);
            response = fooReq.edit(response);
            response.setBarField(finalNewBar);
            fooReq.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResults) {
                req.simpleFooRequest().findSimpleFooById(999L).with(
                    "barField.userName").fire(new Receiver<SimpleFooRecord>() {
                  public void onSuccess(SimpleFooRecord finalFooRecord,
                      Set<SyncResult> syncResults) {
                    // barReq hasn't been persisted, so old value
                    assertEquals("FOO",
                        finalFooRecord.getBarField().getUserName());
                    finishTest();
                  }

                });
              }
            });
          }
        });
  }

  /*
   * Find Entity2 Create Entity, Persist Entity Relate Entity2 to Entity Persist
   * Entity
   */
  public void testPersistNewEntityExistingRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);
    SimpleFooRecord newFoo = (SimpleFooRecord) req.create(SimpleFooRecord.class);

    final RequestObject<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final SimpleFooRecord finalFoo = newFoo;
    req.simpleBarRequest().findSimpleBarById(999L).fire(
        new Receiver<SimpleBarRecord>() {
          public void onSuccess(SimpleBarRecord response,
              Set<SyncResult> syncResults) {
            finalFoo.setBarField(response);
            fooReq.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResults) {
                req.simpleFooRequest().findSimpleFooById(999L).fire(
                    new Receiver<SimpleFooRecord>() {
                      public void onSuccess(SimpleFooRecord finalFooRecord,
                          Set<SyncResult> syncResults) {
                        // newFoo hasn't been persisted, so userName is the old value.
                        assertEquals("GWT", finalFooRecord.getUserName());
                        finishTest();
                      }

                    });
              }
            });
          }
        });
  }

  /*
   * Create Entity, Persist Entity Create Entity2, Perist Entity2 relate Entity2
   * to Entity Persist
   */
  public void testPersistNewEntityNewRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);
    SimpleFooRecord newFoo = (SimpleFooRecord) req.create(SimpleFooRecord.class);
    SimpleBarRecord newBar = (SimpleBarRecord) req.create(SimpleBarRecord.class);

    final RequestObject<SimpleFooRecord> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final RequestObject<SimpleBarRecord> barReq = req.simpleBarRequest().persistAndReturnSelf(
        newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    fooReq.fire(new Receiver<SimpleFooRecord>() {
      public void onSuccess(final SimpleFooRecord persistedFoo,
          Set<SyncResult> syncResult) {
        barReq.fire(new Receiver<SimpleBarRecord>() {
          public void onSuccess(final SimpleBarRecord persistedBar,
              Set<SyncResult> syncResults) {
            assertEquals("Ray", persistedFoo.getUserName());
            final RequestObject<Void> fooReq2 = req.simpleFooRequest().persist(
                persistedFoo);
            SimpleFooRecord editablePersistedFoo = fooReq2.edit(persistedFoo);
            editablePersistedFoo.setBarField(persistedBar);
            fooReq2.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResults) {
                req.simpleFooRequest().findSimpleFooById(999L).with(
                    "barField.userName").fire(new Receiver<SimpleFooRecord>() {
                  public void onSuccess(SimpleFooRecord finalFooRecord,
                      Set<SyncResult> syncResults) {
                    assertEquals("Amit",
                        finalFooRecord.getBarField().getUserName());
                    finishTest();
                  }

                });
              }
            });
          }
        });
      }
    });
  }

   public void testPersistRecursiveRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);

    SimpleFooRecord rayFoo = req.create(SimpleFooRecord.class);
    final RequestObject<SimpleFooRecord> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");
    rayFoo.setFooField(rayFoo);
    persistRay.fire(new Receiver<SimpleFooRecord>() {
      public void onSuccess(final SimpleFooRecord persistedRay,
          Set<SyncResult> ignored) {
        finishTest();
      }
    });
  }

  public void testPersistRelation() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);

    SimpleFooRecord rayFoo = req.create(SimpleFooRecord.class);
    final RequestObject<SimpleFooRecord> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");

    persistRay.fire(new Receiver<SimpleFooRecord>() {
      public void onSuccess(final SimpleFooRecord persistedRay,
          Set<SyncResult> ignored) {

        SimpleBarRecord amitBar = req.create(SimpleBarRecord.class);
        final RequestObject<SimpleBarRecord> persistAmit = req.simpleBarRequest().persistAndReturnSelf(
            amitBar);
        amitBar = persistAmit.edit(amitBar);
        amitBar.setUserName("Amit");

        persistAmit.fire(new Receiver<SimpleBarRecord>() {
          public void onSuccess(SimpleBarRecord persistedAmit,
              Set<SyncResult> ignored) {

            final RequestObject<SimpleFooRecord> persistRelationship = req.simpleFooRequest().persistAndReturnSelf(
                persistedRay).with("barField");
            SimpleFooRecord newRec = persistRelationship.edit(persistedRay);
            newRec.setBarField(persistedAmit);

            persistRelationship.fire(new Receiver<SimpleFooRecord>() {
              public void onSuccess(SimpleFooRecord relatedRay,
                  Set<SyncResult> ignored) {
                assertEquals("Amit", relatedRay.getBarField().getUserName());
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

  public void testRecordsAsInstanceMethodParams() {

    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooRecord>() {
          public void onSuccess(SimpleFooRecord response,
              Set<SyncResult> syncResult) {
            SimpleBarRecord bar = req.create(SimpleBarRecord.class);
            RequestObject<String> helloReq = req.simpleFooRequest().hello(response, bar);
            bar = helloReq.edit(bar);
            bar.setUserName("BAR");
            helloReq.fire(new Receiver<String>() {
              public void onSuccess(String response,
                  Set<SyncResult> syncResults) {
                assertEquals("Greetings BAR from GWT", response);
                finishTest();
              }
            });
          }
        });
  }

  /*
   * tests that (a) any method can have a side effect that is handled correctly. (b)
   * instance methods are handled correctly.
   */
  public void testMethodWithSideEffects() {
    final SimpleRequestFactory req = GWT.create(SimpleRequestFactory.class);
    HandlerManager hm = new HandlerManager(null);
    req.init(hm);
    delayTestFinish(5000);

    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooRecord>() {

          public void onSuccess(SimpleFooRecord newFoo,
              Set<SyncResult> syncResults) {
            final RequestObject<Long> fooReq = req.simpleFooRequest().countSimpleFooWithUserNameSideEffect(
                newFoo);
            newFoo = fooReq.edit(newFoo);
            newFoo.setUserName("Ray");
            fooReq.fire(new Receiver<Long>() {
              public void onSuccess(Long response, Set<SyncResult> syncResults) {
                assertEquals(new Long(1L), response);
                // confirm that there was a sideEffect.
                assertEquals(1, syncResults.size());
                SyncResult syncResultArray[] = syncResults.toArray(new SyncResult[0]);
                assertFalse(syncResultArray[0].hasViolations());
                assertNull(syncResultArray[0].getFutureId());
                Record record = syncResultArray[0].getRecord();
                assertEquals(new Long(999L), record.getId());
                // confirm that the instance method did have the desired sideEffect.
                req.simpleFooRequest().findSimpleFooById(999L).fire(
                    new Receiver<SimpleFooRecord>() {
                      public void onSuccess(SimpleFooRecord finalFoo,
                          Set<SyncResult> syncResults) {
                        assertEquals("Ray", finalFoo.getUserName());
                        finishTest();
                      }
                    });
              }
            });
          }
        });
  }
}
