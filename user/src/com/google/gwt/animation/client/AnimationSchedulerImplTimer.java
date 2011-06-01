/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation using a timer for browsers that do not support animation
 * frames.
 */
class AnimationSchedulerImplTimer extends AnimationSchedulerImpl {

  /**
   * Timer based implementation of {@link AnimationScheduler.AnimationHandle}.
   */
  private class AnimationHandleImpl extends AnimationHandle {
    private final AnimationCallback callback;

    public AnimationHandleImpl(AnimationCallback callback) {
      this.callback = callback;
    }

    @Override
    public void cancel() {
      cancelAnimationFrame(this);
    }

    public AnimationCallback getCallback() {
      return callback;
    }
  }

  /**
   * The default time in milliseconds between frames. 60 fps == 16.67 ms.
   */
  private static final int DEFAULT_FRAME_DELAY = 16;

  /**
   * The minimum delay in milliseconds between frames. The minimum delay is
   * imposed to prevent freezing the UI.
   */
  private static final int MIN_FRAME_DELAY = 5;

  /**
   * The list of animations that are currently running.
   */
  private final List<AnimationHandleImpl> animationRequests = new ArrayList<AnimationHandleImpl>();

  /**
   * The singleton timer that updates all animations.
   */
  private final Timer timer = new Timer() {
    @Override
    public void run() {
      updateAnimations();
    }
  };

  @Override
  public AnimationHandle requestAnimationFrame(final AnimationCallback callback, Element element) {
    // Save the animation frame request.
    AnimationHandleImpl requestId = new AnimationHandleImpl(callback);
    animationRequests.add(requestId);

    // Start the timer if it isn't started.
    if (animationRequests.size() == 1) {
      timer.schedule(DEFAULT_FRAME_DELAY);
    }

    // Return the request id.
    return requestId;
  }

  @Override
  protected boolean isNativelySupported() {
    return true;
  }

  private void cancelAnimationFrame(AnimationHandle requestId) {
    // Remove the request from the list.
    animationRequests.remove(requestId);

    // Stop the timer if there are no more requests.
    if (animationRequests.size() == 0) {
      timer.cancel();
    }
  }

  /**
   * Iterate over all animations and update them.
   */
  private void updateAnimations() {
    // Copy the animation requests to avoid concurrent modifications.
    AnimationHandleImpl[] curAnimations = new AnimationHandleImpl[animationRequests.size()];
    curAnimations = animationRequests.toArray(curAnimations);

    // Iterate over the animation requests.
    Duration duration = new Duration();
    for (AnimationHandleImpl requestId : curAnimations) {
      // Remove the current request.
      animationRequests.remove(requestId);

      // Execute the callback.
      requestId.getCallback().execute(duration.getStartMillis());
    }

    // Reschedule the timer if there are more animation requests.
    if (animationRequests.size() > 0) {
      /*
       * In order to achieve as close to 60fps as possible, we calculate the new
       * delay based on the execution time of this method. The delay will be
       * less than 16ms, assuming this method takes more than 1ms to complete.
       */
      timer.schedule(Math.max(MIN_FRAME_DELAY, DEFAULT_FRAME_DELAY - duration.elapsedMillis()));
    }
  }
}
