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

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.Event.Type;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
 * Wraps {com.google.web.bindery.event.shared.ResettableEventBus} for legacy
 * compatibility.
 */
public class ResettableEventBus extends EventBus {
  private static class TestableResettableEventBus extends com.google.web.bindery.event.shared.ResettableEventBus {
    private TestableResettableEventBus(com.google.web.bindery.event.shared.EventBus wrappedBus) {
      super(wrappedBus);
    }

    @Override
    public int getRegistrationSize() {
      return super.getRegistrationSize();
    }
  }

  private final TestableResettableEventBus real;

  public ResettableEventBus(com.google.web.bindery.event.shared.EventBus wrappedBus) {
    real = new TestableResettableEventBus(wrappedBus);
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

  public void removeHandlers() {
    real.removeHandlers();
  }

  /**
   * Visible for testing.
   */
  int getRegistrationSize() {
    return real.getRegistrationSize();
  }
}
