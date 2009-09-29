/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Fired when the event source is initialized.
 */
public class InitializeEvent extends GwtEvent<InitializeHandler> {

  /**
   * The event type.
   */
  private static Type<InitializeHandler> TYPE;

  /**
   * Fires a initialize event on all registered handlers in the handler source.
   * 
   * @param <S> The handler source
   * @param source the source of the handlers
   */
  public static <S extends HasInitializeHandlers & HasHandlers> void fire(
      S source) {
    if (TYPE != null) {
      InitializeEvent event = new InitializeEvent();
      source.fireEvent(event);
    }
  }

  /**
   * Ensures the existence of the handler hook and then returns it.
   * 
   * @return returns a handler hook
   */
  public static Type<InitializeHandler> getType() {
    if (TYPE == null) {
      TYPE = new Type<InitializeHandler>();
    }
    return TYPE;
  }

  /**
   * Construct a new {@link InitializeEvent}.
   * 
   */
  protected InitializeEvent() {
  }

  @Override
  public final Type<InitializeHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(InitializeHandler handler) {
    handler.onInitialize(this);
  }
}
