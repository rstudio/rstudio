/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.animation.client;

import com.google.gwt.animation.client.AnimationScheduler.AnimationCallback;
import com.google.gwt.animation.client.AnimationScheduler.AnimationHandle;
import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Element;

/**
 * An {@link Animation} is a continuous event that updates progressively over
 * time at a non-fixed frame rate.
 */
public abstract class Animation {

  private final AnimationCallback callback = new AnimationCallback() {
    @Override
    public void execute(double timestamp) {
      if (update(timestamp)) {
        // Schedule the next animation frame.
        requestHandle = scheduler.requestAnimationFrame(callback, element);
      } else {
        requestHandle = null;
      }
    }
  };

  /**
   * The duration of the {@link Animation} in milliseconds.
   */
  private int duration = -1;

  /**
   * The element being animated.
   */
  private Element element;

  /**
   * Is the animation running, even if it hasn't started yet.
   */
  private boolean isRunning = false;

  /**
   * Has the {@link Animation} actually started.
   */
  private boolean isStarted = false;

  /**
   * The ID of the pending animation request.
   */
  private AnimationHandle requestHandle;

  /**
   * The unique ID of the current run. Used to handle cases where an animation
   * is restarted within an execution block.
   */
  private int runId = -1;

  private final AnimationScheduler scheduler;

  /**
   * The start time of the {@link Animation}.
   */
  private double startTime = -1;

  /**
   * Did the animation start before {@link #cancel()} was called.
   */
  private boolean wasStarted = false;

  /**
   * Construct a new {@link Animation}.
   */
  public Animation() {
    this(AnimationScheduler.get());
  }

  /**
   * Construct a new {@link AnimationScheduler} using the specified scheduler to
   * sheduler request frames.
   * 
   * @param scheduler an {@link AnimationScheduler} instance
   */
  protected Animation(AnimationScheduler scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Immediately cancel this animation. If the animation is running or is
   * scheduled to run, {@link #onCancel()} will be called.
   */
  public void cancel() {
    // Ignore if the animation is not currently running.
    if (!isRunning) {
      return;
    }

    // Reset the state.
    wasStarted = isStarted; // Used by onCancel.
    element = null;
    isRunning = false;
    isStarted = false;

    // Cancel the animation request.
    if (requestHandle != null) {
      requestHandle.cancel();
      requestHandle = null;
    }

    onCancel();
  }

  /**
   * Immediately run this animation. If the animation is already running, it
   * will be canceled first.
   * <p>
   * This is equivalent to <code>run(duration, null)</code>.
   * 
   * @param duration the duration of the animation in milliseconds
   * @see #run(int, Element)
   */
  public void run(int duration) {
    run(duration, null);
  }

  /**
   * Immediately run this animation. If the animation is already running, it
   * will be canceled first.
   * <p>
   * If the element is not <code>null</code>, the {@link #onUpdate(double)}
   * method might be called only if the element may be visible (generally left
   * at the appreciation of the browser). Otherwise, it will be called
   * unconditionally.
   * 
   * @param duration the duration of the animation in milliseconds
   * @param element the element that visually bounds the entire animation
   */
  public void run(int duration, Element element) {
    run(duration, Duration.currentTimeMillis(), element);
  }

  /**
   * Run this animation at the given startTime. If the startTime has already
   * passed, the animation will run synchronously as if it started at the
   * specified start time. If the animation is already running, it will be
   * canceled first.
   * <p>
   * This is equivalent to <code>run(duration, startTime, null)</code>.
   * 
   * @param duration the duration of the animation in milliseconds
   * @param startTime the synchronized start time in milliseconds
   * @see #run(int, double, Element)
   */
  public void run(int duration, double startTime) {
    run(duration, startTime, null);
  }

  /**
   * Run this animation at the given startTime. If the startTime has already
   * passed, the animation will run synchronously as if it started at the
   * specified start time. If the animation is already running, it will be
   * canceled first.
   * <p>
   * If the element is not <code>null</code>, the {@link #onUpdate(double)}
   * method might be called only if the element may be visible (generally left
   * at the appreciation of the browser). Otherwise, it will be called
   * unconditionally.
   * 
   * @param duration the duration of the animation in milliseconds
   * @param startTime the synchronized start time in milliseconds
   * @param element the element that visually bounds the entire animation
   */
  public void run(int duration, double startTime, Element element) {
    // Cancel the animation if it is running
    cancel();

    // Save the duration and startTime
    isRunning = true;
    isStarted = false;
    this.duration = duration;
    this.startTime = startTime;
    this.element = element;
    ++runId;

    // Execute the first callback.
    callback.execute(Duration.currentTimeMillis());
  }

  /**
   * Interpolate the linear progress into a more natural easing function.
   * 
   * Depending on the {@link Animation}, the return value of this method can be
   * less than 0.0 or greater than 1.0.
   * 
   * @param progress the linear progress, between 0.0 and 1.0
   * @return the interpolated progress
   */
  protected double interpolate(double progress) {
    return (1 + Math.cos(Math.PI + progress * Math.PI)) / 2;
  }

  /**
   * Called immediately after the animation is canceled. The default
   * implementation of this method calls {@link #onComplete()} only if the
   * animation has actually started running.
   */
  protected void onCancel() {
    if (wasStarted) {
      onComplete();
    }
  }

  /**
   * Called immediately after the animation completes.
   */
  protected void onComplete() {
    onUpdate(interpolate(1.0));
  }

  /**
   * Called immediately before the animation starts.
   */
  protected void onStart() {
    onUpdate(interpolate(0.0));
  }

  /**
   * Called when the animation should be updated.
   * 
   * The value of progress is between 0.0 and 1.0 (inclusive) (unless you
   * override the {@link #interpolate(double)} method to provide a wider range
   * of values). You can override {@link #onStart()} and {@link #onComplete()}
   * to perform setup and tear down procedures.
   * 
   * @param progress a double, normally between 0.0 and 1.0 (inclusive)
   */
  protected abstract void onUpdate(double progress);

  /**
   * Check if the specified run ID is still being run.
   * 
   * @param curRunId the current run ID to check
   * @return true if running, false if canceled or restarted
   */
  private boolean isRunning(int curRunId) {
    return isRunning && (runId == curRunId);
  }

  /**
   * Update the {@link Animation}.
   * 
   * @param curTime the current time
   * @return true if the animation should run again, false if it is complete
   */
  private boolean update(double curTime) {
    /*
     * Save the run id. If the runId is incremented during this execution block,
     * we know that this run has been canceled.
     */
    final int curRunId = runId;

    boolean finished = curTime >= startTime + duration;
    if (isStarted && !finished) {
      // Animation is in progress.
      double progress = (curTime - startTime) / duration;
      onUpdate(interpolate(progress));
      return isRunning(curRunId); // Check if this run was canceled.
    }
    if (!isStarted && curTime >= startTime) {
      /*
       * Start the animation. We do not call onUpdate() because onStart() calls
       * onUpdate() by default.
       */
      isStarted = true;
      onStart();
      if (!isRunning(curRunId)) {
        // This run was canceled.
        return false;
      }
      // Intentional fall through to possibly end the animation.
    }
    if (finished) {
      // Animation is complete.
      isRunning = false;
      isStarted = false;
      onComplete();
      return false;
    }
    return true;
  }
}
