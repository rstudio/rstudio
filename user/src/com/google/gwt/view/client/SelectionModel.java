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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * A model for selection within a list.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 * 
 * @param <T> the data type of records in the list
 */
public interface SelectionModel<T> extends HasHandlers, ProvidesKey<T> {

  /**
   * A default implementation of SelectionModel that provides listener addition
   * and removal.
   * 
   * @param <T> the data type of records in the list
   */
  public abstract class AbstractSelectionModel<T> implements SelectionModel<T> {

    private final HandlerManager handlerManager = new HandlerManager(this);

    /**
     * Set to true if the next scheduled event should be cancelled.
     */
    private boolean isEventCancelled;

    /**
     * Set to true if an event is scheduled to be fired.
     */
    private boolean isEventScheduled;

    private final ProvidesKey<T> keyProvider;
    
    /**
     * @param keyProvider an instance of ProvidesKey<T>, or null if the record
     *        object should act as its own key
     */
    protected AbstractSelectionModel(ProvidesKey<T> keyProvider) {
      this.keyProvider = keyProvider;
    }

    public HandlerRegistration addSelectionChangeHandler(
        SelectionChangeEvent.Handler handler) {
      return handlerManager.addHandler(SelectionChangeEvent.getType(), handler);
    }

    public void fireEvent(GwtEvent<?> event) {
      handlerManager.fireEvent(event);
    }

    public Object getKey(T item) {
      return keyProvider == null ? item : keyProvider.getKey(item);
    }

    /**
     * Returns a ProvidesKey instance that simply returns the input data item.
     */
    public ProvidesKey<T> getKeyProvider() {
      return keyProvider;
    }

    protected void fireSelectionChangeEvent() {
      if (isEventScheduled()) {
        setEventCancelled(true);
      }
      SelectionChangeEvent.fire(AbstractSelectionModel.this);
    }

    protected boolean isEventCancelled() {
      return isEventCancelled;
    }

    protected boolean isEventScheduled() {
      return isEventScheduled;
    }

    /**
     * Schedules a {@link SelectionChangeEvent} to fire at the
     * end of the current event loop.
     */
    protected void scheduleSelectionChangeEvent() {
      setEventCancelled(false);
      if (!isEventScheduled()) {
        setEventScheduled(true);
        Scheduler.get().scheduleFinally(new ScheduledCommand() {
          public void execute() {
            setEventScheduled(false);
            if (isEventCancelled()) {
              setEventCancelled(false);
              return;
            }
            fireSelectionChangeEvent();
          }
        });
      }
    }

    protected void setEventCancelled(boolean isEventCancelled) {
      this.isEventCancelled = isEventCancelled;
    }

    protected void setEventScheduled(boolean isEventScheduled) {
      this.isEventScheduled = isEventScheduled;
    }
  }

  /**
   * Adds a {@link SelectionChangeEvent} handler.
   * 
   * @param handler the handler
   * @return the registration for the event
   */
  HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler);

  /**
   * Check if an object is selected.
   * 
   * @param object the object
   * @return true if selected, false if not
   */
  boolean isSelected(T object);

  /**
   * Set the selected state of an object and fire a
   * {@link SelectionChangeEvent} if the selection has
   * changed.  Subclasses should not fire an event in the case where
   * selected is true and the object was already selected, or selected
   * is false and the object was not previously selected.
   * 
   * @param object the object to select or deselect
   * @param selected true to select, false to deselect
   */
  void setSelected(T object, boolean selected);
}
