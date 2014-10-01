/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A simplified, browser-safe timer class. This class serves the same purpose as
 * java.util.Timer, but is simplified because of the single-threaded
 * environment.
 * 
 * <p>
 * To schedule a timer, simply create a subclass of it (overriding {@link #run})
 * and call {@link #schedule} or {@link #scheduleRepeating}.
 * </p>
 * 
 * <p>
 * NOTE: If you are using a timer to schedule a UI animation, use
 * {@link com.google.gwt.animation.client.AnimationScheduler} instead. The
 * browser can optimize your animation for maximum performance.
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TimerExample}
 * </p>
 */
public abstract class Timer {

  private boolean isRepeating;

  private Integer timerId = null;

  /**
   * Workaround for broken clearTimeout in IE. Keeps track of whether cancel has been called since
   * schedule was called. See https://code.google.com/p/google-web-toolkit/issues/detail?id=8101
   */
  private int cancelCounter = 0;

  /**
   * Returns {@code true} if the timer is running. Timer is running if and only if it is scheduled
   * but it is not expired or cancelled.
   */
  public final boolean isRunning() {
    return timerId != null;
  }

  /**
   * Cancels this timer. If the timer is not running, this is a no-op.
   */
  public void cancel() {
    if (!isRunning()) {
      return;
    }

    cancelCounter++;
    if (isRepeating) {
      clearInterval(timerId);
    } else {
      clearTimeout(timerId);
    }
    timerId = null;
  }

  /**
   * This method will be called when a timer fires. Override it to implement the
   * timer's logic.
   */
  public abstract void run();

  /**
   * Schedules a timer to elapse in the future. If the timer is already running then it will be
   * first canceled before re-scheduling.
   *
   * @param delayMillis how long to wait before the timer elapses, in milliseconds
   */
  public void schedule(int delayMillis) {
    if (delayMillis < 0) {
      throw new IllegalArgumentException("must be non-negative");
    }
    if (isRunning()) {
      cancel();
    }
    isRepeating = false;
    timerId = setTimeout(createCallback(this, cancelCounter), delayMillis);
  }

  /**
   * Schedules a timer that elapses repeatedly. If the timer is already running then it will be
   * first canceled before re-scheduling.
   *
   * @param periodMillis how long to wait before the timer elapses, in milliseconds, between each
   *        repetition
   */
  public void scheduleRepeating(int periodMillis) {
    if (periodMillis <= 0) {
      throw new IllegalArgumentException("must be positive");
    }
    if (isRunning()) {
      cancel();
    }
    isRepeating = true;
    timerId = setInterval(createCallback(this, cancelCounter), periodMillis);
  }

  /*
   * Called by native code when this timer fires.
   *
   * Only call run() if cancelCounter has not changed since the timer was scheduled.
   */
  final void fire(int scheduleCancelCounter) {
    // Workaround for broken clearTimeout in IE.
    if (scheduleCancelCounter != cancelCounter) {
      return;
    }

    if (!isRepeating) {
      timerId = null;
    }

    // Run the timer's code.
    run();
  }

  private static native JavaScriptObject createCallback(Timer timer, int cancelCounter) /*-{
    return $entry(function() { timer.@Timer::fire(I)(cancelCounter); });
  }-*/;

  private static native int setInterval(JavaScriptObject func, int time) /*-{
    return $wnd.setInterval(func, time);
  }-*/;

  private static native int setTimeout(JavaScriptObject func, int time) /*-{
    return $wnd.setTimeout(func, time);
  }-*/;

  private static native void clearInterval(int timerId) /*-{
    $wnd.clearInterval(timerId);
  }-*/;

  private static native void clearTimeout(int timerId) /*-{
    $wnd.clearTimeout(timerId);
  }-*/;
}
