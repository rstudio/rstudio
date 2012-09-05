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
package com.google.gwt.place.shared;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.web.bindery.event.shared.EventBus;

import java.util.logging.Logger;

/**
 * In charge of the user's location in the app.
 */
public class PlaceController {
  /**
   * Default implementation of {@link Delegate}, based on {@link Window}.
   */
  public static class DefaultDelegate implements Delegate {
    public HandlerRegistration addWindowClosingHandler(ClosingHandler handler) {
      return Window.addWindowClosingHandler(handler);
    }

    public boolean confirm(String message) {
      return Window.confirm(message);
    }
  }

  /**
   * Optional delegate in charge of Window-related events. Provides nice
   * isolation for unit testing, and allows customization of confirmation
   * handling.
   */
  public interface Delegate {
    /**
     * Adds a {@link ClosingHandler} to the Delegate.
     * 
     * @param handler a {@link ClosingHandler} instance
     * @return a {@link HandlerRegistration} instance
     */
    HandlerRegistration addWindowClosingHandler(ClosingHandler handler);

    /**
     * Called to confirm a window closing event.
     * 
     * @param message a warning message
     * @return true to allow the window closing
     */
    boolean confirm(String message);
  }

  private static final Logger log = Logger.getLogger(PlaceController.class.getName());

  private final EventBus eventBus;

  private final Delegate delegate;
  private Place where = Place.NOWHERE;

  /**
   * Legacy method tied to the old location for {@link EventBus}.
   * 
   * @deprecated use {@link #PlaceController(EventBus)}
   */
  @Deprecated
  public PlaceController(com.google.gwt.event.shared.EventBus eventBus) {
    this((EventBus) eventBus);
  }

  /**
   * Legacy method tied to the old location for {@link EventBus}.
   * 
   * @deprecated use {@link #PlaceController(EventBus, Delegate)}
   */
  @Deprecated
  public PlaceController(com.google.gwt.event.shared.EventBus eventBus, Delegate delegate) {
    this((EventBus) eventBus, delegate);
  }

  /**
   * Create a new PlaceController with a {@link DefaultDelegate}. The
   * DefaultDelegate is created via a call to GWT.create(), so an alternative
   * default implementation can be provided through &lt;replace-with&gt; rules
   * in a {@code .gwt.xml} file.
   * 
   * @param eventBus the {@link EventBus}
   */
  public PlaceController(EventBus eventBus) {
    this(eventBus, (Delegate) GWT.create(DefaultDelegate.class));
  }

  /**
   * Create a new PlaceController.
   * 
   * @param eventBus the {@link EventBus}
   * @param delegate the {@link Delegate} in charge of Window-related events
   */
  public PlaceController(EventBus eventBus, Delegate delegate) {
    this.eventBus = eventBus;
    this.delegate = delegate;
    delegate.addWindowClosingHandler(new ClosingHandler() {
      public void onWindowClosing(ClosingEvent event) {
        String warning = maybeGoTo(Place.NOWHERE);
        if (warning != null) {
          event.setMessage(warning);
        }
      }
    });
  }

  /**
   * Returns the current place.
   * 
   * @return a {@link Place} instance
   */
  public Place getWhere() {
    return where;
  }

  /**
   * Request a change to a new place. It is not a given that we'll actually get
   * there. First a {@link PlaceChangeRequestEvent} will be posted to the event
   * bus. If any receivers post a warning message to that event, it will be
   * presented to the user via {@link Delegate#confirm(String)} (which is
   * typically a call to {@link Window#confirm(String)}). If she cancels, the
   * current location will not change. Otherwise, the location changes and a
   * {@link PlaceChangeEvent} is posted announcing the new place.
   * 
   * @param newPlace a {@link Place} instance
   */
  public void goTo(Place newPlace) {
    log().fine("goTo: " + newPlace);

    if (getWhere().equals(newPlace)) {
      log().fine("Asked to return to the same place: " + newPlace);
      return;
    }

    String warning = maybeGoTo(newPlace);
    if (warning == null || delegate.confirm(warning)) {
      where = newPlace;
      eventBus.fireEvent(new PlaceChangeEvent(newPlace));
    }
  }

  /**
   * Visible for testing.
   */
  Logger log() {
    return log;
  }

  private String maybeGoTo(Place newPlace) {
    PlaceChangeRequestEvent willChange = new PlaceChangeRequestEvent(newPlace);
    eventBus.fireEvent(willChange);
    String warning = willChange.getWarning();
    return warning;
  }
}
