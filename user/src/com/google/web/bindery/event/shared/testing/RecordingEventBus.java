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

import java.util.HashSet;
import java.util.Set;

/**
 * Wraps an {@link EventBus} implementation to keep track of fired events.
 */
public class RecordingEventBus extends EventBus {
  private final Set<Event<?>> firedEvents = new HashSet<Event<?>>();
  private final EventBus wrapped;

  public RecordingEventBus() {
    this(new SimpleEventBus());
  }

  public RecordingEventBus(EventBus wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
    return wrapped.addHandler(type, handler);
  }

  @Override
  public <H> HandlerRegistration addHandlerToSource(Type<H> type, Object source, H handler) {
    return wrapped.addHandlerToSource(type, source, handler);
  }

  /**
   * Clears the remembered list of fired events.
   */
  public void clearFiredEvents() {
    firedEvents.clear();
  }

  @Override
  public void fireEvent(Event<?> event) {
    wrapped.fireEvent(event);
    firedEvents.add(event);
  }

  @Override
  public void fireEventFromSource(Event<?> event, Object source) {
    firedEvents.add(event);
    wrapped.fireEventFromSource(event, source);
  }

  /**
   * Returns {@code true} if the specified event was fired.
   */
  public boolean wasEventFired(Event<?> event) {
    return firedEvents.contains(event);
  }
}
