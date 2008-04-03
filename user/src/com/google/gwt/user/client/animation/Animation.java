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
package com.google.gwt.user.client.animation;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Animation} is a continuous event that updates progressively over
 * time at a non-fixed frame rate.
 */
public abstract class Animation {
  /**
   * Implementation class for {@link Animation} that does not allow any
   * animations. If an animation is called, it is immediately started and
   * completed.
   */
  public static class AnimationImpl {
    protected void cancel(Animation anim) {
    }

    protected void run(Animation anim, int duration, double startTime) {
      anim.onRunWhenDisabled();
    }
  }

  /**
   * Implementation class for {@link Animation} that actually performs
   * animations.
   */
  public static class AnimationImplEnabled extends AnimationImpl {
    /**
     * The default time in milliseconds between frames.
     */
    private static final int DEFAULT_FRAME_DELAY = 25;

    /**
     * The {@link Animation}s that are currently in progress.
     */
    private static List<Animation> animations;

    /**
     * The {@link Timer} that applies the animations.
     */
    private static Timer animationTimer;

    @Override
    protected void cancel(Animation anim) {
      // No animations available
      if (animations == null) {
        return;
      }

      // Remove the animation
      anim.started = false;
      if (animations.remove(anim)) {
        anim.onCancel();
      }
    }

    @Override
    protected void run(Animation anim, int duration, double startTime) {
      // Cancel the animation if it is running
      anim.cancel();

      // Save the duration and startTime
      anim.duration = duration;
      anim.startTime = startTime;

      // Start synchronously if start time has passed
      if (anim.update(Duration.currentTimeMillis())) {
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
      animations.add(anim);

      // Restart the timer if there is the only animation
      if (animations.size() == 1) {
        animationTimer.schedule(DEFAULT_FRAME_DELAY);
      }
    }

    /**
     * Update all {@link Animation}s.
     */
    private void updateAnimations() {
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
  }

  /**
   * The implementation class.
   */
  static AnimationImpl impl = GWT.create(AnimationImpl.class);

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
   * Immediately cancel this animation.
   */
  public void cancel() {
    impl.cancel(this);
  }

  /**
   * Called immediately after the animation is canceled.
   */
  public abstract void onCancel();

  /**
   * Called immediately after the animation completes.
   */
  public abstract void onComplete();

  /**
   * Called immediately before the animation starts.
   */
  public abstract void onStart();

  /**
   * Called when the animation should be updated.
   * 
   * The value of progress is between 0.0 and 1.0 inclusively, but it is not
   * safe to assume that either 0.0 or 1.0 will be passed in. Use
   * {@link #onStart()} and {@link #onComplete()} to do setup and tear down
   * procedures.
   */
  public abstract void onUpdate(double progress);

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
    impl.run(this, duration, startTime);
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
   * Called when we run an animation that is disabled.
   */
  void onRunWhenDisabled() {
    onStart();
    onComplete();
  }

  /**
   * Update the {@link Animation}.
   * 
   * @param curTime the current time
   * @return true if the animation is complete, false if still running
   */
  private boolean update(double curTime) {
    // Start the animation
    if (!started && curTime >= startTime) {
      started = true;
      onStart();
    }

    if (curTime >= startTime + duration) {
      // Animation is complete
      started = false;
      onComplete();
      return true;
    } else if (curTime >= startTime) {
      // Animation is in progress
      double progress = (double) (curTime - startTime) / (double) duration;
      onUpdate(interpolate(progress));
    }
    return false;
  }
}
