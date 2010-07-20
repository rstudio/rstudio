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

import com.google.gwt.layout.client.Layout.AnimationCallback;

/**
 * Specifies that a panel can animate between layouts.
 * 
 * <p>
 * The normal use pattern is to set all childrens' positions, then to call
 * {@link #animate(int)} to move them to their new positions over some period
 * of time.
 * </p>
 */
public interface AnimatedLayout {

  /**
   * Layout children, animating over the specified period of time.
   * 
   * @param duration the animation duration, in milliseconds
   */
  void animate(int duration);

  /**
   * Layout children, animating over the specified period of time.
   * 
   * <p>
   * This method provides a callback that will be informed of animation updates.
   * This can be used to create more complex animation effects.
   * </p>
   * 
   * @param duration the animation duration, in milliseconds
   * @param callback the animation callback
   */
  void animate(final int duration, final AnimationCallback callback);

  /**
   * Layout children immediately.
   * 
   * <p>
   * This is not normally necessary, unless you want to update child widgets'
   * positions explicitly to create a starting point for a subsequent call to
   * {@link #animate(int)}.
   * </p>
   * 
   * @see #animate(int)
   * @see #animate(int, Layout.AnimationCallback)
   */
  void forceLayout();
}
