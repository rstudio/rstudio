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
package com.google.gwt.bikeshed.list.shared;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * A model for selection within a list.
 *
 * @param <T> the data type of records in the list
 */
public interface SelectionModel<T> extends HasHandlers {

  /**
   * Handler interface for {@link SelectionChangeEvent} events.
   */
  public interface SelectionChangeHandler extends EventHandler {

    /**
     * Called when {@link SelectionChangeEvent} is fired.
     *
     * @param event the {@link SelectionChangeEvent} that was fired
     */
    void onSelectionChange(SelectionChangeEvent event);
  }

  /**
   * Represents a selection change event.
   */
  public static class SelectionChangeEvent extends
      GwtEvent<SelectionChangeHandler> {

    /**
     * Handler type.
     */
    private static Type<SelectionChangeHandler> TYPE;

    /**
     * Fires a selection change event on all registered handlers in the handler
     * manager. If no such handlers exist, this method will do nothing.
     *
     * @param source the source of the handlers
     */
    static void fire(SelectionModel<?> source) {
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
    public static Type<SelectionChangeHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<SelectionChangeHandler>();
      }
      return TYPE;
    }

    /**
     * Creates a selection change event.
     */
    SelectionChangeEvent() {
    }

    @Override
    public final Type<SelectionChangeHandler> getAssociatedType() {
      return TYPE;
    }

    @Override
    protected void dispatch(SelectionChangeHandler handler) {
      handler.onSelectionChange(this);
    }
  }

  /**
   * A default implementation of SelectionModel that provides listener addition
   * and removal.
   *
   * @param <T> the data type of records in the list
   */
  public abstract class AbstractSelectionModel<T> implements SelectionModel<T> {

    private final HandlerManager handlerManager = new HandlerManager(this);

    /**
     * Set to true if an event is scheduled to be fired.
     */
    private boolean isEventScheduled;

    private ProvidesKey<T> keyProvider;

    public HandlerRegistration addSelectionChangeHandler(
        SelectionChangeHandler handler) {
      return handlerManager.addHandler(SelectionChangeEvent.getType(), handler);
    }

    public void fireEvent(GwtEvent<?> event) {
      handlerManager.fireEvent(event);
    }

    /**
     * Returns a ProvidesKey instance that simply returns the input data item.
     */
    public ProvidesKey<T> getProvidesKey() {
      if (keyProvider == null) {
        keyProvider  = new ProvidesKey<T>() {
          public Object getKey(T item) {
            return item;
          }
        };
      }
      return keyProvider;
    }

    /**
     * Schedules a {@link SelectionModel.SelectionChangeEvent} to fire at the
     * end of the current event loop.
     */
    protected void scheduleSelectionChangeEvent() {
      if (!isEventScheduled) {
        isEventScheduled = true;
        Scheduler.get().scheduleFinally(new ScheduledCommand() {
          public void execute() {
            isEventScheduled = false;
            SelectionChangeEvent.fire(AbstractSelectionModel.this);
          }
        });
      }
    }
  }

  /**
   * Adds a {@link SelectionChangeEvent} handler.
   *
   * @param handler the handler
   * @return the registration for the event
   */
  HandlerRegistration addSelectionChangeHandler(SelectionChangeHandler handler);

  /**
   * Returns a ProvidesKey instance that may be used to provide a unique
   * key for each record.
   */
  ProvidesKey<T> getProvidesKey();

  /**
   * Check if an object is selected.
   *
   * @param object the object
   * @return true if selected, false if not
   */
  boolean isSelected(T object);

  /**
   * Set the selected state of an object.
   *
   * @param object the object to select or deselect
   * @param selected true to select, false to deselect
   */
  void setSelected(T object, boolean selected);
}
