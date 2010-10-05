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
package com.google.gwt.app.place;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Event thrown when the user has reached a new location in the app.
 */
public class PlaceChangeEvent extends GwtEvent<PlaceChangeEvent.Handler> {

  /**
   * Implemented by handlers of PlaceChangeEvent.
   */
  public interface Handler extends EventHandler {
    void onPlaceChange(PlaceChangeEvent event);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  private final Place newPlace;

  public PlaceChangeEvent(Place newPlace) {
    this.newPlace = newPlace;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Place getNewPlace() {
    return newPlace;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onPlaceChange(this);
  }
}
