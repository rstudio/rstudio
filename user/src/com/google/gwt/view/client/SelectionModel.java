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
import com.google.gwt.view.client.SelectionChangeEvent.HasSelectionChangedHandlers;

/**
 * A model for selection within a list.
 * 
 * @param <T> the data type of records in the list
 */
public interface SelectionModel<T> extends HasSelectionChangedHandlers, ProvidesKey<T> {

  /**
   * A default implementation of {@link SelectionModel} that provides listener
   * addition and removal.
   * 
   * @param <T> the data type of records in the list
   */
  public abstract class AbstractSelectionModel<T> implements SelectionModel<T> {

    private final HandlerManager handlerManager = new HandlerManager(this);

    /**
     * Set to true if the next scheduled event should be canceled.
     */
    private boolean isEventCancelled;

    /**
     * Set to true if an event is scheduled to be fired.
     */
    private boolean isEventScheduled;

    private final ProvidesKey<T> keyProvider;
    
    /**
     * Construct an AbstractSelectionModel with a given key provider.
     * 
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
      return (keyProvider == null || item == null) ? item
          : keyProvider.getKey(item);
    }

    /**
     * Returns a {@link ProvidesKey} instance that simply returns the input data
     * item.
     *
     * @return the key provider, which may be null
     */
    public ProvidesKey<T> getKeyProvider() {
      return keyProvider;
    }

    /**
     * Fire a {@link SelectionChangeEvent}.  Multiple firings may be coalesced.
     */
    protected void fireSelectionChangeEvent() {
      if (isEventScheduled()) {
        setEventCancelled(true);
      }
      SelectionChangeEvent.fire(AbstractSelectionModel.this);
    }

    /**
     * Return true if the next scheduled event should be canceled.
     *
     * @return true if the event is canceled
     */
    protected boolean isEventCancelled() {
      return isEventCancelled;
    }

    /**
     * Return true if an event is scheduled to be fired.
     *
     * @return true if the event is scheduled
     */
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

    /**
     * Set whether the next scheduled event should be canceled.
     * 
     * @param isEventCancelled if true, cancel the event
     */
    protected void setEventCancelled(boolean isEventCancelled) {
      this.isEventCancelled = isEventCancelled;
    }

    /**
     * Set whether an event is scheduled to be fired.
     * 
     * @param isEventScheduled if true, schedule the event
     */
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
