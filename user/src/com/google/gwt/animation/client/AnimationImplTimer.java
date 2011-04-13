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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation using a timer.
 */
class AnimationImplTimer extends AnimationImpl {

  /**
   * The default time in milliseconds between frames.
   */
  private static final int DEFAULT_FRAME_DELAY = 25;

  /**
   * The {@link Scheduler.RepeatingCommand} that applies the animations.
   */
  private static Scheduler.RepeatingCommand animationCommand;

  /**
   * The {@link Animation Animations} that are currently in progress.
   */
  private static List<Animation> animations = null;

  @Override
  public void cancel(Animation animation) {
    animations.remove(animation);
  }

  @Override
  public void run(Animation animation, Element element) {
    // Add to the list of animations

    // We use a static list of animations and a single timer, and create them
    // only if we are the only active animation. This is safe since JS is
    // single-threaded.
    if (animations == null) {
      animations = new ArrayList<Animation>();
      animationCommand = new Scheduler.RepeatingCommand() {
        public boolean execute() {
          // Duplicate the animations list in case it changes as we iterate over it
          Animation[] curAnimations = new Animation[animations.size()];
          curAnimations = animations.toArray(curAnimations);

          // Iterate through the animations
          double curTime = Duration.currentTimeMillis();
          for (Animation animation : curAnimations) {
            if (updateAnimation(animation, curTime)) {
              // We can't just remove the animation at the index, because calling
              // animation.update may have the side effect of canceling this
              // animation, running new animations, or canceling other animations.
              animations.remove(animation);
            }
          }

          return (animations.size() > 0);
        }
      };
    }
    animations.add(animation);

    // Restart the timer if this is the only animation
    if (animations.size() == 1) {
      Scheduler.get().scheduleFixedDelay(animationCommand, DEFAULT_FRAME_DELAY);
    }
  }
}
