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

import com.google.gwt.core.client.GWT;

/**
 * An {@link WidgetAnimation} is an {@link Animation} specifically designed for
 * use with Widgets. If the animation is disabled, the #onInstantaneousRun
 * method will be called instead of onStart and onComplete. This allows you to
 * increase performance when animations are disabled.
 */
public abstract class WidgetAnimation extends Animation {
  /**
   * Implementation class for {@link WidgetAnimation} that does not allow any
   * animations. If an animation is called, it is immediately started and
   * completed.
   */
  public static class WidgetAnimationImpl {
    protected void cancel(WidgetAnimation anim) {
    }

    protected void run(WidgetAnimation anim, int duration, double startTime) {
      anim.onRunWhenDisabled();
    }
  }

  /**
   * Implementation class for {@link WidgetAnimation} that actually performs
   * animations.
   */
  public static class WidgetAnimationImplEnabled extends WidgetAnimationImpl {
    @Override
    protected void cancel(WidgetAnimation anim) {
      Animation.impl.cancel(anim);
    }

    @Override
    protected void run(WidgetAnimation anim, int duration, double startTime) {
      Animation.impl.run(anim, duration, startTime);
    }
  }

  /**
   * The implementation class.
   */
  private static WidgetAnimationImpl widgetImpl = GWT.create(WidgetAnimationImpl.class);

  /**
   * Immediately cancel this animation.
   */
  @Override
  public void cancel() {
    widgetImpl.cancel(this);
  }

  /**
   * Called if this animation is run when it is disabled. If this method is
   * called, {@link #onStart()} and {@link #onComplete()} will not be called.
   */
  public abstract void onInstantaneousRun();

  /**
   * Run this animation at the given startTime. If the startTime has already
   * passed, the animation will be synchronize as if it started at the specified
   * start time. If the animation is already running, it will be canceled first.
   * 
   * @param duration the duration of the animation in milliseconds
   * @param startTime the synchronized start time in milliseconds
   */
  @Override
  public void run(int duration, double startTime) {
    widgetImpl.run(this, duration, startTime);
  }

  /**
   * Called when we run an animation that is disabled.
   */
  @Override
  void onRunWhenDisabled() {
    onInstantaneousRun();
  }

}
