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

import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Animation} is a continuous event that updates progressively over
 * time at a non-fixed frame rate.
 */
public abstract class Animation {
  /**
   * The default time in milliseconds between frames.
   */
  private static final int DEFAULT_FRAME_DELAY = 25;

  /**
   * The {@link Animation Animations} that are currently in progress.
   */
  private static List<Animation> animations = null;

  /**
   * The {@link Timer} that applies the animations.
   */
  private static Timer animationTimer = null;

  /**
   * Update all {@link Animation Animations}.
   */
  private static void updateAnimations() {
    // Duplicate the animations list in case it changes as we iterate over it
    Animation[] curAnimations = new Animation[animations.size()];
    curAnimations = animations.toArray(curAnimations);

    // Iterator through the animations
    double curTime = Duration.currentTimeMillis();
    for (Animation animation : curAnimations) {
      if (animation.running && animation.update(curTime)) {
        // We can't just remove the animation at the index, because calling
        // animation.update may have the side effect of canceling this
        // animation, running new animations, or canceling other animations.
        animations.remove(animation);
      }
    }

    // Reschedule the timer
    if (animations.size() > 0) {
      animationTimer.schedule(DEFAULT_FRAME_DELAY);
    }
  }

  /**
   * The duration of the {@link Animation} in milliseconds.
   */
  private int duration = -1;

  /**
   * Is the {@link Animation} running, even if {@link #onStart()} has not yet
   * been called.
   */
  private boolean running = false;

  /**
   * Has the {@link Animation} actually started.
   */
  private boolean started = false;

  /**
   * The start time of the {@link Animation}.
   */
  private double startTime = -1;

  /**
   * Immediately cancel this animation. If the animation is running or is
   * scheduled to run, {@link #onCancel()} will be called.
   */
  public void cancel() {
    // Ignore if the animation is not currently running
    if (!running) {
      return;
    }

    animations.remove(this);
    onCancel();
    started = false;
    running = false;
  }

  /**
   * Immediately run this animation. If the animation is already running, it
   * will be canceled first.
   * 
   * @param duration the duration of the animation in milliseconds
   */
  public void run(int duration) {
    run(duration, Duration.currentTimeMillis());
  }

  /**
   * Run this animation at the given startTime. If the startTime has already
   * passed, the animation will be synchronize as if it started at the specified
   * start time. If the animation is already running, it will be canceled first.
   * 
   * @param duration the duration of the animation in milliseconds
   * @param startTime the synchronized start time in milliseconds
   */
  public void run(int duration, double startTime) {
    // Cancel the animation if it is running
    cancel();

    // Save the duration and startTime
    this.running = true;
    this.duration = duration;
    this.startTime = startTime;

    // Start synchronously if start time has passed
    if (update(Duration.currentTimeMillis())) {
      return;
    }

    // Add to the list of animations

    // We use a static list of animations and a single timer, and create them
    // only if we are the only active animation. This is safe since JS is
    // single-threaded.
    if (animations == null) {
      animations = new ArrayList<Animation>();
      animationTimer = new Timer() {
        @Override
        public void run() {
          updateAnimations();
        }
      };
    }
    animations.add(this);

    // Restart the timer if there is the only animation
    if (animations.size() == 1) {
      animationTimer.schedule(DEFAULT_FRAME_DELAY);
    }
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
    if (started) {
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
   * The value of progress is between 0.0 and 1.0 inclusively (unless you
   * override the {@link #interpolate(double)} method to provide a wider range
   * of values). You can override {@link #onStart()} and {@link #onComplete()}
   * to perform setup and tear down procedures.
   */
  protected abstract void onUpdate(double progress);

  /**
   * Update the {@link Animation}.
   * 
   * @param curTime the current time
   * @return true if the animation is complete, false if still running
   */
  private boolean update(double curTime) {
    boolean finished = curTime >= startTime + duration;
    if (started && !finished) {
      // Animation is in progress.
      double progress = (curTime - startTime) / duration;
      onUpdate(interpolate(progress));
      return false;
    }
    if (!started && curTime >= startTime) {
      // Start the animation.
      started = true;
      onStart();
      // Intentional fall through to possibly end the animation.
    }
    if (finished) {
      // Animation is complete.
      onComplete();
      started = false;
      running = false;
      return true;
    }
    return false;
  }
}
