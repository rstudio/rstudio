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
package com.google.gwt.event.shared;

import com.google.gwt.event.shared.GwtEvent.Type;

import junit.framework.TestCase;

/**
 * Test that EventBus is api compatible after its retrofit to extend
 * {@link com.google.web.bindery.event.shared.EventBus}.
 */
public class EventBusTest extends TestCase {
  EventBus bus = new EventBus() {

    @Override
    public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type, H handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <H extends EventHandler> HandlerRegistration addHandlerToSource(Type<H> type,
        Object source, H handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void fireEventFromSource(GwtEvent<?> event, Object source) {
      throw new UnsupportedOperationException();
    }
  };
  
  public void testOne() {
    // Nothing to test, really, just make sure it still compiles.
    assertNotNull(bus);
  }
}
