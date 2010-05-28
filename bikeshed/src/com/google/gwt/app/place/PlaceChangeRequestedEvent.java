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
 * Event thrown when the user may go to a new place in the app. May be rejected.
 * 
 * @param <P> the type of the requested place
 */
public class PlaceChangeRequestedEvent<P extends Place> extends
    GwtEvent<PlaceChangeRequestedEvent.Handler<P>> {

  /**
   * Implemented by handlers of PlaceChangeRequestedEvent.
   * @param <P> the type of the requested Place
   */
  public interface Handler<P extends Place> extends EventHandler {
    void onPlaceChangeRequested(PlaceChangeRequestedEvent<P> event);
  }

  public static final Type<Handler<?>> TYPE = new Type<Handler<?>>();

  private boolean rejected = false;

  private final P newPlace;

  public PlaceChangeRequestedEvent(P newPlace) {
    this.newPlace = newPlace;
  }

  // param type of static TYPE cannot be set
  @SuppressWarnings("unchecked")
  @Override
  public Type<Handler<P>> getAssociatedType() {
    return (Type) TYPE;
  }

  public P getNewPlace() {
    return newPlace;
  }

  public boolean isRejected() {
    return rejected;
  }

  public void reject() {
    this.rejected = true;
  }

  @Override
  protected void dispatch(Handler<P> handler) {
    handler.onPlaceChangeRequested(this);
  }
}
