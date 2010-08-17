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
package com.google.gwt.app.place;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.GwtEvent.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * Wraps an EventBus to hold on to any HandlerRegistrations, so that they can
 * easily all be cleared at once.
 */
public class StopperedEventBus implements EventBus {
  private final EventBus wrappedBus;
  private final Set<HandlerRegistration> registrations = new HashSet<HandlerRegistration>();
  
  public StopperedEventBus(EventBus wrappedBus) {
    this.wrappedBus = wrappedBus;
  }

  public <H extends EventHandler> HandlerRegistration addHandler(Type<H> type,
      H handler) {
    HandlerRegistration rtn = wrappedBus.addHandler(type, handler);
    registrations.add(rtn);
    return rtn;
  }

  public void fireEvent(GwtEvent<?> event) {
    wrappedBus.fireEvent(event);
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
