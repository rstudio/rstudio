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

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Allows the previewing of events before they are fired to Cells.
 * 
 * @param <T> the data type of the {@link HasData} source
 */
public class CellPreviewEvent<T> extends GwtEvent<CellPreviewEvent.Handler<T>> {

  /**
   * Handler for {@link CellPreviewEvent}.
   * 
   * @param <T> the data type of the {@link HasData}
   */
  public static interface Handler<T> extends EventHandler {

    /**
     * Called when {@link CellPreviewEvent} is fired.
     * 
     * @param event the {@link CellPreviewEvent} that was fired
     */
    void onCellPreview(CellPreviewEvent<T> event);
  }

  /**
   * Handler type.
   */
  private static Type<Handler<?>> TYPE;

  /**
   * Fires a cell preview event on all registered handlers in the handler
   * manager. If no such handlers exist, this implementation will do nothing.
   * This implementation sets the column to 0.
   * 
   * @param <T> the old value type
   * @param source the source of the handlers
   * @param nativeEvent the event to preview
   * @param display the {@link HasData} source of the event
   * @param context the Cell {@link Context}
   * @param value the value where the event occurred
   * @param isCellEditing indicates whether or not the cell is being edited
   * @param isSelectionHandled indicates whether or not selection is handled
   * @return the {@link CellPreviewEvent} that was fired
   */
  public static <T> CellPreviewEvent<T> fire(HasCellPreviewHandlers<T> source,
      NativeEvent nativeEvent, HasData<T> display, Context context, T value,
      boolean isCellEditing, boolean isSelectionHandled) {
    CellPreviewEvent<T> event = new CellPreviewEvent<T>(nativeEvent, display,
        context, value, isCellEditing, isSelectionHandled);
    if (TYPE != null) {
      source.fireEvent(event);
    }
    return event;
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<Handler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<Handler<?>>();
    }
    return TYPE;
  }

  private final Context context;
  private final HasData<T> display;
  private boolean isCanceled = false;
  private final boolean isCellEditing;
  private final boolean isSelectionHandled;
  private final NativeEvent nativeEvent;
  private final T value;

  /**
   * Construct a new {@link CellPreviewEvent}.
   * 
   * @param nativeEvent the event to preview
   * @param display the {@link HasData} source of the event
   * @param context the Cell {@link Context}
   * @param value the value where the event occurred
   * @param isCellEditing indicates whether or not the cell is being edited
   * @param isSelectionHandled indicates whether or not selection is handled
   */
  protected CellPreviewEvent(NativeEvent nativeEvent, HasData<T> display,
      Context context, T value, boolean isCellEditing,
      boolean isSelectionHandled) {
    this.nativeEvent = nativeEvent;
    this.display = display;
    this.context = context;
    this.value = value;
    this.isCellEditing = isCellEditing;
    this.isSelectionHandled = isSelectionHandled;
  }

  // The instance knows its Handler is of type T, but the TYPE
  // field itself does not, so we have to do an unsafe cast here.
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Type<Handler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Get the column index of the Cell where the event occurred if the source is
   * a table. If the source is not a table, the column is always 0.
   * 
   * @return the column index, or 0 if there is only one column
   */
  public int getColumn() {
    return context.getColumn();
  }

  /**
   * Get the cell {@link Context}.
   * 
   * @return the cell {@link Context}
   */
  public Context getContext() {
    return context;
  }

  /**
   * Get the {@link HasData} source of the event.
   */
  public HasData<T> getDisplay() {
    return display;
  }

  /**
   * Get the index of the value where the event occurred.
   */
  public int getIndex() {
    return context.getIndex();
  }

  /**
   * Get the {@link NativeEvent} to preview.
   */
  public NativeEvent getNativeEvent() {
    return nativeEvent;
  }

  /**
   * Get the value where the event occurred.
   */
  public T getValue() {
    return value;
  }

  /**
   * Check if the event has been canceled.
   * 
   * @return true if the event has been canceled
   * @see #setCanceled(boolean)
   */
  public boolean isCanceled() {
    return isCanceled;
  }

  /**
   * Check whether or not the cell where the event occurred is being edited.
   * 
   * @return true if the cell is being edited, false if not
   */
  public boolean isCellEditing() {
    return isCellEditing;
  }

  /**
   * Check whether or not selection is being handled by the widget or one of its
   * Cells.
   * 
   * @return true if selection is handled by the widget
   */
  public boolean isSelectionHandled() {
    return isSelectionHandled;
  }

  /**
   * Cancel the event and prevent it from firing to the Cell.
   * 
   * @param cancel true to cancel the event, false to allow it
   */
  public void setCanceled(boolean cancel) {
    this.isCanceled = cancel;
  }

  @Override
  protected void dispatch(Handler<T> handler) {
    handler.onCellPreview(this);
  }
}
