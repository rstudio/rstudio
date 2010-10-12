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

import com.google.gwt.event.shared.GwtEvent;

/**
 * Represents a before selection event.
 * 
 * @param <T> the type about to be selected
 */
public class BeforeSelectionEvent<T> extends
    GwtEvent<BeforeSelectionHandler<T>> {

  /**
   * Handler type.
   */
  private static Type<BeforeSelectionHandler<?>> TYPE;

  /**
   * Fires a before selection event on all registered handlers in the handler
   * manager. If no such handlers exist, this method will do nothing.
   * 
   * @param <T> the item type
   * @param source the source of the handlers
   * @param item the item
   * @return the event so that the caller can check if it was canceled, or null
   *         if no handlers of this event type have been registered
   */
  public static <T> BeforeSelectionEvent<T> fire(
      HasBeforeSelectionHandlers<T> source, T item) {
    // If no handlers exist, then type can be null.
    if (TYPE != null) {
      BeforeSelectionEvent<T> event = new BeforeSelectionEvent<T>();
      event.setItem(item);
      source.fireEvent(event);
      return event;
    }
    return null;
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<BeforeSelectionHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<BeforeSelectionHandler<?>>();
    }
    return TYPE;
  }

  private T item;

  private boolean canceled;

  /**
   * Creates a new before selection event.
   */
  protected BeforeSelectionEvent() {
  }

  /**
   * Cancel the before selection event.
   * 
   * Classes overriding this method should still call super.cancel().
   */
  public void cancel() {
    canceled = true;
  }

  // The instance knows its BeforeSelectionHandler is of type I, but the TYPE
  // field itself does not, so we have to do an unsafe cast here.
  @SuppressWarnings("unchecked")
  @Override
  public final Type<BeforeSelectionHandler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Gets the item.
   * 
   * @return the item
   */
  public T getItem() {
    return item;
  }

  /**
   * Has the selection event already been canceled?
   * 
   * @return is canceled
   */
  public boolean isCanceled() {
    return canceled;
  }

  @Override
  protected void dispatch(BeforeSelectionHandler<T> handler) {
    handler.onBeforeSelection(this);
  }

  /**
   * Sets the item.
   * 
   * @param item the item
   */
  protected final void setItem(T item) {
    this.item = item;
  }
}
