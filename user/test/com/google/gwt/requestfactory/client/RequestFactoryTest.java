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
import com.google.gwt.requestfactory.client.impl.ProxyImpl;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class RequestFactoryTest extends GWTTestCase {

  private SimpleRequestFactory req;

  @Override
  public void gwtSetUp() {
    req = GWT.create(SimpleRequestFactory.class);
    req.init(new SimpleEventBus());
  }

  @Override
  public void gwtTearDown() {
    req.simpleFooRequest().reset().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
    req.simpleBarRequest().reset().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
  }

  public void testDummyCreate() {
    delayTestFinish(5000);

    final SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    Object futureId = foo.getId();
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).isFuture());
    RequestObject<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        foo);
    fooReq.fire(new Receiver<SimpleFooProxy>() {

      public void onSuccess(final SimpleFooProxy returned,
          Set<SyncResult> syncResults) {
        Object futureId = foo.getId();
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).isFuture());

        checkStableIdEquals(foo, returned);
        finishTest();
      }
    });
  }

  public void testStableId() {
    delayTestFinish(5000);

    final SimpleFooProxy foo = req.create(SimpleFooProxy.class);
    final Object futureId = foo.getId();
    assertTrue(((ProxyImpl) foo).isFuture());
    RequestObject<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        foo);

    final SimpleFooProxy newFoo = fooReq.edit(foo);
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).isFuture());
    assertEquals(futureId, newFoo.getId());
    assertTrue(((ProxyImpl) newFoo).isFuture());

    newFoo.setUserName("GWT basic user");
    fooReq.fire(new Receiver<SimpleFooProxy>() {

      public void onSuccess(final SimpleFooProxy returned,
          Set<SyncResult> syncResults) {
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).isFuture());
        assertEquals(futureId, newFoo.getId());
        assertTrue(((ProxyImpl) newFoo).isFuture());

        assertFalse(((ProxyImpl) returned).isFuture());

        checkStableIdEquals(foo, returned);
        checkStableIdEquals(newFoo, returned);

        RequestObject<SimpleFooProxy> editRequest = req.simpleFooRequest().persistAndReturnSelf(
            returned);
        final SimpleFooProxy editableFoo = editRequest.edit(returned);
        editableFoo.setUserName("GWT power user");
        editRequest.fire(new Receiver<SimpleFooProxy>() {

          public void onSuccess(SimpleFooProxy returnedAfterEdit,
              Set<SyncResult> syncResults) {
            checkStableIdEquals(editableFoo, returnedAfterEdit);
            assertEquals(returnedAfterEdit.getId(), returned.getId());
            finishTest();
          }
        });
      }
    });
  }

  private void checkStableIdEquals(SimpleFooProxy expected, SimpleFooProxy actual) {
    assertNotSame(expected.stableId(), actual.stableId());
    assertEquals(expected.stableId(), actual.stableId());
    assertEquals(expected.stableId().hashCode(), actual.stableId().hashCode());

    // No assumptions about the proxy objects (being proxies and all)
    assertNotSame(expected, actual);
    assertFalse(expected.equals(actual));
  }

  public void testViolationsOnCreate() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final RequestObject<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("A"); // will cause constraint violation

    fooReq.fire(new Receiver<Void>() {
      public void onSuccess(Void ignore, Set<SyncResult> syncResults) {
        assertEquals(1, syncResults.size());
        SyncResult syncResult = syncResults.iterator().next();
        /*
         * Make sure your class path includes:
         *
         * tools/apache/log4j/log4j-1.2.16.jar
         * tools/hibernate/validator/hibernate-validator-4.1.0.Final.jar
         * tools/slf4j/slf4j-api/slf4j-api-1.6.1.jar
         * tools/slf4j/slf4j-log4j12/slf4j-log4j12-1.6.1.jar
         */
        assertTrue("Violations expected (you might be missing some jars, "
            + "see the comment above this line)", syncResult.hasViolations());
        Map<String, String> violations = syncResult.getViolations();
        assertEquals(1, violations.size());
        assertEquals("size must be between 3 and 30",
            violations.get("userName"));
        finishTest();
      }
    });
  }

  public void testViolationsOnEdit() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final RequestObject<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("GWT User");

    fooReq.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(SimpleFooProxy returned, Set<SyncResult> syncResults) {
        assertEquals(1, syncResults.size());
        SyncResult syncResult = syncResults.iterator().next();
        assertFalse(syncResult.hasViolations());

        RequestObject<Void> editRequest = req.simpleFooRequest().persist(returned);
        SimpleFooProxy editableFoo = editRequest.edit(returned);
        editableFoo.setUserName("A");

        editRequest.fire(new Receiver<Void>() {
          public void onSuccess(Void returned, Set<SyncResult> syncResults) {
            assertEquals(1, syncResults.size());
            SyncResult syncResult = syncResults.iterator().next();
            /*
             * Make sure your class path includes:
             *
             * tools/apache/log4j/log4j-1.2.16.jar
             * tools/hibernate/validator/hibernate-validator-4.1.0.Final.jar
             * tools/slf4j/slf4j-api/slf4j-api-1.6.1.jar
             * tools/slf4j/slf4j-log4j12/slf4j-log4j12-1.6.1.jar
             */
            assertTrue("Violations expected (you might be missing some jars, "
                + "see the comment above this line)", syncResult.hasViolations());
            Map<String, String> violations = syncResult.getViolations();
            assertEquals(1, violations.size());
            assertEquals("size must be between 3 and 30",
                violations.get("userName"));
            finishTest();
          }
        });
      }
    });
  }

  /*
   * Disabling this new test for now. Will fix once the constraint violation portion of
   * https://jira.springsource.org/browse/ROO-954 is done.
   */
  public void disabled_testViolationsOnEdit() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final RequestObject<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("GWT User");

    fooReq.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(SimpleFooProxy returned, Set<SyncResult> syncResults) {
        assertEquals(1, syncResults.size());
        SyncResult syncResult = syncResults.iterator().next();
        assertFalse(syncResult.hasViolations());

        RequestObject<SimpleFooProxy> editRequest = req.simpleFooRequest().persistAndReturnSelf(returned);
        SimpleFooProxy editableFoo = editRequest.edit(returned);
        editableFoo.setUserName("A");

        editRequest.fire(new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy returned, Set<SyncResult> syncResults) {
            assertEquals(1, syncResults.size());
            SyncResult syncResult = syncResults.iterator().next();
            /*
             * Make sure your class path includes:
             *
             * tools/apache/log4j/log4j-1.2.16.jar
             * tools/hibernate/validator/hibernate-validator-4.1.0.Final.jar
             * tools/slf4j/slf4j-api/slf4j-api-1.6.1.jar
             * tools/slf4j/slf4j-log4j12/slf4j-log4j12-1.6.1.jar
             */
            assertTrue("Violations expected (you might be missing some jars, "
                + "see the comment above this line)", syncResult.hasViolations());
            Map<String, String> violations = syncResult.getViolations();
            assertEquals(1, violations.size());
            assertEquals("size must be between 3 and 30",
                violations.get("userName"));
            finishTest();
          }
        });
      }
    });
  }

  public void testViolationAbsent() {
    delayTestFinish(5000);

    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    final RequestObject<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Amit"); // will not cause violation.

    fooReq.fire(new Receiver<Void>() {
      public void onSuccess(Void ignore, Set<SyncResult> syncResults) {
        assertEquals(1, syncResults.size());
        SyncResult syncResult = syncResults.iterator().next();
        assertFalse(syncResult.hasViolations());
        finishTest();
      }
    });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistExistingEntityExistingRelation() {
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById(999L).fire(
        new Receiver<SimpleBarProxy>() {
          public void onSuccess(final SimpleBarProxy barProxy,
              Set<SyncResult> syncResults) {
            req.simpleFooRequest().findSimpleFooById(999L).fire(
                new Receiver<SimpleFooProxy>() {
                  public void onSuccess(SimpleFooProxy fooProxy,
                      Set<SyncResult> syncResults) {
                    RequestObject<Void> updReq = req.simpleFooRequest().persist(
                        fooProxy);
                    fooProxy = updReq.edit(fooProxy);
                    fooProxy.setBarField(barProxy);
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
    delayTestFinish(5000);

    SimpleBarProxy newBar = req.create(SimpleBarProxy.class);

    final RequestObject<Void> barReq = req.simpleBarRequest().persist(newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    final SimpleBarProxy finalNewBar = newBar;
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResults) {
            RequestObject<Void> fooReq = req.simpleFooRequest().persist(
                response);
            response = fooReq.edit(response);
            response.setBarField(finalNewBar);
            fooReq.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResults) {
                req.simpleFooRequest().findSimpleFooById(999L).with(
                    "barField.userName").fire(new Receiver<SimpleFooProxy>() {
                  public void onSuccess(SimpleFooProxy finalFooProxy,
                      Set<SyncResult> syncResults) {
                    // barReq hasn't been persisted, so old value
                    assertEquals("FOO",
                        finalFooProxy.getBarField().getUserName());
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
    delayTestFinish(5000);
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);

    final RequestObject<Void> fooReq = req.simpleFooRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final SimpleFooProxy finalFoo = newFoo;
    req.simpleBarRequest().findSimpleBarById(999L).fire(
        new Receiver<SimpleBarProxy>() {
          public void onSuccess(SimpleBarProxy response,
              Set<SyncResult> syncResults) {
            finalFoo.setBarField(response);
            fooReq.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResults) {
                req.simpleFooRequest().findSimpleFooById(999L).fire(
                    new Receiver<SimpleFooProxy>() {
                      public void onSuccess(SimpleFooProxy finalFooProxy,
                          Set<SyncResult> syncResults) {
                        // newFoo hasn't been persisted, so userName is the old
                        // value.
                        assertEquals("GWT", finalFooProxy.getUserName());
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
    delayTestFinish(5000);
    SimpleFooProxy newFoo = req.create(SimpleFooProxy.class);
    SimpleBarProxy newBar = req.create(SimpleBarProxy.class);

    final RequestObject<SimpleFooProxy> fooReq = req.simpleFooRequest().persistAndReturnSelf(
        newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final RequestObject<SimpleBarProxy> barReq = req.simpleBarRequest().persistAndReturnSelf(
        newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    fooReq.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(final SimpleFooProxy persistedFoo,
          Set<SyncResult> syncResult) {
        barReq.fire(new Receiver<SimpleBarProxy>() {
          public void onSuccess(final SimpleBarProxy persistedBar,
              Set<SyncResult> syncResults) {
            assertEquals("Ray", persistedFoo.getUserName());
            final RequestObject<Void> fooReq2 = req.simpleFooRequest().persist(
                persistedFoo);
            SimpleFooProxy editablePersistedFoo = fooReq2.edit(persistedFoo);
            editablePersistedFoo.setBarField(persistedBar);
            fooReq2.fire(new Receiver<Void>() {
              public void onSuccess(Void response, Set<SyncResult> syncResults) {
                req.simpleFooRequest().findSimpleFooById(999L).with(
                    "barField.userName").fire(new Receiver<SimpleFooProxy>() {
                  public void onSuccess(SimpleFooProxy finalFooProxy,
                      Set<SyncResult> syncResults) {
                    assertEquals("Amit",
                        finalFooProxy.getBarField().getUserName());
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
    delayTestFinish(5000);

    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final RequestObject<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");
    rayFoo.setFooField(rayFoo);
    persistRay.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(final SimpleFooProxy persistedRay,
          Set<SyncResult> ignored) {
        finishTest();
      }
    });
  }

  public void testPersistRelation() {
    delayTestFinish(5000);

    SimpleFooProxy rayFoo = req.create(SimpleFooProxy.class);
    final RequestObject<SimpleFooProxy> persistRay = req.simpleFooRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");

    persistRay.fire(new Receiver<SimpleFooProxy>() {
      public void onSuccess(final SimpleFooProxy persistedRay,
          Set<SyncResult> ignored) {

        SimpleBarProxy amitBar = req.create(SimpleBarProxy.class);
        final RequestObject<SimpleBarProxy> persistAmit = req.simpleBarRequest().persistAndReturnSelf(
            amitBar);
        amitBar = persistAmit.edit(amitBar);
        amitBar.setUserName("Amit");

        persistAmit.fire(new Receiver<SimpleBarProxy>() {
          public void onSuccess(SimpleBarProxy persistedAmit,
              Set<SyncResult> ignored) {

            final RequestObject<SimpleFooProxy> persistRelationship = req.simpleFooRequest().persistAndReturnSelf(
                persistedRay).with("barField");
            SimpleFooProxy newRec = persistRelationship.edit(persistedRay);
            newRec.setBarField(persistedAmit);

            persistRelationship.fire(new Receiver<SimpleFooProxy>() {
              public void onSuccess(SimpleFooProxy relatedRay,
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
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testFetchEntity() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResult) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertEquals(null, response.getBarField());
            finishTest();
          }
        });
  }

  public void testFetchEntityWithRelation() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).with("barField").fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResult) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertNotNull(response.getBarField());
            finishTest();
          }
        });
  }

  public void testProxysAsInstanceMethodParams() {
    delayTestFinish(5000);
    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResult) {
            SimpleBarProxy bar = req.create(SimpleBarProxy.class);
            RequestObject<String> helloReq = req.simpleFooRequest().hello(
                response, bar);
            bar = helloReq.edit(bar);
            bar.setUserName("BAR");
            helloReq.fire(new Receiver<String>() {
              public void onSuccess(String response, Set<SyncResult> syncResults) {
                assertEquals("Greetings BAR from GWT", response);
                finishTest();
              }
            });
          }
        });
  }

  /*
   * tests that (a) any method can have a side effect that is handled correctly.
   * (b) instance methods are handled correctly.
   */
  public void testMethodWithSideEffects() {
    delayTestFinish(5000);

    req.simpleFooRequest().findSimpleFooById(999L).fire(
        new Receiver<SimpleFooProxy>() {

          public void onSuccess(SimpleFooProxy newFoo,
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
                EntityProxy proxy = syncResultArray[0].getProxy();
                assertEquals(new Long(999L), proxy.getId());
                // confirm that the instance method did have the desired
                // sideEffect.
                req.simpleFooRequest().findSimpleFooById(999L).fire(
                    new Receiver<SimpleFooProxy>() {
                      public void onSuccess(SimpleFooProxy finalFoo,
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
