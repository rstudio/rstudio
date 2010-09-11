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

import com.google.gwt.event.shared.GwtEvent.Type;

import java.util.HashSet;
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
  public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type,
      H handler) {
    HandlerRegistration rtn = wrapped.addHandler(type, handler);
    registrations.add(rtn);
    return rtn;
  }

  @Override
  public <H extends EventHandler> HandlerRegistration addHandlerToSource(
      GwtEvent.Type<H> type, Object source, H handler) {
    HandlerRegistration rtn = wrapped.addHandlerToSource(type, source,
        handler);
    registrations.add(rtn);
    return rtn;
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    wrapped.fireEvent(event);
  }

  @Override
  public void fireEventFromSource(GwtEvent<?> event, Object source) {
    wrapped.fireEventFromSource(event, source);
  }

  /**
   * Remove all handlers that have been added through this wrapper.
   */
  public void removeHandlers() {
    for (HandlerRegistration r : registrations) {
      r.removeHandler();
    }
    registrations.clear();
  }
}
