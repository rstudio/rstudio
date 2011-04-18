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

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.Event.Type;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps an {@link EventBus} to keep a count of registered handlers. Handy for
 * tests.
 */
public class CountingEventBus extends EventBus {
  private final Map<Type<?>, Integer> counts = new HashMap<Event.Type<?>, Integer>();
  private final EventBus wrapped;

  public CountingEventBus() {
    this(new SimpleEventBus());
  }

  public CountingEventBus(EventBus wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
    increment(type);
    final HandlerRegistration superReg = wrapped.addHandler(type, handler);
    return makeReg(type, superReg);
  }

  @Override
  public <H> HandlerRegistration addHandlerToSource(final Type<H> type, Object source, H handler) {
    increment(type);
    final HandlerRegistration superReg = wrapped.addHandlerToSource(type, source, handler);
    return makeReg(type, superReg);
  }

  @Override
  public void fireEvent(Event<?> event) {
    wrapped.fireEvent(event);
  }

  @Override
  public void fireEventFromSource(Event<?> event, Object source) {
    wrapped.fireEventFromSource(event, source);
  }

  public int getCount(Type<?> type) {
    Integer count = counts.get(type);
    return count == null ? 0 : count;
  }

  private void decrement(Type<?> type) {
    counts.put(type, getCount(type) - 1);
  }

  private <H> void increment(final Type<H> type) {
    counts.put(type, getCount(type) + 1);
  }

  private <H> HandlerRegistration makeReg(final Type<H> type, final HandlerRegistration superReg) {
    return new HandlerRegistration() {
      public void removeHandler() {
        decrement(type);
        superReg.removeHandler();
      }
    };
  }
}
