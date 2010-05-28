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

import com.google.gwt.app.place.Activity.Callback;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.ui.Widget;

/**
 * Manages {@link Activity} objects that should be kicked off in response to
 * {@link PlaceChangeEvent} events. Each activity can start itself
 * asynchronously, and provides a widget to be shown when it's ready to run.
 * 
 * @param <P> the type of {@link Place} objects that this ActivityManager can
 *          map to Activities
 */
public class ActivityManager<P extends Place> implements
    PlaceChangeEvent.Handler<P>, PlaceChangeRequestedEvent.Handler<P> {

  /**
   * Implemented by the view of an ActivityManager,
   */
  public interface View {
    /**
     * Displays widget, swapping out the previous.
     * 
     * @param widget the widget to display
     */
    void setWidget(Widget widget);
  }

  private final ActivityMapper<P> mapper;
  private final HandlerManager eventBus;

  private Activity currentActivity;
  private View display;
  private boolean startingNext = false;

  /**
   * Create an ActivityManager. Next call {@link #setDisplay} and
   * {@link #activate}.
   * 
   * @param mapper finds the {@link Activity} for a given {@link Place}
   * @param eventBus source of {@link PlaceChangeEvent} and
   *          {@link PlaceChangeRequestedEvent} events.
   */
  public ActivityManager(ActivityMapper<P> mapper, HandlerManager eventBus) {
    this.mapper = mapper;
    this.eventBus = eventBus;
  }

  /**
   * Deactive the current activity, find the next one from our ActivityMapper,
   * and start it.
   * 
   * @see PlaceChangeEvent.Handler#onPlaceChange(PlaceChangeEvent)
   */
  public void onPlaceChange(PlaceChangeEvent<P> event) {
    Activity nextActivity = mapper.getActivity(event.getNewPlace());

    if (currentActivity != null) {
      display.setWidget(null);
      currentActivity.onStop();
    }

    if (startingNext) {
      currentActivity.onCancel();
      currentActivity = null;
      startingNext = false;
    }

    if (nextActivity == null) {
      display.setWidget(null);
      currentActivity = null;
      return;
    }

    currentActivity = nextActivity;
    startingNext = true;
    currentActivity.start(new Callback() {
      public void onStarted(Widget widget) {
        startingNext = false;
        display.setWidget(widget);
      }
    });
  }

  /**
   * Reject the place change if the current is not willing to stop.
   * 
   * @see PlaceChangeRequestedEvent.Handler#onPlaceChangeRequested(PlaceChangeRequestedEvent)
   */
  public void onPlaceChangeRequested(PlaceChangeRequestedEvent<P> event) {
    if (!event.isRejected()) {

      /*
       * TODO Allow asynchronous willClose check? Could have the event object
       * vend callbacks. Place change doesn't happen until they all vended
       * callbacks, if any, reply with yes. Would likely need to add
       * onPlaceChangeCanceled?
       * 
       * Complicated, but I really want to keep AM and PC isolated. Alternative
       * is to mash them together and take place conversation off the event bus.
       * And it's still complicated, just very slightly less so.
       * 
       * Let's see if a real use case pops up.
       */
      if (currentActivity != null && !currentActivity.willStop()) {
        event.reject();
      }
    }
  }

  /**
   * Sets the display for the receiver, and has the side effect of starting or
   * stopping its monitoring the event bus for place change events.
   * <p>
   * If you are disposing of an ActivityManager, it is important to call
   * setDisplay(null) to get it to deregister from the event bus, so that it can
   * be garbage collected.
   * 
   * @param display
   */
  public void setDisplay(View display) {
    boolean wasActive = (null != this.display);
    boolean willBeActive = (null != display);
    this.display = display;
    if (wasActive != willBeActive) {
      updateHandlers(willBeActive);
    }
  }

  private void updateHandlers(boolean activate) {
    if (activate) {
      eventBus.addHandler(PlaceChangeEvent.TYPE, this);
      eventBus.addHandler(PlaceChangeRequestedEvent.TYPE, this);
    } else {
      eventBus.removeHandler(PlaceChangeEvent.TYPE, this);
      eventBus.removeHandler(PlaceChangeRequestedEvent.TYPE, this);
    }
  }
}
