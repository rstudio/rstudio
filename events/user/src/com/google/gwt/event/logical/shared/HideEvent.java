/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.event.logical.shared;

import com.google.gwt.event.shared.AbstractEvent;

/**
 * Fired after an event source has hidden its contents.
 */
public class HideEvent extends AbstractEvent {

  /**
   * Event Key for {@link HideEvent}.
   */
  public static final Type<HideEvent, HideHandler> TYPE = new Type<HideEvent, HideHandler>() {
    @Override
    protected void fire(HideHandler handler, HideEvent event) {
      handler.onHide(event);
    }
  };

  /**
   * Constructs a HideEvent event.
   */
  public HideEvent() {
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
