/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.user.cellview.client;

import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Event;

/**
 * Represents a row hover event.
 */
public class RowHoverEvent extends GwtEvent<RowHoverEvent.Handler> {

  /**
   * Handler for {@link RowHoverEvent}.
   */
  public static interface Handler extends EventHandler {

    /**
     * Called when {@link RowHoverEvent} is fired.
     * 
     * @param event the {@link ColumnSortEvent} that was fired
     */
    void onRowHover(RowHoverEvent event);
  }
  
  /**
   * Handler type.
   */
  private static Type<Handler> TYPE;

  /**
   * Fires a row hover event on all registered handlers in the handler
   * manager. If no such handlers exist, this implementation will do nothing.
   * 
   * @param source the source of the event
   * @param hoveringRow the currently hovering {@link TableRowElement}. If isUnHover is false, this
   *          should be the previouly hovering {@link TableRowElement}
   * @param isUnHover false if this is an unhover event
   * @return the {@link RowHoverEvent} that was fired
   */
  public static RowHoverEvent fire(HasHandlers source, TableRowElement hoveringRow,
      boolean isUnHover) {
    return fire(source, hoveringRow, null, isUnHover);
  }
  
  /**
   * Fires a row hover event on all registered handlers in the handler
   * manager. If no such handlers exist, this implementation will do nothing.
   * 
   * @param source the source of the event
   * @param hoveringRow the currently hovering {@link TableRowElement}. If isUnHover is false, this
   *          should be the previouly hovering {@link TableRowElement}
   * @param browserEvent the original browser event
   * @param isUnHover false if this is an unhover event
   * @return the {@link RowHoverEvent} that was fired
   */
  public static RowHoverEvent fire(HasHandlers source, TableRowElement hoveringRow,
      Event browserEvent, boolean isUnHover) {
    RowHoverEvent event = new RowHoverEvent(hoveringRow, browserEvent, isUnHover);
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
  public static Type<Handler> getType() {
    if (TYPE == null) {
      TYPE = new Type<Handler>();
    }
    return TYPE;
  }

  private Event browserEvent;
  
  private TableRowElement hoveringRow;
  
  private boolean isUnHover;
  
  /**
   * Construct a new {@link RowHoverEvent}.
   * 
   * @param hoveringRow the currently hovering {@link TableRowElement}. If isUnHover is false, this
   *          should be the previouly hovering {@link TableRowElement}
   * @param isUnHover false if this is an unhover event
   */
  protected RowHoverEvent(TableRowElement hoveringRow, boolean isUnHover) {
    this(hoveringRow, null, isUnHover);
  }
  
  /**
   * Construct a new {@link RowHoverEvent}.
   * 
   * @param hoveringRow the currently hovering {@link TableRowElement}. If isUnHover is false, this
   *                    should be the previouly hovering {@link TableRowElement}
   * @param browserEvent the original browser event
   * @param isUnHover false if this is an unhover event
   */
  protected RowHoverEvent(TableRowElement hoveringRow, Event browserEvent, boolean isUnHover) {
    this.hoveringRow = hoveringRow;
    this.browserEvent = browserEvent;
    this.isUnHover = isUnHover;
  }
  
  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }
  
  /**
   * Return the original browser {@link Event}. The browser event could be null if the event is
   * fired without one (e.g., by calling {@link #fire(HasHandler, TableRowElement, isUnHover)})
   */
  public Event getBrowserEvent() {
    return browserEvent;
  }
  
  /**
   * Return the {@link TableRowElement} that the user just hovered or unhovered.
   */
  public TableRowElement getHoveringRow() {
    return hoveringRow;
  }
  
  /**
   * Return whether this is an unhover event.
   */
  public boolean isUnHover() {
    return isUnHover;
  }
  
  @Override
  protected void dispatch(Handler handler) {
    handler.onRowHover(this);
  }
}

