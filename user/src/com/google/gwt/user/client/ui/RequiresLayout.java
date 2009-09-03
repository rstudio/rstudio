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

import com.google.gwt.layout.client.Layout;

/**
 * Designates that a widget requires a method to be explicitly called after its
 * children are modified.
 * 
 * <p>
 * Widgets that implement this interface perform some layout work that will not
 * be fully realized until {@link #layout()} or one of its overloads is called.
 * This is required after adding or removing child widgets, and after any other
 * operations that the implementor designates as requiring layout. Note that
 * only <em>one</em> call to {@link #layout()} is required after any number of
 * modifications.
 * </p>
 */
public interface RequiresLayout {

  /**
   * Layout children immediately.
   * 
   * @see #layout(int)
   * @see #layout(int, com.google.gwt.layout.client.Layout.AnimationCallback)
   */
  void layout();

  /**
   * Layout children, animating over the specified period of time.
   * 
   * @param duration the animation duration, in milliseconds
   * 
   * @see #layout()
   * @see #layout(int, com.google.gwt.layout.client.Layout.AnimationCallback)
   */
  void layout(int duration);

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
   * 
   * @see #layout()
   * @see #layout(int, com.google.gwt.layout.client.Layout.AnimationCallback)
   */
  void layout(int duration, final Layout.AnimationCallback callback);
}
