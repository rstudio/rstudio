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
package com.google.web.bindery.event.shared;

import com.google.web.bindery.event.shared.testing.CountingEventBus;

/**
 * Eponymous unit test.
 */
public class ResettableEventBusTest extends EventBusTestBase {
  public void testSimple() {
    CountingEventBus wrapped = new CountingEventBus();
    ResettableEventBus subject = new ResettableEventBus(wrapped);

    Event.Type<FooEvent.Handler> type = FooEvent.TYPE;

    assertEquals(0, wrapped.getCount(type));

    subject.addHandler(type, fooHandler1);
    subject.addHandlerToSource(type, "baker", fooHandler2);
    subject.addHandler(type, fooHandler3);

    assertEquals(3, wrapped.getCount(type));

    subject.fireEvent(new FooEvent());
    assertFired(fooHandler1, fooHandler3);
    assertNotFired(fooHandler2);

    reset();

    subject.fireEventFromSource(new FooEvent(), "baker");
    assertFired(fooHandler1, fooHandler2, fooHandler3);

    reset();

    subject.removeHandlers();
    assertEquals(0, wrapped.getCount(type));

    subject.fireEvent(new FooEvent());
    assertNotFired(fooHandler1, fooHandler2, fooHandler3);
  }

  public void testNestedResetInnerFirst() {
    CountingEventBus wrapped = new CountingEventBus();
    ResettableEventBus wideScope = new ResettableEventBus(wrapped);
    ResettableEventBus narrowScope = new ResettableEventBus(wideScope);

    Event.Type<FooEvent.Handler> type = FooEvent.TYPE;

    wideScope.addHandler(type, fooHandler1);
    narrowScope.addHandler(type, fooHandler2);

    wrapped.fireEvent(new FooEvent());
    assertFired(fooHandler1, fooHandler2);

    reset();

    /*
     * When I remove handlers from the narrow resettable, it should have no
     * effect on handlers registered with the wider instance.
     */

    narrowScope.removeHandlers();

    wrapped.fireEvent(new FooEvent());
    assertFired(fooHandler1);
    assertNotFired(fooHandler2);
  }

  public void testNestedResetOuterFirst() {
    CountingEventBus wrapped = new CountingEventBus();
    ResettableEventBus wideScope = new ResettableEventBus(wrapped);
    ResettableEventBus narrowScope = new ResettableEventBus(wideScope);

    Event.Type<FooEvent.Handler> type = FooEvent.TYPE;

    wideScope.addHandler(type, fooHandler1);
    narrowScope.addHandler(type, fooHandler2);

    wrapped.fireEvent(new FooEvent());
    assertFired(fooHandler1, fooHandler2);

    reset();

    /*
     * When I remove handlers from the first resettable, handlers registered by
     * the narrower scoped one that wraps it should also be severed.
     */

    wideScope.removeHandlers();

    wrapped.fireEvent(new FooEvent());
    assertNotFired(fooHandler1);
    assertNotFired(fooHandler2);
  }

  public void testManualRemoveMemory() {
    SimpleEventBus eventBus = new SimpleEventBus();
    ResettableEventBus subject = new ResettableEventBus(eventBus);

    Event.Type<FooEvent.Handler> type = FooEvent.TYPE;

    HandlerRegistration registration1 = subject.addHandler(type, fooHandler1);
    HandlerRegistration registration2 = subject.addHandler(type, fooHandler2);
    HandlerRegistration registration3 = subject.addHandler(type, fooHandler3);

    registration1.removeHandler();
    registration2.removeHandler();
    registration3.removeHandler();

    /*
     * removing handlers manually should remove registration from the internal
     * set.
     */

    assertEquals(0, subject.getRegistrationSize());

    subject.removeHandlers();

    // Expect nothing to happen. Especially no exceptions.
    registration1.removeHandler();
  }

  public void testNestedRemoveMemory() {
    SimpleEventBus eventBus = new SimpleEventBus();
    ResettableEventBus wideScope = new ResettableEventBus(eventBus);
    ResettableEventBus narrowScope = new ResettableEventBus(wideScope);

    Event.Type<FooEvent.Handler> type = FooEvent.TYPE;

    wideScope.addHandler(type, fooHandler1);
    narrowScope.addHandler(type, fooHandler2);
    narrowScope.addHandler(type, fooHandler3);

    narrowScope.removeHandlers();
    wideScope.removeHandlers();

    /*
     * Internal registeration should be empty after calling removeHandlers
     */

    assertEquals(0, wideScope.getRegistrationSize());
    assertEquals(0, narrowScope.getRegistrationSize());

    wideScope.addHandler(type, fooHandler1);
    narrowScope.addHandler(type, fooHandler2);

    /*
     * Reverse remove order
     */

    wideScope.removeHandlers();
    narrowScope.removeHandlers();

    assertEquals(0, wideScope.getRegistrationSize());
    assertEquals(0, narrowScope.getRegistrationSize());
  }
}
