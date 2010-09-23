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

import com.google.gwt.requestfactory.client.impl.ProxyImpl;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleFooStringProxy;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.List;
import java.util.Set;

/**
 * Tests for {@link com.google.gwt.requestfactory.shared.RequestFactory}.
 */
public class RequestFactoryStringTest extends RequestFactoryTestBase {
  /*
   * DO NOT USE finishTest(). Instead, call finishTestAndReset();
   */

  private class FailFixAndRefire<T> extends Receiver<T> {

    private final SimpleFooStringProxy proxy;
    private final Request<T> request;
    private boolean voidReturnExpected;

    FailFixAndRefire(SimpleFooStringProxy proxy, Request<T> request) {
      this.proxy = request.edit(proxy);
      this.request = request;
    }

    @Override
    public void onSuccess(T response) {
      /*
       * Make sure your class path includes:
       * 
       * tools/lib/apache/log4j/log4j-1.2.16.jar
       * tools/lib/hibernate/validator/hibernate-validator-4.1.0.Final.jar
       * tools/lib/slf4j/slf4j-api/slf4j-api-1.6.1.jar
       * tools/lib/slf4j/slf4j-log4j12/slf4j-log4j12-1.6.1.jar
       */
      fail("Violations expected (you might be missing some jars, "
          + "see the comment above this line)");
    }

    @Override
    public void onViolation(Set<Violation> errors) {

      // size violation expected

      assertEquals(1, errors.size());
      Violation error = errors.iterator().next();
      assertEquals("userName", error.getPath());
      assertEquals("size must be between 3 and 30", error.getMessage());
      assertEquals(proxy.stableId(), error.getProxyId());

      // Now re-used the request to fix the edit

      proxy.setUserName("long enough");
      request.fire(new Receiver<T>() {
        @Override
        public void onSuccess(T response) {
          if (voidReturnExpected) {
            assertNull(response);
          } else {
            assertEquals(proxy.stableId(),
                ((SimpleFooStringProxy) response).stableId());
          }
          finishTestAndReset();
        }
      });
    }

    void doVoidTest() {
      voidReturnExpected = true;
      doTest();
    }

