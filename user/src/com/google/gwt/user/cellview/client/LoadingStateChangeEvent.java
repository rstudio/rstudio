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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * An event used to indicate that the data loading state has changed.
 */
public class LoadingStateChangeEvent extends
    GwtEvent<LoadingStateChangeEvent.Handler> {

  /**
   * Implemented by handlers of {@link LoadingStateChangeEvent}.
   */
  public interface Handler extends EventHandler {
    /**
     * Called when a {@link LoadingStateChangeEvent} is fired.
     * 
     * @param event the {@link LoadingStateChangeEvent}
     */
    void onLoadingStateChanged(LoadingStateChangeEvent event);
  }

  /**
   * Represents the current status of the data being loaded.
   */
  public static interface LoadingState {
    /**
     * Indicates that the data has started to load.
     */
    LoadingState LOADING = new DefaultLoadingState();

    /**
     * Indicates that part of the data set has been loaded, but more data is
     * still pending.
     */
    LoadingState PARTIALLY_LOADED = new DefaultLoadingState();

    /**
     * Indicates that the data set has been completely loaded.
     */
    LoadingState LOADED = new DefaultLoadingState();
  }

  /**
   * Default implementation of {@link LoadingState}.
   */
  private static class DefaultLoadingState implements LoadingState {
  }

  /**
   * A singleton instance of Type.
   */
  public static final Type<Handler> TYPE = new Type<Handler>();

  private final LoadingState state;

  /**
   * Construct a new {@link LoadingStateChangeEvent}.
   * 
   * @param state the new state
   */
  public LoadingStateChangeEvent(LoadingState state) {
    this.state = state;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Get the new {@link LoadingState} associated with this event.
   * 
   * @return the {@link LoadingState}
   */
  public LoadingState getLoadingState() {
    return state;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onLoadingStateChanged(this);
  }
}
