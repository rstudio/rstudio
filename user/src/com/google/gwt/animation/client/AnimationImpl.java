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

import com.google.gwt.dom.client.Element;

/**
 * Base class for animation implementations.
 */
abstract class AnimationImpl {

  /**
   * Cancel the animation.
   */
  public abstract void cancel(Animation animation);

  /**
   * Run the animation with an optional bounding element.
   */
  public abstract void run(Animation animation, Element element);

  /**
   * Update the {@link Animation}.
   * 
   * @param animation the {@link Animation}
   * @param curTime the current time
   * @return true if the animation is complete, false if still running
   */
  protected final boolean updateAnimation(Animation animation, double curTime) {
    return animation.isRunning() && animation.update(curTime);
  }
}
