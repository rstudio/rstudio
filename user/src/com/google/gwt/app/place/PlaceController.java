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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * In charge of the user's location in the app.
 * 
 * @param <P> the type of places managed
 */
public class PlaceController<P extends Place> {

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
   * Optional delegate in charge of Window related events. Provides nice
   * isolation for unit testing, and allows customization of confirmation
   * handling.
   */
  public interface Delegate {
    HandlerRegistration addWindowClosingHandler(ClosingHandler handler);

    boolean confirm(String message);
  }

  private final HandlerManager eventBus;
  private final Delegate delegate;

  private P where;

  /**
   * Create a new PlaceController with a {@link DefaultDelegate}.
   * The DefaultDelegate is created via a call to GWT.create(), so 
   * an alternative default implementation can be provided through
   * &lt;replace-with> rules in a gwt.xml file.
   */
  public PlaceController(HandlerManager eventBus) {
    this(eventBus, (Delegate) GWT.create(DefaultDelegate.class));
  }

  /**
   * Create a new PlaceController.
   */
  public PlaceController(HandlerManager eventBus, Delegate delegate) {
    this.eventBus = eventBus;
    this.delegate = delegate;
    delegate.addWindowClosingHandler(new ClosingHandler() {
      public void onWindowClosing(ClosingEvent event) {
        String warning = maybeGoTo(null);
        if (warning != null) {
          event.setMessage(warning);
        }
      }
    });
  }

  /**
   * @return the current place
   */
  public P getWhere() {
    return where;
  }

  /**
   * Request a change to a new place. It is not a given that we'll actually get
   * there. First a {@link PlaceChangeRequestedEvent} will be posted to the
   * event bus. If any receivers post a warning message to that event, it will
   * be presented to the user via {@link Delegate#confirm(String)} (which is
   * typically a call to {@link Window#confirm(String)}). If she cancels, the
   * current location will not change. Otherwise, the location changes and a
   * {@link PlaceChangeEvent} is posted announcing the new place.
   */
  public void goTo(P newPlace) {
    String warning = maybeGoTo(newPlace);
    if (warning == null || delegate.confirm(warning)) {
      where = newPlace;
      eventBus.fireEvent(new PlaceChangeEvent<P>(newPlace));
    }
  }

  private String maybeGoTo(P newPlace) {
    PlaceChangeRequestedEvent<P> willChange = new PlaceChangeRequestedEvent<P>(
        newPlace);
    eventBus.fireEvent(willChange);
    String warning = willChange.getWarning();
    return warning;
  }
}
