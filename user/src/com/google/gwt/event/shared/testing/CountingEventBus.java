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
package com.google.gwt.event.shared.testing;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps an {@link EventBus} to keep a count of registered handlers. Handy for
 * tests.
 */
public class CountingEventBus extends EventBus {
  private final Map<Type<?>, Integer> counts = new HashMap<GwtEvent.Type<?>, Integer>();
  private final EventBus wrapped;
  
  public CountingEventBus() {
    this(new SimpleEventBus());
  }
  
  public CountingEventBus(EventBus wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type,
      H handler) {
    increment(type);
    final HandlerRegistration superReg = wrapped.addHandler(type, handler);
    return makeReg(type, superReg);
  }

  @Override
  public <H extends EventHandler> HandlerRegistration addHandlerToSource(
      final Type<H> type, Object source, H handler) {
    increment(type);
    final HandlerRegistration superReg = wrapped.addHandlerToSource(type,
        source, handler);
    return makeReg(type, superReg);
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    wrapped.fireEvent(event);
  }

  @Override
  public void fireEventFromSource(GwtEvent<?> event, Object source) {
    wrapped.fireEventFromSource(event, source);
  }

  public int getCount(Type<?> type) {
    Integer count = counts.get(type);
    return count == null ? 0 : count;
  }

  private void decrement(Type<?> type) {
    Integer count = counts.get(type);
    if (count == null) {
      count = 0;
    }
    counts.put(type, count - 1);
  }

  private <H> void increment(final Type<H> type) {
    Integer count = counts.get(type);
    if (count == null) {
      count = 0;
    }
    counts.put(type, count + 1);
  }

  private <H> HandlerRegistration makeReg(final Type<H> type,
      final HandlerRegistration superReg) {
    return new HandlerRegistration() {
      public void removeHandler() {
        decrement(type);
        superReg.removeHandler();
      }
    };
  }
}
