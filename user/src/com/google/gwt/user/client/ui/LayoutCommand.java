/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * A scheduled command used by animated layouts to ensure that only layout is
 * ever performed for a panel within a given user event.
 * 
 * <p>
 * Note: This class assumes that {@link com.google.gwt.layout.client.Layout.Layer#getUserObject Layer.getUserObject()} will
 * return the widget associated with a given layer.
 * </p>
 */
public class LayoutCommand implements ScheduledCommand {

  private boolean scheduled, canceled;
  private int duration;
  private Layout.AnimationCallback callback;
  private final Layout layout;

  /**
   * Creates a new command for the given layout object.
   * 
   * @param layout
   */
  public LayoutCommand(Layout layout) {
    this.layout = layout;
  }

  /**
   * Cancels this command. A subsequent call to
   * {@link #schedule(int, Layout.AnimationCallback)} will re-enable it.
   */
  public void cancel() {
    // There's no way to "unschedule" a command, so we use a canceled flag.
    canceled = true;
  }

  public final void execute() {
    scheduled = false;
    if (canceled) {
      return;
    }

    doBeforeLayout();

    layout.layout(duration, new Layout.AnimationCallback() {
      public void onAnimationComplete() {
        // Chain to the passed callback.
        if (callback != null) {
          callback.onAnimationComplete();
        }
      }

      public void onLayout(Layer layer, double progress) {
        // Inform the child associated with this layer that its size may
        // have changed.
        Widget child = (Widget) layer.getUserObject();
        if (child instanceof RequiresResize) {
          ((RequiresResize) child).onResize();
        }

        // Chain to the passed callback.
        if (callback != null) {
          callback.onLayout(layer, progress);
        }
      }
    });
  }

  /**
   * Schedules a layout. The duration and callback passed to this method will
   * supercede any previous call that has not yet been executed.
   * 
   * @param duration
   * @param callback
   */
  public void schedule(int duration, AnimationCallback callback) {
    this.duration = duration;
    this.callback = callback;

    canceled = false;
    if (!scheduled) {
      scheduled = true;
      Scheduler.get().scheduleFinally(this);
    }
  }

  /**
   * Called before the layout is executed. Override this method to perform any
   * work that needs to happen just before it.
   */
  protected void doBeforeLayout() {
  }
}
