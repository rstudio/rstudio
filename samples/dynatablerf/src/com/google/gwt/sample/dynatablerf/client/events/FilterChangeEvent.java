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
package com.google.gwt.sample.dynatablerf.client.events;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * An event to indicate a change in the filter options.
 */
public class FilterChangeEvent extends GwtEvent<FilterChangeEvent.Handler> {
  /**
   * Handles {@link FilterChangeEvent}.
   */
  public interface Handler extends EventHandler {
    void onFilterChanged(FilterChangeEvent e);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    return eventBus.addHandler(TYPE, handler);
  }

  private final int day;
  private final boolean selected;

  public FilterChangeEvent(int day, boolean selected) {
    this.day = day;
    this.selected = selected;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getDay() {
    return day;
  }

  public boolean isSelected() {
    return selected;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onFilterChanged(this);
  }
}
