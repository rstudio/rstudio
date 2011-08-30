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

package com.google.web.bindery.event.shared.testing;

import com.google.web.bindery.event.shared.BarEvent;
import com.google.web.bindery.event.shared.Event.Type;
import com.google.web.bindery.event.shared.EventBusTestBase;
import com.google.web.bindery.event.shared.FooEvent;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Eponymous unit tests.
 */
public class CountingEventBusTest extends EventBusTestBase {
  private CountingEventBus eventBus;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    eventBus = new CountingEventBus(new SimpleEventBus());
  }

  public void testAddAndRemoveMultipleHandlers() {
    HandlerRegistration fooReg = eventBus.addHandler(FooEvent.TYPE, fooHandler1);
    checkHandlerCount(1, FooEvent.TYPE);

    HandlerRegistration barReg1 = eventBus.addHandler(BarEvent.TYPE, barHandler1);
    HandlerRegistration barReg2 = eventBus.addHandler(BarEvent.TYPE, barHandler2);
    checkHandlerCount(2, BarEvent.TYPE);

    fooReg.removeHandler();
    checkHandlerCount(0, FooEvent.TYPE);

    barReg2.removeHandler();
    checkHandlerCount(1, BarEvent.TYPE);

    barReg1.removeHandler();
    checkHandlerCount(0, BarEvent.TYPE);
  }

  public void testAddAndRemoveSourcedHandlers() {
    Object source1 = new Object();
    Object source2 = new Object();
    
    HandlerRegistration fooReg1 = eventBus.addHandlerToSource(FooEvent.TYPE, source1, fooHandler1);
    checkHandlerCount(1, FooEvent.TYPE);

    HandlerRegistration fooReg2 = eventBus.addHandlerToSource(FooEvent.TYPE, source2, fooHandler2);
    checkHandlerCount(2, FooEvent.TYPE);

    fooReg2.removeHandler();
    checkHandlerCount(1, FooEvent.TYPE);

    fooReg1.removeHandler();
    checkHandlerCount(0, FooEvent.TYPE);
  }

  public void testFireEvent() {
    checkTotalEvents(0, FooEvent.TYPE);
    checkTotalEvents(0, BarEvent.TYPE);

    for (int i = 0; i < 5; i++) {
      eventBus.fireEvent(new FooEvent());
      checkTotalEvents(i + 1, FooEvent.TYPE);
      checkTotalEvents(i, BarEvent.TYPE);

      eventBus.fireEvent(new BarEvent());
      checkTotalEvents(i + 1, FooEvent.TYPE);
      checkTotalEvents(i + 1, BarEvent.TYPE);
    }
  }

  public void testFireEventFromSource() {
    Object source1 = new Object();
    Object source2 = new Object();
    
    eventBus.fireEvent(new FooEvent());
    checkSourceEvents(0, FooEvent.TYPE, source1);
    checkSourceEvents(0, FooEvent.TYPE, source2);
    checkTotalEvents(1, FooEvent.TYPE);

    eventBus.fireEventFromSource(new FooEvent(), source1);
    checkSourceEvents(1, FooEvent.TYPE, source1);
    checkSourceEvents(0, FooEvent.TYPE, source2);
    checkSourceEvents(1, FooEvent.TYPE, null);
    assertEquals(2, eventBus.getFiredCount(FooEvent.TYPE));

    eventBus.fireEventFromSource(new FooEvent(), source1);
    checkSourceEvents(2, FooEvent.TYPE, source1);
    checkSourceEvents(0, FooEvent.TYPE, source2);
    checkSourceEvents(1, FooEvent.TYPE, null);
    assertEquals(3, eventBus.getFiredCount(FooEvent.TYPE));

    eventBus.fireEventFromSource(new FooEvent(), source2);
    checkSourceEvents(2, FooEvent.TYPE, source1);
    checkSourceEvents(1, FooEvent.TYPE, source2);
    checkSourceEvents(1, FooEvent.TYPE, null);
    assertEquals(4, eventBus.getFiredCount(FooEvent.TYPE));

    eventBus.fireEventFromSource(new BarEvent(), source2);
    checkSourceEvents(2, FooEvent.TYPE, source1);
    checkSourceEvents(1, FooEvent.TYPE, source2);
    checkSourceEvents(1, FooEvent.TYPE, null);
    assertEquals(4, eventBus.getFiredCount(FooEvent.TYPE));
    checkSourceEvents(1, BarEvent.TYPE, source2);
    assertEquals(1, eventBus.getFiredCount(BarEvent.TYPE));
  }

  public void testFireEventFromSource_LotsOfEvents() {
    Object source = new Object();

    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      eventBus.fireEventFromSource(new FooEvent(), source);
      assertEquals(i + 1, eventBus.getFiredCount(FooEvent.TYPE));
      assertEquals(i + 1, eventBus.getFiredCountFromSource(FooEvent.TYPE, source));
    }
  }

  private void checkHandlerCount(int expected, Type<?> type) {
    assertEquals(expected, eventBus.getHandlerCount(type));
    assertEquals(expected, eventBus.getCount(type));
  }

  private void checkSourceEvents(int expectedCount, Type<?> type, Object source) {
    assertEquals(expectedCount, eventBus.getFiredCountFromSource(type, source));
  }

  private void checkTotalEvents(int expectedCount, Type<?> type) {
    assertEquals(expectedCount, eventBus.getFiredCount(type));
    assertEquals(expectedCount, eventBus.getFiredCountFromSource(type, null));
  }
}

