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
 * Event thrown when the user has reached a new location in the app.
 */
public class PlaceChanged extends GwtEvent<PlaceChanged.Handler> {

  /**
   * Implemented by handlers of PlaceChanged events.
   */
  public interface Handler extends EventHandler {
    void onPlaceChanged(PlaceChanged event);
  }

  public static Type<Handler> TYPE = new Type<Handler>();

  private final Place newPlace;

  public PlaceChanged(Place newPlace) {
    this.newPlace = newPlace;
  }

  @Override
  public GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Place getNewPlace() {
    return newPlace;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onPlaceChanged(this);
  }
}
