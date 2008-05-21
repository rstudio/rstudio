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
    // Iterator through the animations
    double curTime = Duration.currentTimeMillis();
    for (int i = 0; i < animations.size(); i++) {
      Animation animation = animations.get(i);
      if (animation.update(curTime)) {
        animations.remove(i);
        i--;
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
   * Has the {@link Animation} been started.
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
    // Ignore if no animations are running
    if (animations == null) {
      return;
    }

    // Remove this animation from the list
    if (animations.remove(this)) {
      onCancel();
    }
    started = false;
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
    this.duration = duration;
    this.startTime = startTime;

    // Start synchronously if start time has passed
    if (update(Duration.currentTimeMillis())) {
      return;
    }

    // Add to the list of animations
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
      started = false;
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
      started = false;
      onComplete();
      return true;
    }
    return false;
  }
}
