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

import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleBarRequest;
import com.google.gwt.requestfactory.shared.SimpleFooStringProxy;
import com.google.gwt.requestfactory.shared.SimpleFooStringRequest;
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

    FailFixAndRefire(SimpleFooStringProxy proxy, RequestContext context,
        Request<T> request) {
      this.proxy = context.edit(proxy);
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

    final SimpleFooEventHandler<SimpleFooStringProxy> handler = new SimpleFooEventHandler<SimpleFooStringProxy>();
    EntityProxyChange.registerForProxyType(req.getEventBus(),
        SimpleFooStringProxy.class, handler);

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    final SimpleFooStringProxy foo = context.create(SimpleFooStringProxy.class);
    Object futureId = foo.getId();
    assertEquals(futureId, foo.getId());
    Request<SimpleFooStringProxy> fooReq = context.persistAndReturnSelf().using(
        foo);
    fooReq.fire(new Receiver<SimpleFooStringProxy>() {

      @Override
      public void onSuccess(final SimpleFooStringProxy returned) {
        Object futureId = foo.getId();
        assertEquals(futureId, foo.getId());

        /*
         * Two events are fired: (i) PERSIST event fired from
         * DeltaValueStoreJsonImpl because the proxy was persisted, and (ii)
         * UPDATE event fired from ValueStoreJsonImpl because the new proxy was
         * part of the return value.
         */
        assertEquals(1, handler.persistEventCount);
        assertEquals(1, handler.updateEventCount);
        assertEquals(2, handler.totalEventCount);

        checkStableIdEquals(foo, returned);
        finishTestAndReset();
      }
    });
  }

  public void testDummyCreateBar() {
    delayTestFinish(5000);

    SimpleBarRequest context = req.simpleBarRequest();
    final SimpleBarProxy foo = context.create(SimpleBarProxy.class);
    Request<SimpleBarProxy> fooReq = context.persistAndReturnSelf().using(foo);
    fooReq.fire(new Receiver<SimpleBarProxy>() {

      @Override
      public void onSuccess(final SimpleBarProxy returned) {
        checkStableIdEquals(foo, returned);
        finishTestAndReset();
      }
    });
  }

  public void testFindFindEdit() {
    delayTestFinish(5000);

    final SimpleFooEventHandler<SimpleFooStringProxy> handler = new SimpleFooEventHandler<SimpleFooStringProxy>();
    EntityProxyChange.registerForProxyType(req.getEventBus(),
        SimpleFooStringProxy.class, handler);

    req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
        new Receiver<SimpleFooStringProxy>() {

          @Override
          public void onSuccess(SimpleFooStringProxy newFoo) {
            assertEquals(1, handler.updateEventCount);
            assertEquals(1, handler.totalEventCount);

            req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
                new Receiver<SimpleFooStringProxy>() {

                  @Override
                  public void onSuccess(SimpleFooStringProxy newFoo) {
                    // no events are fired second time.
                    assertEquals(1, handler.updateEventCount);
                    assertEquals(1, handler.totalEventCount);
                    SimpleFooStringRequest context = req.simpleFooStringRequest();
                    final Request<Void> mutateRequest = context.persist().using(
                        newFoo);
                    newFoo = context.edit(newFoo);
                    newFoo.setUserName("Ray");
                    mutateRequest.fire(new Receiver<Void>() {
                      @Override
                      public void onSuccess(Void response) {
                        // events fired on updates.
                        assertEquals(2, handler.updateEventCount);
                        assertEquals(2, handler.totalEventCount);

                        finishTestAndReset();
                      }
                    });
                  }
                });
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
    req.simpleFooStringRequest().findSimpleFooStringById("999x").with(
        "barField").fire(new Receiver<SimpleFooStringProxy>() {
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

    final SimpleFooEventHandler<SimpleFooStringProxy> handler = new SimpleFooEventHandler<SimpleFooStringProxy>();
    EntityProxyChange.registerForProxyType(req.getEventBus(),
        SimpleFooStringProxy.class, handler);

    req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
        new Receiver<SimpleFooStringProxy>() {

          @Override
          public void onSuccess(SimpleFooStringProxy newFoo) {
            assertEquals(1, handler.updateEventCount);
            assertEquals(1, handler.totalEventCount);
            SimpleFooStringRequest context = req.simpleFooStringRequest();
            final Request<Long> mutateRequest = context.countSimpleFooWithUserNameSideEffect().using(
                newFoo);
            newFoo = context.edit(newFoo);
            newFoo.setUserName("Ray");
            mutateRequest.fire(new Receiver<Long>() {
              @Override
              public void onSuccess(Long response) {
                assertCannotFire(mutateRequest);
                assertEquals(new Long(1L), response);
                assertEquals(2, handler.updateEventCount);
                assertEquals(2, handler.totalEventCount);

                // confirm that the instance method did have the desired
                // sideEffect.
                req.simpleFooStringRequest().findSimpleFooStringById("999x").fire(
                    new Receiver<SimpleFooStringProxy>() {
                      @Override
                      public void onSuccess(SimpleFooStringProxy finalFoo) {
                        assertEquals("Ray", finalFoo.getUserName());
                        assertEquals(2, handler.updateEventCount);
                        assertEquals(2, handler.totalEventCount);
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
                    SimpleFooStringRequest context = req.simpleFooStringRequest();
                    Request<Void> updReq = context.persist().using(fooProxy);
                    fooProxy = context.edit(fooProxy);
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
    SimpleBarRequest context = req.simpleBarRequest();
    SimpleBarProxy makeABar = context.create(SimpleBarProxy.class);
    Request<SimpleBarProxy> persistRequest = context.persistAndReturnSelf().using(
        makeABar);
    makeABar = context.edit(makeABar);
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
                SimpleFooStringRequest context = req.simpleFooStringRequest();
                Request<Void> fooReq = context.persist().using(response);
                response = context.edit(response);
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
                          public void onSuccess(
                              SimpleFooStringProxy finalFooProxy) {
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
    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy newFoo = context.create(SimpleFooStringProxy.class);
    final Request<Void> fooReq = context.persist().using(newFoo);

    newFoo = context.edit(newFoo);
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
    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy newFoo = context.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> fooReq = context.persistAndReturnSelf().using(
        newFoo);

    newFoo = context.edit(newFoo);
    newFoo.setUserName("Ray");

    SimpleBarRequest contextBar = req.simpleBarRequest();
    SimpleBarProxy newBar = contextBar.create(SimpleBarProxy.class);
    final Request<SimpleBarProxy> barReq = contextBar.persistAndReturnSelf().using(
        newBar);
    newBar = contextBar.edit(newBar);
    newBar.setUserName("Amit");

    fooReq.fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(final SimpleFooStringProxy persistedFoo) {
        barReq.fire(new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(final SimpleBarProxy persistedBar) {
            assertEquals("Ray", persistedFoo.getUserName());
            SimpleFooStringRequest context = req.simpleFooStringRequest();
            final Request<Void> fooReq2 = context.persist().using(persistedFoo);
            SimpleFooStringProxy editablePersistedFoo = context.edit(persistedFoo);
            editablePersistedFoo.setBarField(persistedBar);
            fooReq2.fire(new Receiver<Void>() {
              @Override
              public void onSuccess(Void response) {
                req.simpleFooStringRequest().findSimpleFooStringById("999x").with(
                    "barField.userName").fire(
                    new Receiver<SimpleFooStringProxy>() {
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

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy rayFoo = context.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> persistRay = context.persistAndReturnSelf().using(
        rayFoo);
    rayFoo = context.edit(rayFoo);
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

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy rayFoo = context.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> persistRay = context.persistAndReturnSelf().using(
        rayFoo);
    rayFoo = context.edit(rayFoo);
    rayFoo.setUserName("Ray");

    persistRay.fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(final SimpleFooStringProxy persistedRay) {
        SimpleBarRequest context = req.simpleBarRequest();
        SimpleBarProxy amitBar = context.create(SimpleBarProxy.class);
        final Request<SimpleBarProxy> persistAmit = context.persistAndReturnSelf().using(
            amitBar);
        amitBar = context.edit(amitBar);
        amitBar.setUserName("Amit");

        persistAmit.fire(new Receiver<SimpleBarProxy>() {
          @Override
          public void onSuccess(SimpleBarProxy persistedAmit) {

            SimpleFooStringRequest context = req.simpleFooStringRequest();
            final Request<SimpleFooStringProxy> persistRelationship = context.persistAndReturnSelf().using(
                persistedRay).with("barField");
            SimpleFooStringProxy newRec = context.edit(persistedRay);
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
            SimpleFooStringRequest context = req.simpleFooStringRequest();
            SimpleBarProxy bar = context.create(SimpleBarProxy.class);
            Request<String> helloReq = context.hello(bar).using(response);
            bar = context.edit(bar);
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

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy newFoo = context.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> persistRequest = context.persistAndReturnSelf().using(
        newFoo);

    final SimpleFooStringProxy mutableFoo = context.edit(newFoo);
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

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    final SimpleFooStringProxy foo = context.create(SimpleFooStringProxy.class);
    final Object futureId = foo.getId();
    Request<SimpleFooStringProxy> fooReq = context.persistAndReturnSelf().using(
        foo);

    final SimpleFooStringProxy newFoo = context.edit(foo);
    assertEquals(futureId, foo.getId());
    assertEquals(futureId, newFoo.getId());

    newFoo.setUserName("GWT basic user");
    fooReq.fire(new Receiver<SimpleFooStringProxy>() {

      @Override
      public void onSuccess(final SimpleFooStringProxy returned) {
        assertEquals(futureId, foo.getId());
        assertEquals(futureId, newFoo.getId());

        checkStableIdEquals(foo, returned);
        checkStableIdEquals(newFoo, returned);

        SimpleFooStringRequest context = req.simpleFooStringRequest();
        Request<SimpleFooStringProxy> editRequest = context.persistAndReturnSelf().using(
            returned);
        final SimpleFooStringProxy editableFoo = context.edit(returned);
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

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy newFoo = context.create(SimpleFooStringProxy.class);
    final Request<Void> fooReq = context.persist().using(newFoo);

    newFoo = context.edit(newFoo);
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

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy newFoo = context.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> create = context.persistAndReturnSelf().using(
        newFoo);
    new FailFixAndRefire<SimpleFooStringProxy>(newFoo, context, create).doTest();
  }

  public void testViolationsOnCreateVoidReturn() {
    delayTestFinish(5000);

    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy newFoo = context.create(SimpleFooStringProxy.class);
    final Request<Void> create = context.persist().using(newFoo);
    new FailFixAndRefire<Void>(newFoo, context, create).doVoidTest();
  }

  public void testViolationsOnEdit() {
    delayTestFinish(5000);

    fooCreationRequest().fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(SimpleFooStringProxy returned) {
        SimpleFooStringRequest context = req.simpleFooStringRequest();
        Request<SimpleFooStringProxy> editRequest = context.persistAndReturnSelf().using(
            returned);
        new FailFixAndRefire<SimpleFooStringProxy>(returned, context,
            editRequest).doTest();
      }
    });
  }

  public void testViolationsOnEditVoidReturn() {
    delayTestFinish(5000);

    fooCreationRequest().fire(new Receiver<SimpleFooStringProxy>() {
      @Override
      public void onSuccess(SimpleFooStringProxy returned) {
        SimpleFooStringRequest context = req.simpleFooStringRequest();
        Request<Void> editRequest = context.persist().using(returned);
        new FailFixAndRefire<Void>(returned, context, editRequest).doVoidTest();
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
    SimpleFooStringRequest context = req.simpleFooStringRequest();
    SimpleFooStringProxy originalFoo = context.create(SimpleFooStringProxy.class);
    final Request<SimpleFooStringProxy> fooReq = context.persistAndReturnSelf().using(
        originalFoo);
    originalFoo = context.edit(originalFoo);
    originalFoo.setUserName("GWT User");
    return fooReq;
  }
}
