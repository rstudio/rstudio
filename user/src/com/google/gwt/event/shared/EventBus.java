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
public abstract class EventBus extends com.google.web.bindery.event.shared.EventBus implements
    HasHandlers {

  @Override
  public <H> com.google.web.bindery.event.shared.HandlerRegistration addHandler(Event.Type<H> type, H handler) {
    throw new UnsupportedOperationException("Subclass responsibility. "
        + "This class is a legacy wrapper for com.google.web.bindery.event.shared.EventBus. "
        + "Use that directly, or try com.google.gwt.event.shared.SimpleEventBus");
  }
  
  public abstract <H extends EventHandler> HandlerRegistration addHandler(GwtEvent.Type<H> type, H handler);

  @Override
  public <H> com.google.web.bindery.event.shared.HandlerRegistration addHandlerToSource(Event.Type<H> type,
      Object source, H handler) {
    throw new UnsupportedOperationException("Subclass responsibility. "
        + "This class is a legacy wrapper for com.google.web.bindery.event.shared.EventBus. "
        + "Use that directly, or try com.google.gwt.event.shared.SimpleEventBus");
  }

  public abstract <H extends EventHandler> HandlerRegistration addHandlerToSource(GwtEvent.Type<H> type,
      Object source, H handler);

  @Override
  public void fireEvent(Event<?> event) {
    throw new UnsupportedOperationException("Subclass responsibility. "
        + "This class is a legacy wrapper for com.google.web.bindery.event.shared.EventBus. "
        + "Use that directly, or try com.google.gwt.event.shared.SimpleEventBus");
  }

  public abstract void fireEvent(GwtEvent<?> event);

  @Override
  public void fireEventFromSource(Event<?> event, Object source) {
    throw new UnsupportedOperationException("Subclass responsibility. "
        + "This class is a legacy wrapper for com.google.web.bindery.event.shared.EventBus. "
        + "Use that directly, or try com.google.gwt.event.shared.SimpleEventBus");
  }

  public abstract void fireEventFromSource(GwtEvent<?> event, Object source);

  protected void castFireEvent(GwtEvent<?> event) {
    try {
      fireEvent((Event<?>) event);
    } catch (com.google.web.bindery.event.shared.UmbrellaException e) {
      throw new UmbrellaException(e.getCauses());
    }
  }

  protected void castFireEventFromSource(GwtEvent<?> event, Object source) {
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
