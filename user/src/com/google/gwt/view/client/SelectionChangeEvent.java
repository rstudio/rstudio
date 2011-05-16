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
package com.google.gwt.view.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Represents a selection change event.
 */
public class SelectionChangeEvent extends
    GwtEvent<SelectionChangeEvent.Handler> {

  /**
   * Handler interface for {@link SelectionChangeEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link SelectionChangeEvent} is fired.
     *
     * @param event the {@link SelectionChangeEvent} that was fired
     */
    void onSelectionChange(SelectionChangeEvent event);
  }

  /**
   * Interface specifying that a class can add
   * {@code SelectionChangeEvent.Handler}s.
   */
  public interface HasSelectionChangedHandlers extends HasHandlers {
    /**
     * Adds a {@link SelectionChangeEvent} handler.
     * 
     * @param handler the handler
     * @return {@link HandlerRegistration} used to remove this handler
     */
    HandlerRegistration addSelectionChangeHandler(Handler handler);
  }

  /**
   * Handler type.
   */
  private static Type<SelectionChangeEvent.Handler> TYPE;

  /**
   * Fires a selection change event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   */
  public static void fire(HasSelectionChangedHandlers source) {
    if (TYPE != null) {
      SelectionChangeEvent event = new SelectionChangeEvent();
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<SelectionChangeEvent.Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<SelectionChangeEvent.Handler>();
    }
    return TYPE;
  }

  /**
   * Creates a selection change event.
   */
  SelectionChangeEvent() {
  }

  @Override
  public final Type<SelectionChangeEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(SelectionChangeEvent.Handler handler) {
    handler.onSelectionChange(this);
  }
}
