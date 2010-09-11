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
package com.google.gwt.event.shared;

import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.testing.CountingEventBus;

/**
 * Eponymous unit test.
 */
public class ResettableEventBusTest extends HandlerTestBase {
  public void testSimple() {
    CountingEventBus wrapped = new CountingEventBus();
    ResettableEventBus subject = new ResettableEventBus(wrapped);

    Type<MouseDownHandler> type = MouseDownEvent.getType();

    assertEquals(0, wrapped.getCount(type));

    subject.addHandler(type, mouse1);
    subject.addHandlerToSource(type, "baker", mouse2);
    subject.addHandler(type, mouse3);

    assertEquals(3, wrapped.getCount(type));

    subject.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse3);
    assertNotFired(mouse2);

    reset();

    subject.fireEventFromSource(new MouseDownEvent() {
    }, "baker");
    assertFired(mouse1, mouse2, mouse3);

    reset();
    
    subject.removeHandlers();
    assertEquals(0, wrapped.getCount(type));

    subject.fireEvent(new MouseDownEvent() {
    });
    assertNotFired(mouse1, mouse2, mouse3);
  }
}
