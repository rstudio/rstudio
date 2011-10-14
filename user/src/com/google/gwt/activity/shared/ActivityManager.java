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
package com.google.gwt.activity.shared;

import com.google.gwt.event.shared.ResettableEventBus;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceChangeRequestEvent;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages {@link Activity} objects that should be kicked off in response to
 * {@link PlaceChangeEvent} events. Each activity can start itself
 * asynchronously, and provides a widget to be shown when it's ready to run.
 */
public class ActivityManager implements PlaceChangeEvent.Handler, PlaceChangeRequestEvent.Handler {

  /**
   * Wraps our real display to prevent an Activity from taking it over if it is
   * not the currentActivity.
   */
  private class ProtectedDisplay implements AcceptsOneWidget {
    private final Activity activity;

    ProtectedDisplay(Activity activity) {
      this.activity = activity;
    }

    public void setWidget(IsWidget view) {
      if (this.activity == ActivityManager.this.currentActivity) {
        startingNext = false;
        showWidget(view);
      }
    }
  }

  private static final Activity NULL_ACTIVITY = new AbstractActivity() {
    public void start(AcceptsOneWidget panel, com.google.gwt.event.shared.EventBus eventBus) {
    }
  };

  private final ActivityMapper mapper;

  private final EventBus eventBus;

  /*
   * Note that we use the legacy class from com.google.gwt.event.shared, because
   * we can't change the Activity interface.
   */
  private final ResettableEventBus stopperedEventBus;

  private Activity currentActivity = NULL_ACTIVITY;

  private AcceptsOneWidget display;

  private boolean startingNext = false;

  private HandlerRegistration handlerRegistration;

  /**
   * Create an ActivityManager. Next call {@link #setDisplay}.
   * 
   * @param mapper finds the {@link Activity} for a given
   *          {@link com.google.gwt.place.shared.Place}
   * @param eventBus source of {@link PlaceChangeEvent} and
   *          {@link PlaceChangeRequestEvent} events.
   */
  public ActivityManager(ActivityMapper mapper, EventBus eventBus) {
    this.mapper = mapper;
    this.eventBus = eventBus;
    this.stopperedEventBus = new ResettableEventBus(eventBus);
  }

  /**
  * Returns an event bus which is in use by the currently running activity.
  * <p>
  * Any handlers attached to the returned event bus will be de-registered when
  * the current activity is stopped.
  *
  * @return the event bus used by the current activity
  */
  public EventBus getActiveEventBus() {
    return stopperedEventBus;
  }
 
  /**
   * Deactivate the current activity, find the next one from our ActivityMapper,
   * and start it.
   * <p>
   * The current activity's widget will be hidden immediately, which can cause
   * flicker if the next activity provides its widget asynchronously. That can
   * be minimized by decent caching. Perenially slow activities might mitigate
   * this by providing a widget immediately, with some kind of "loading"
   * treatment.
   */
  public void onPlaceChange(PlaceChangeEvent event) {
    Activity nextActivity = getNextActivity(event);

    Throwable caughtOnStop = null;
    Throwable caughtOnCancel = null;
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
      caughtOnCancel = tryStopOrCancel(false);
      currentActivity = NULL_ACTIVITY;
      startingNext = false;
    } else if (!currentActivity.equals(NULL_ACTIVITY)) {
      showWidget(null);

      /*
       * Kill off the activity's handlers, so it doesn't have to worry about
       * them accidentally firing as a side effect of its tear down
       */
      stopperedEventBus.removeHandlers();
      caughtOnStop = tryStopOrCancel(true);
    }

    currentActivity = nextActivity;

    if (currentActivity.equals(NULL_ACTIVITY)) {
      showWidget(null);
    } else {
      startingNext = true;
      caughtOnStart = tryStart();
    }

    if (caughtOnStart != null || caughtOnCancel != null || caughtOnStop != null) {
      Set<Throwable> causes = new LinkedHashSet<Throwable>();
      if (caughtOnStop != null) {
        causes.add(caughtOnStop);
      }
      if (caughtOnCancel != null) {
        causes.add(caughtOnCancel);
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
   * @see com.google.gwt.place.shared.PlaceChangeRequestEvent.Handler#onPlaceChangeRequest(PlaceChangeRequestEvent)
   */
  public void onPlaceChangeRequest(PlaceChangeRequestEvent event) {
    event.setWarning(currentActivity.mayStop());
  }

  /**
   * Sets the display for the receiver, and has the side effect of starting or
   * stopping its monitoring the event bus for place change events.
   * <p>
   * If you are disposing of an ActivityManager, it is important to call
   * setDisplay(null) to get it to deregister from the event bus, so that it can
   * be garbage collected.
   * 
   * @param display an instance of AcceptsOneWidget
   */
  public void setDisplay(AcceptsOneWidget display) {
    boolean wasActive = (null != this.display);
    boolean willBeActive = (null != display);
    this.display = display;
    if (wasActive != willBeActive) {
      updateHandlers(willBeActive);
    }
  }

  private Activity getNextActivity(PlaceChangeEvent event) {
    if (display == null) {
      /*
       * Display may have been nulled during PlaceChangeEvent dispatch. Don't
       * bother the mapper, just return a null to ensure we shut down the
       * current activity
       */
      return null;
    }
    return mapper.getActivity(event.getNewPlace());
  }

  private void showWidget(IsWidget view) {
    if (display != null) {
      display.setWidget(view);
    }
  }

  private Throwable tryStart() {
    Throwable caughtOnStart = null;
    try {
      /*
       * Wrap the actual display with a per-call instance that protects the
       * display from canceled or stopped activities, and which maintains our
       * startingNext state.
       */
      currentActivity.start(new ProtectedDisplay(currentActivity), stopperedEventBus);
    } catch (Throwable t) {
      caughtOnStart = t;
    }
    return caughtOnStart;
  }

  private Throwable tryStopOrCancel(boolean stop) {
    Throwable caughtOnStop = null;
    try {
      if (stop) {
        currentActivity.onStop();
      } else {
        currentActivity.onCancel();
      }
    } catch (Throwable t) {
      caughtOnStop = t;
    } finally {
      /*
       * Kill off the handlers again in case it was naughty and added new ones
       * during onstop or oncancel
       */
      stopperedEventBus.removeHandlers();
    }
    return caughtOnStop;
  }

  private void updateHandlers(boolean activate) {
    if (activate) {
      final HandlerRegistration placeReg = eventBus.addHandler(PlaceChangeEvent.TYPE, this);
      final HandlerRegistration placeRequestReg =
          eventBus.addHandler(PlaceChangeRequestEvent.TYPE, this);

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
