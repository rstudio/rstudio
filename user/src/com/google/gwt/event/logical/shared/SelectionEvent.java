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
 * Represents a selection event.
 * 
 * @param <T> the type being selected
 */
public class SelectionEvent<T> extends GwtEvent<SelectionHandler<T>> {

  /**
   * Handler type.
   */
  private static Type<SelectionHandler<?>> TYPE;

  /**
   * Fires a selection event on all registered handlers in the handler
   * manager.If no such handlers exist, this method will do nothing.
   * 
   * @param <T> the selected item type
   * @param source the source of the handlers
   * @param selectedItem the selected item
   */
  public static <T> void fire(HasSelectionHandlers<T> source, T selectedItem) {
    if (TYPE != null) {
      SelectionEvent<T> event = new SelectionEvent<T>(selectedItem);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<SelectionHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<SelectionHandler<?>>();
    }
    return TYPE;
  }

  private final T selectedItem;

  /**
   * Creates a new selection event.
   * 
   * @param selectedItem selected item
   */
  protected SelectionEvent(T selectedItem) {
    this.selectedItem = selectedItem;
  }

  // The instance knows its BeforeSelectionHandler is of type I, but the TYPE
  // field itself does not, so we have to do an unsafe cast here.
  @SuppressWarnings("unchecked")
  @Override
  public final Type<SelectionHandler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Gets the selected item.
   * 
   * @return the selected item
   */
  public T getSelectedItem() {
    return selectedItem;
  }

  @Override
  protected void dispatch(SelectionHandler<T> handler) {
    handler.onSelection(this);
  }
}
