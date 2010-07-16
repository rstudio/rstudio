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
 * Event thrown when the user may go to a new place in the app, or tries to
 * leave it. Receivers can call {@link #setWarning(String)} request that the
 * user be prompted to confirm the change.
 * 
 * @param <P> the type of the requested place
 */
public class PlaceChangeRequestedEvent<P extends Place> extends
    GwtEvent<PlaceChangeRequestedEvent.Handler<P>> {

  /**
   * Implemented by handlers of PlaceChangeRequestedEvent.
   * 
   * @param <P> the type of the requested Place
   */
  public interface Handler<P extends Place> extends EventHandler {
    void onPlaceChangeRequested(PlaceChangeRequestedEvent<P> event);
  }

  public static final Type<Handler<?>> TYPE = new Type<Handler<?>>();

  private String warning;

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

  /**
   * @return the place we may navigate to, or null on window close
   */
  public P getNewPlace() {
    return newPlace;
  }

  /**
   * @return the warning message to show the user before allowing the place
   *         change, or null if none has been set
   */
  public String getWarning() {
    return warning;
  }

  /**
   * Set a message to warn the user that it might be unwise to navigate away
   * from the current place, e.g. due to unsaved changes. If the user clicks
   * okay to that message, navigation will be canceled.
   * <p>
   * Calling with a null warning is the same as not calling the method at all --
   * the user will not be prompted.
   * <p>
   * Only the first non-null call to setWarning has any effect. That is, once
   * the warning message has been set it cannot be cleared.
   */
  public void setWarning(String warning) {
    if (this.warning == null) {
      this.warning = warning;
    }
  }

  @Override
  protected void dispatch(Handler<P> handler) {
    handler.onPlaceChangeRequested(this);
  }
}
