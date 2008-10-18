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
 * Fired directly before an event source's contents are shown.
 */
public class BeforeShowEvent extends AbstractEvent {

  /**
   * Event type for {@link BeforeShowEvent}.
   */
  public static final Type<BeforeShowEvent, BeforeShowHandler> TYPE = new Type<BeforeShowEvent, BeforeShowHandler>() {
    @Override
    protected void fire(BeforeShowHandler handler, BeforeShowEvent event) {
      handler.onBeforeShow(event);
    }
  };

  /**
   * Constructor.
   */
  public BeforeShowEvent() {
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