    void doTest() {
      proxy.setUserName("a"); // too short
      request.fire(this);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  public void testDummyCreate() {
    delayTestFinish(5000);

    final SimpleFooEventHandler<SimpleFooStringProxy> handler = 
      new SimpleFooEventHandler<SimpleFooStringProxy>();
    EntityProxyChange.registerForProxyType(
        req.getEventBus(), SimpleFooStringProxy.class, handler);

    final SimpleFooStringProxy foo = req.create(SimpleFooStringProxy.class);
    Object futureId = foo.getId();
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).isFuture());
    Request<SimpleFooStringProxy> fooReq = req.simpleFooStringRequest().persistAndReturnSelf(
        foo);
    fooReq.fire(new Receiver<SimpleFooStringProxy>() {

      @Override
      public void onSuccess(final SimpleFooStringProxy returned) {
        Object futureId = foo.getId();
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).isFuture());

        assertEquals(1, handler.acquireEventCount);
        assertEquals(1, handler.createEventCount);
        assertEquals(2, handler.totalEventCount);

        checkStableIdEquals(foo, returned);
        finishTestAndReset();
      }
    });
  }

  public void testDummyCreateBar() {
    delayTestFinish(5000);

    final SimpleBarProxy foo = req.create(SimpleBarProxy.class);
    Object futureId = foo.getId();
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).isFuture());
    Request<SimpleBarProxy> fooReq = req.simpleBarRequest().persistAndReturnSelf(
        foo);
    fooReq.fire(new Receiver<SimpleBarProxy>() {

      @Override
      public void onSuccess(final SimpleBarProxy returned) {
        Object futureId = foo.getId();
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).isFuture());

        checkStableIdEquals(foo, returned);
        finishTestAndReset();
      }
    });
  }

  public void testFetchEntity() {
    delayTestFinish(5000);
    req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
        new Receiver<SimpleFooStringProxy>() {
          @Override
          public void onSuccess(SimpleFooStringProxy response) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertEquals(null, response.getBarField());
            finishTestAndReset();
          }
        });
  }

  public void testFetchEntityWithRelation() {
    delayTestFinish(5000);
    req.simpleFooStringRequest().findSimpleFooStringById("999x").with("barField").fire(
        new Receiver<SimpleFooStringProxy>() {
          @Override
          public void onSuccess(SimpleFooStringProxy response) {
            assertEquals(42, (int) response.getIntId());
            assertEquals("GWT", response.getUserName());
            assertEquals(8L, (long) response.getLongField());
            assertEquals(com.google.gwt.requestfactory.shared.SimpleEnum.FOO,
                response.getEnumField());
            assertNotNull(response.getBarField());
            finishTestAndReset();
          }
        });
  }

  public void testGetEventBus() {
    assertEquals(eventBus, req.getEventBus());
  }

  public void testGetListStringId() {
    delayTestFinish(5000);

    // String ids
    req.simpleBarRequest().findAll().fire(new Receiver<List<SimpleBarProxy>>() {
      @Override
      public void onSuccess(List<SimpleBarProxy> response) {
        assertEquals(2, response.size());
        for (SimpleBarProxy bar : response) {
          assertNotNull(bar.stableId());
          finishTestAndReset();
        }
      }
    });
  }

  public void testGetListLongId() {
    delayTestFinish(5000);

    // Long ids
    req.simpleFooStringRequest().findAll().with("barField.userName").fire(
        new Receiver<List<SimpleFooStringProxy>>() {
          @Override
          public void onSuccess(List<SimpleFooStringProxy> response) {
            assertEquals(1, response.size());
            for (SimpleFooStringProxy foo : response) {
              assertNotNull(foo.stableId());
              assertEquals("FOO", foo.getBarField().getUserName());
              finishTestAndReset();
            }
          }
        });
  }

  /*
   * tests that (a) any method can have a side effect that is handled correctly.
   * (b) instance methods are handled correctly and (c) a request cannot be
   * reused after a successful response is received. (Yet?)
   */
  public void testMethodWithSideEffects() {
    delayTestFinish(5000);

    final SimpleFooEventHandler<SimpleFooStringProxy> handler = 
      new SimpleFooEventHandler<SimpleFooStringProxy>();
    EntityProxyChange.registerForProxyType(
        req.getEventBus(), SimpleFooStringProxy.class, handler);

    req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
        new Receiver<SimpleFooStringProxy>() {

          @Override
          public void onSuccess(SimpleFooStringProxy newFoo) {
            assertEquals(1, handler.acquireEventCount);
            assertEquals(1, handler.totalEventCount);
            final Request<Long> mutateRequest = req.simpleFooStringRequest().countSimpleFooWithUserNameSideEffect(
                newFoo);
            newFoo = mutateRequest.edit(newFoo);
            newFoo.setUserName("Ray");
            mutateRequest.fire(new Receiver<Long>() {
              @Override
              public void onSuccess(Long response) {
                assertCannotFire(mutateRequest);
                assertEquals(new Long(1L), response);
                assertEquals(1, handler.acquireEventCount);
                assertEquals(1, handler.updateEventCount);
                assertEquals(2, handler.totalEventCount);

                // confirm that the instance method did have the desired
                // sideEffect.
                req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
                    new Receiver<SimpleFooStringProxy>() {
                      @Override
                      public void onSuccess(SimpleFooStringProxy finalFoo) {
                        assertEquals("Ray", finalFoo.getUserName());
                        assertEquals(1, handler.acquireEventCount);
                        assertEquals(2, handler.updateEventCount);
                        assertEquals(3, handler.totalEventCount);
                        finishTestAndReset();
                      }
                    });
              }

            });

            try {
              newFoo.setUserName("Barney");
              fail();
            } catch (IllegalStateException e) {
              /* pass, cannot change a request that is in flight */
            }
          }
        });
  }

  /*
   * TODO: all these tests should check the final values. It will be easy when
   * we have better persistence than the singleton pattern.
   */
  public void testPersistExistingEntityExistingRelation() {
    delayTestFinish(5000);

    req.simpleBarRequest().findSimpleBarById("999L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy barProxy) {
            req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
                new Receiver<SimpleFooStringProxy>() {
                  @Override
                  public void onSuccess(SimpleFooStringProxy fooProxy) {
                    Request<Void> updReq = req.simpleFooStringRequest().persist(
                        fooProxy);
                    fooProxy = updReq.edit(fooProxy);
                    fooProxy.setBarField(barProxy);
                    updReq.fire(new Receiver<Void>() {
                      @Override
                      public void onSuccess(Void response) {

                        finishTestAndReset();
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

    // Make a new bar
    SimpleBarProxy makeABar = req.create(SimpleBarProxy.class);
    Request<SimpleBarProxy> persistRequest = req.simpleBarRequest().persistAndReturnSelf(
        makeABar);
    makeABar = persistRequest.edit(makeABar);
    makeABar.setUserName("Amit");

    persistRequest.fire(new Receiver<SimpleBarProxy>() {
      @Override
      public void onSuccess(final SimpleBarProxy persistedBar) {

        // It was made, now find a foo to assign it to
        req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
            new Receiver<SimpleFooStringProxy>() {
              @Override
              public void onSuccess(SimpleFooStringProxy response) {

                // Found the foo, edit it
                Request<Void> fooReq = req.simpleFooStringRequest().persist(
                    response);
                response = fooReq.edit(response);
                response.setBarField(persistedBar);
                fooReq.fire(new Receiver<Void>() {
                  @Override
                  public void onSuccess(Void response) {

                    // Foo was persisted, fetch it again check the goods
                    req.simpleFooStringRequest().findSimpleFooStringById("999x").with(
                        "barField.userName").fire(
                        new Receiver<SimpleFooStringProxy>() {

                          // Here it is
                          @Override
                          public void onSuccess(SimpleFooStringProxy finalFooProxy) {
                            assertEquals("Amit",
                                finalFooProxy.getBarField().getUserName());
                            finishTestAndReset();
                          }
                        });
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
    SimpleFooStringProxy newFoo = req.create(SimpleFooStringProxy.class);

    final Request<Void> fooReq = req.simpleFooStringRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final SimpleFooStringProxy finalFoo = newFoo;
    req.simpleBarRequest().findSimpleBarById("999L").fire(
        new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy response) {
            finalFoo.setBarField(response);
            fooReq.fire(new Receiver<Void>() {
              @Override
              public void onSuccess(Void response) {
                req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
                    new Receiver<SimpleFooStringProxy>() {
                      @Override
                      public void onSuccess(SimpleFooStringProxy finalFooProxy) {
                        // newFoo hasn't been persisted, so userName is the old
                        // value.
                        assertEquals("GWT", finalFooProxy.getUserName());
                        finishTestAndReset();
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
    SimpleFooStringProxy newFoo = req.create(SimpleFooStringProxy.class);
    SimpleBarProxy newBar = req.create(SimpleBarProxy.class);

    final Request<SimpleFooStringProxy> fooReq = req.simpleFooStringRequest().persistAndReturnSelf(
        newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Ray");

    final Request<SimpleBarProxy> barReq = req.simpleBarRequest().persistAndReturnSelf(
        newBar);
    newBar = barReq.edit(newBar);
    newBar.setUserName("Amit");

    fooReq.fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(final SimpleFooStringProxy persistedFoo) {
        barReq.fire(new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy persistedBar) {
            assertEquals("Ray", persistedFoo.getUserName());
            final Request<Void> fooReq2 = req.simpleFooStringRequest().persist(
                persistedFoo);
            SimpleFooStringProxy editablePersistedFoo = fooReq2.edit(persistedFoo);
            editablePersistedFoo.setBarField(persistedBar);
            fooReq2.fire(new Receiver<Void>() {
              @Override
              public void onSuccess(Void response) {
                req.simpleFooStringRequest().findSimpleFooStringById("999x").with(
                    "barField.userName").fire(new Receiver<SimpleFooStringProxy>() {
                  @Override
                  public void onSuccess(SimpleFooStringProxy finalFooProxy) {
                    assertEquals("Amit",
                        finalFooProxy.getBarField().getUserName());
                    finishTestAndReset();
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

    SimpleFooStringProxy rayFoo = req.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> persistRay = req.simpleFooStringRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");
    rayFoo.setFooField(rayFoo);
    persistRay.fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(final SimpleFooStringProxy persistedRay) {
        finishTestAndReset();
      }
    });
  }

  public void testPersistRelation() {
    delayTestFinish(5000);

    SimpleFooStringProxy rayFoo = req.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> persistRay = req.simpleFooStringRequest().persistAndReturnSelf(
        rayFoo);
    rayFoo = persistRay.edit(rayFoo);
    rayFoo.setUserName("Ray");

    persistRay.fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(final SimpleFooStringProxy persistedRay) {
        SimpleBarProxy amitBar = req.create(SimpleBarProxy.class);
        final Request<SimpleBarProxy> persistAmit = req.simpleBarRequest().persistAndReturnSelf(
            amitBar);
        amitBar = persistAmit.edit(amitBar);
        amitBar.setUserName("Amit");

        persistAmit.fire(new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy persistedAmit) {

            final Request<SimpleFooStringProxy> persistRelationship = req.simpleFooStringRequest().persistAndReturnSelf(
                persistedRay).with("barField");
            SimpleFooStringProxy newRec = persistRelationship.edit(persistedRay);
            newRec.setBarField(persistedAmit);

            persistRelationship.fire(new Receiver<SimpleFooStringProxy>() {
              @Override
              public void onSuccess(SimpleFooStringProxy relatedRay) {
                assertEquals("Amit", relatedRay.getBarField().getUserName());
                finishTestAndReset();
              }
            });
          }
        });
      }
    });
  }

  public void testProxysAsInstanceMethodParams() {
    delayTestFinish(5000);
    req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
        new Receiver<SimpleFooStringProxy>() {
          @Override
          public void onSuccess(SimpleFooStringProxy response) {
            SimpleBarProxy bar = req.create(SimpleBarProxy.class);
            Request<String> helloReq = req.simpleFooStringRequest().hello(
                response, bar);
            bar = helloReq.edit(bar);
            bar.setUserName("BAR");
            helloReq.fire(new Receiver<String>() {
              @Override
              public void onSuccess(String response) {
                assertEquals("Greetings BAR from GWT", response);
                finishTestAndReset();
              }
            });
          }
        });
  }

  public void testServerFailure() {
    delayTestFinish(5000);

    SimpleFooStringProxy newFoo = req.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> persistRequest = req.simpleFooStringRequest().persistAndReturnSelf(
        newFoo);

    final SimpleFooStringProxy mutableFoo = persistRequest.edit(newFoo);
    mutableFoo.setPleaseCrash(42); // 42 is the crash causing magic number

    persistRequest.fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onFailure(ServerFailure error) {
        assertEquals("Server Error: THIS EXCEPTION IS EXPECTED BY A TEST",
            error.getMessage());
        assertEquals("", error.getExceptionType());
        assertEquals("", error.getStackTraceString());

        // Now show that we can fix the error and try again with the same
        // request

        mutableFoo.setPleaseCrash(24); // Only 42 crashes
        persistRequest.fire(new Receiver<SimpleFooStringProxy>() {
          @Override
          public void onSuccess(SimpleFooStringProxy response) {
            finishTestAndReset();
          }
        });
      }

      @Override
      public void onSuccess(SimpleFooStringProxy response) {
        fail("Failure expected but onSuccess() was called");
      }

      @Override
      public void onViolation(Set<Violation> errors) {
        fail("Failure expected but onViolation() was called");
      }
    });
  }

  public void testStableId() {
    delayTestFinish(5000);

    final SimpleFooStringProxy foo = req.create(SimpleFooStringProxy.class);
    final Object futureId = foo.getId();
    assertTrue(((ProxyImpl) foo).isFuture());
    Request<SimpleFooStringProxy> fooReq = req.simpleFooStringRequest().persistAndReturnSelf(
        foo);

    final SimpleFooStringProxy newFoo = fooReq.edit(foo);
    assertEquals(futureId, foo.getId());
    assertTrue(((ProxyImpl) foo).isFuture());
    assertEquals(futureId, newFoo.getId());
    assertTrue(((ProxyImpl) newFoo).isFuture());

    newFoo.setUserName("GWT basic user");
    fooReq.fire(new Receiver<SimpleFooStringProxy>() {

      @Override
      public void onSuccess(final SimpleFooStringProxy returned) {
        assertEquals(futureId, foo.getId());
        assertTrue(((ProxyImpl) foo).isFuture());
        assertEquals(futureId, newFoo.getId());
        assertTrue(((ProxyImpl) newFoo).isFuture());

        assertFalse(((ProxyImpl) returned).isFuture());

        checkStableIdEquals(foo, returned);
        checkStableIdEquals(newFoo, returned);

        Request<SimpleFooStringProxy> editRequest = req.simpleFooStringRequest().persistAndReturnSelf(
            returned);
        final SimpleFooStringProxy editableFoo = editRequest.edit(returned);
        editableFoo.setUserName("GWT power user");
        editRequest.fire(new Receiver<SimpleFooStringProxy>() {

          @Override
          public void onSuccess(SimpleFooStringProxy returnedAfterEdit) {
            checkStableIdEquals(editableFoo, returnedAfterEdit);
            assertEquals(returnedAfterEdit.getId(), returned.getId());
            finishTestAndReset();
          }
        });
      }
    });
  }

  public void testViolationAbsent() {
    delayTestFinish(5000);

    SimpleFooStringProxy newFoo = req.create(SimpleFooStringProxy.class);
    final Request<Void> fooReq = req.simpleFooStringRequest().persist(newFoo);

    newFoo = fooReq.edit(newFoo);
    newFoo.setUserName("Amit"); // will not cause violation.

    fooReq.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void ignore) {
        finishTestAndReset();
      }
    });
  }

  public void testViolationsOnCreate() {
    delayTestFinish(5000);

    SimpleFooStringProxy newFoo = req.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> create = req.simpleFooStringRequest().persistAndReturnSelf(
        newFoo);
    new FailFixAndRefire<SimpleFooStringProxy>(newFoo, create).doTest();
  }

  public void testViolationsOnCreateVoidReturn() {
    delayTestFinish(5000);

    SimpleFooStringProxy newFoo = req.create(SimpleFooStringProxy.class);
    final Request<Void> create = req.simpleFooStringRequest().persist(newFoo);
    new FailFixAndRefire<Void>(newFoo, create).doVoidTest();
  }

  public void testViolationsOnEdit() {
    delayTestFinish(5000);

    fooCreationRequest().fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(SimpleFooStringProxy returned) {
        Request<SimpleFooStringProxy> editRequest = req.simpleFooStringRequest().persistAndReturnSelf(
            returned);
        new FailFixAndRefire<SimpleFooStringProxy>(returned, editRequest).doTest();
      }
    });
  }

  public void testViolationsOnEditVoidReturn() {
    delayTestFinish(5000);

    fooCreationRequest().fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(SimpleFooStringProxy returned) {
        Request<Void> editRequest = req.simpleFooStringRequest().persist(
            returned);
        new FailFixAndRefire<Void>(returned, editRequest).doVoidTest();
      }
    });
  }

  private void assertCannotFire(final Request<Long> mutateRequest) {
    try {
      mutateRequest.fire(new Receiver<Long>() {
        @Override
        public void onSuccess(Long response) {
          fail("Should not be called");
        }
      });
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      /* cannot reuse a successful request, mores the pity */
    }
  }

  private Request<SimpleFooStringProxy> fooCreationRequest() {
    SimpleFooStringProxy originalFoo = req.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> fooReq = req.simpleFooStringRequest().persistAndReturnSelf(
        originalFoo);
    originalFoo = fooReq.edit(originalFoo);
    originalFoo.setUserName("GWT User");
    return fooReq;
  }
}
