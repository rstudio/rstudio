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
package com.google.gwt.view.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Represents a row count change event.
 */
public class RowCountChangeEvent extends GwtEvent<RowCountChangeEvent.Handler> {

  /**
   * Handler type.
   */
  private static Type<Handler> TYPE;

  /**
   * Handler interface for {@link RowCountChangeEvent} events.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when a {@link RowCountChangeEvent} is fired.
     *
     * @param event the {@link RowCountChangeEvent} that was fired
     */
    void onRowCountChange(RowCountChangeEvent event);
  }

  /**
   * Fires a {@link RowCountChangeEvent} on all registered handlers in the
   * handler manager. If no such handlers exist, this method will do nothing.
   *
   * @param source the source of the handlers
   * @param rowCount the new rowCount
   * @param isExact true if rowCount is an exact count
   */
  public static void fire(HasRows source, int rowCount, boolean isExact) {
    if (TYPE != null) {
      RowCountChangeEvent event = new RowCountChangeEvent(rowCount, isExact);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   *
   * @return returns the handler type
   */
  public static Type<Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<Handler>();
    }
    return TYPE;
  }

  private final int rowCount;
  private final boolean isExact;

  /**
   * Creates a {@link RowCountChangeEvent}.
   *
   * @param rowCount the new row count
   * @param isExact true if the row count is exact
   */
  protected RowCountChangeEvent(int rowCount, boolean isExact) {
    this.rowCount = rowCount;
    this.isExact = isExact;
  }

  @Override
  public final Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Gets the new row count.
   *
   * @return the new row count
   */
  public int getNewRowCount() {
    return rowCount;
  }

  /**
   * Check if the new row count is exact.
   *
   * @return true if the new row count is exact, false if not
   */
  public boolean isNewRowCountExact() {
    return isExact;
  }

  @Override
  public String toDebugString() {
    return super.toDebugString() + getNewRowCount();
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowCountChange(this);
  }
}
