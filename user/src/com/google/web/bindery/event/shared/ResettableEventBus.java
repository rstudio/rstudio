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

import com.google.web.bindery.event.shared.Event.Type;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Wraps an EventBus to hold on to any HandlerRegistrations, so that they can
 * easily all be cleared at once.
 */
public class ResettableEventBus extends EventBus {
  private final EventBus wrapped;
  private final Set<HandlerRegistration> registrations = new HashSet<HandlerRegistration>();

  public ResettableEventBus(EventBus wrappedBus) {
    this.wrapped = wrappedBus;
  }

  @Override
  public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
    HandlerRegistration rtn = wrapped.addHandler(type, handler);
    return doRegisterHandler(rtn);
  }

  @Override
  public <H> HandlerRegistration addHandlerToSource(Event.Type<H> type, Object source, H handler) {
    HandlerRegistration rtn = wrapped.addHandlerToSource(type, source, handler);
    return doRegisterHandler(rtn);
  }

  @Override
  public void fireEvent(Event<?> event) {
    wrapped.fireEvent(event);
  }

  @Override
  public void fireEventFromSource(Event<?> event, Object source) {
    wrapped.fireEventFromSource(event, source);
  }

  /**
   * Remove all handlers that have been added through this wrapper.
   */
  public void removeHandlers() {
    Iterator<HandlerRegistration> it = registrations.iterator();
    while (it.hasNext()) {
      HandlerRegistration r = it.next();

      /*
       * must remove before we call removeHandler. Might have come from nested
       * ResettableEventBus
       */
      it.remove();

      r.removeHandler();
    }
  }

  /**
   *  Visible for testing.
   */
  protected int getRegistrationSize() {
    return registrations.size();
  }

  private HandlerRegistration doRegisterHandler(final HandlerRegistration registration) {
    registrations.add(registration);
    return new HandlerRegistration() {
      public void removeHandler() {
        doUnregisterHandler(registration);
      }
    };
  }

  private void doUnregisterHandler(HandlerRegistration registration) {
    if (registrations.contains(registration)) {
      registration.removeHandler();
      registrations.remove(registration);
    }
  }
}
