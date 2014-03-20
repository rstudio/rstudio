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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.Event.Type;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
 * Legacy compatibility wrapper for
 * {@link com.google.web.bindery.event.shared.testing.CountingEventBus}.
 */
public class CountingEventBus extends com.google.gwt.event.shared.EventBus {
  private final com.google.web.bindery.event.shared.testing.CountingEventBus real;

  public CountingEventBus() {
    real = new com.google.web.bindery.event.shared.testing.CountingEventBus();
  }

  public CountingEventBus(com.google.gwt.event.shared.EventBus wrapped) {
    real = new com.google.web.bindery.event.shared.testing.CountingEventBus(wrapped);
  }

  public <H extends EventHandler> com.google.gwt.event.shared.HandlerRegistration addHandler(
      GwtEvent.Type<H> type, H handler) {
    return wrap(addHandler((Event.Type<H>) type, handler));
  }

  @Override
  public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
    return real.addHandler(type, handler);
  }

  public <H extends EventHandler> com.google.gwt.event.shared.HandlerRegistration addHandlerToSource(
      GwtEvent.Type<H> type, Object source, H handler) {
    return wrap(addHandlerToSource((Event.Type<H>) type, source, handler));
  }
  
  @Override
  public <H> HandlerRegistration addHandlerToSource(Type<H> type, Object source, H handler) {
    return real.addHandlerToSource(type, source, handler);
  }

  @Override
  public void fireEvent(Event<?> event) {
    real.fireEvent(event);
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    castFireEvent(event);
  }

  @Override
  public void fireEventFromSource(Event<?> event, Object source) {
    real.fireEventFromSource(event, source);
  }

  @Override
  public void fireEventFromSource(GwtEvent<?> event, Object source) {
    castFireEventFromSource(event, source);
  }

  /**
   * How many handlers are registered for the given {@code type}.
   *
   * @deprecated Please use {@code getHandlerCount}.
   */
  @Deprecated
  public int getCount(GwtEvent.Type<?> type) {
    return real.getCount(type);
  }

  /**
   * How many events have fired for the given {@code type}. These events may not have been
   * passed to any handlers.
   */
  public int getFiredCount(Type<?> type) {
    return real.getFiredCount(type);
  }

  /**
   * How many handlers are registered for the given {@code type}.
   */
  public int getHandlerCount(Type<?> type) {
    return real.getHandlerCount(type);
  }
}
