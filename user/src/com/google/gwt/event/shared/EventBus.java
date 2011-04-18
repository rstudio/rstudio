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

/**
 * Extends {com.google.web.bindery.event.shared.EventBus} for legacy
 * compatibility.
 */
public abstract class EventBus extends com.google.web.bindery.event.shared.EventBus implements HasHandlers {
  
  public <H extends EventHandler> HandlerRegistration addHandler(GwtEvent.Type<H> type, H handler) {
    return wrap(addHandler((Event.Type<H>) type, handler));
  }

  public <H extends EventHandler> HandlerRegistration addHandlerToSource(GwtEvent.Type<H> type,
      Object source, H handler) {
    return wrap(addHandlerToSource((Event.Type<H>) type, source, handler));
  }
  
  public void fireEvent(GwtEvent<?> event) {
    try {
      fireEvent((Event<?>) event);
    } catch (com.google.web.bindery.event.shared.UmbrellaException e) {
      throw new UmbrellaException(e.getCauses());
    }
  }

  public void fireEventFromSource(GwtEvent<?> event, Object source) {
    try {
      fireEventFromSource((Event<?>) event, source);
    } catch (com.google.web.bindery.event.shared.UmbrellaException e) {
      throw new UmbrellaException(e.getCauses());
    }
  }

  protected HandlerRegistration wrap(com.google.web.bindery.event.shared.HandlerRegistration reg) {
    return new LegacyHandlerWrapper(reg);
  }
}
