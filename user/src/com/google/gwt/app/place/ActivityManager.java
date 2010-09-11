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

import com.google.gwt.app.place.Activity.Display;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.ResettableEventBus;
import com.google.gwt.event.shared.UmbrellaException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Manages {@link Activity} objects that should be kicked off in response to
 * {@link PlaceChangeEvent} events. Each activity can start itself
 * asynchronously, and provides a widget to be shown when it's ready to run.
 */
public class ActivityManager implements PlaceChangeEvent.Handler,
    PlaceChangeRequesteEvent.Handler {

  /**
   * Wraps our real display to prevent an Activity from taking it over if it is
   * not the currentActivity.
   */
  private class ProtectedDisplay implements Display {
    private final Activity activity;

    ProtectedDisplay(Activity activity) {
      this.activity = activity;
    }

    public void showActivityWidget(IsWidget view) {
      if (this.activity == ActivityManager.this.currentActivity) {
        startingNext = false;
        display.showActivityWidget(view);
      }
    }
  }

  private static final Activity NULL_ACTIVITY = new AbstractActivity() {
    public void start(Display panel, EventBus eventBus) {
    }
  };

  private final ActivityMapper mapper;

  private final EventBus eventBus;
  private final ResettableEventBus stopperedEventBus;

  private Activity currentActivity = NULL_ACTIVITY;

  private Activity.Display display;

  private boolean startingNext = false;

  private HandlerRegistration handlerRegistration;

  /**
   * Create an ActivityManager. Next call {@link #setDisplay}.
   * 
   * @param mapper finds the {@link Activity} for a given {@link Place}
   * @param eventBus source of {@link PlaceChangeEvent} and
   *          {@link PlaceChangeRequesteEvent} events.
   */
  public ActivityManager(ActivityMapper mapper, EventBus eventBus) {
    this.mapper = mapper;
    this.eventBus = eventBus;
    this.stopperedEventBus = new ResettableEventBus(eventBus);
  }

  /**
   * Deactive the current activity, find the next one from our ActivityMapper,
   * and start it.
   * 
   * @see PlaceChangeEvent.Handler#onPlaceChange(PlaceChangeEvent)
   */
  public void onPlaceChange(PlaceChangeEvent event) {
    Activity nextActivity = mapper.getActivity(event.getNewPlace());

    Throwable caughtOnStop = null;
    Throwable caughtOnStart = null;

    if (nextActivity == null) {
      nextActivity = NULL_ACTIVITY;
    }

    if (currentActivity.equals(nextActivity)) {
      return;
    }

    if (startingNext) {
      // The place changed again before the new current activity showed its
      // widget
      currentActivity.onCancel();
      currentActivity = NULL_ACTIVITY;
      startingNext = false;
    } else if (!currentActivity.equals(NULL_ACTIVITY)) {
      /*
       * TODO until caching is in place, relying on stopped activities to be
       * good citizens to reduce flicker. This makes me very nervous.
       */
      // display.showActivityWidget(null);

      /*
       * Kill off the activity's handlers, so it doesn't have to worry about
       * them accidentally firing as a side effect of its tear down
       */
      stopperedEventBus.removeHandlers();

      try {
        currentActivity.onStop();
      } catch (Throwable t) {
        caughtOnStop = t;
      } finally {
        /*
         * And kill them off again in case it was naughty and added new ones
         * during onstop
         */
        stopperedEventBus.removeHandlers();
      }
    }

    currentActivity = nextActivity;

    if (currentActivity.equals(NULL_ACTIVITY)) {
      display.showActivityWidget(null);
      return;
    }

    startingNext = true;

    /*
     * Now start the thing. Wrap the actual display with a per-call instance
     * that protects the display from canceled or stopped activities, and which
     * maintain our startingNext state.
     */
    try {
      currentActivity.start(new ProtectedDisplay(currentActivity),
          stopperedEventBus);
    } catch (Throwable t) {
      caughtOnStart = t;
    }

    if (caughtOnStart != null || caughtOnStop != null) {
      Set<Throwable> causes = new LinkedHashSet<Throwable>();
      if (caughtOnStop != null) {
        causes.add(caughtOnStop);
      }
      if (caughtOnStart != null) {
        causes.add(caughtOnStart);
      }

      throw new UmbrellaException(causes);
    }
  }

  /**
   * Reject the place change if the current activity is not willing to stop.
   * 
   * @see PlaceChangeRequesteEvent.Handler#onPlaceChangeRequest(PlaceChangeRequesteEvent)
   */
  public void onPlaceChangeRequest(PlaceChangeRequesteEvent event) {
    if (!currentActivity.equals(NULL_ACTIVITY)) {
      event.setWarning(currentActivity.mayStop());
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
  public void setDisplay(Activity.Display display) {
    boolean wasActive = (null != this.display);
    boolean willBeActive = (null != display);
    this.display = display;
    if (wasActive != willBeActive) {
      updateHandlers(willBeActive);
    }
  }

  private void updateHandlers(boolean activate) {
    if (activate) {
      final HandlerRegistration placeReg = eventBus.addHandler(
          PlaceChangeEvent.TYPE, this);
      final HandlerRegistration placeRequestReg = eventBus.addHandler(
          PlaceChangeRequesteEvent.TYPE, this);
      
      this.handlerRegistration = new HandlerRegistration() {
        public void removeHandler() {
          placeReg.removeHandler();
          placeRequestReg.removeHandler();
        }
      };
    } else {
      if (handlerRegistration != null) {
        handlerRegistration.removeHandler();
        handlerRegistration = null;
      }
    }
  }
}
