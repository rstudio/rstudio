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
 * Wraps {com.google.web.bindery.event.shared.SimpleEventBus} for legacy
 * compatibility.
 * 
 * @deprecated Use com.google.web.bindery.event.shared.SimpleEventBus
 */
@SuppressWarnings("deprecation")
@Deprecated
public class SimpleEventBus extends EventBus {
  private final com.google.web.bindery.event.shared.SimpleEventBus real =
      new com.google.web.bindery.event.shared.SimpleEventBus();

  @Override
  public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
    return real.addHandler(type, handler);
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
  public void fireEventFromSource(Event<?> event, Object source) {
    real.fireEventFromSource(event, source);
  }
}
