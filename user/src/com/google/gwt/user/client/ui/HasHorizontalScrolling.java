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
package com.google.gwt.user.client.ui;

/**
 * Implemented by widgets that support horizontal scrolling.
 */
public interface HasHorizontalScrolling {

  /**
   * Gets the horizontal scroll position.
   * 
   * @return the horizontal scroll position, in pixels
   */
  int getHorizontalScrollPosition();

  /**
   * Get the maximum position of horizontal scrolling. This is usually the
   * <code>scrollWidth - clientWidth</code>.
   * 
   * @return the maximum horizontal scroll position
   */
  int getMaximumHorizontalScrollPosition();

  /**
   * Get the minimum position of horizontal scrolling.
   * 
   * @return the minimum horizontal scroll position
   */
  int getMinimumHorizontalScrollPosition();

  /**
   * Sets the horizontal scroll position.
   * 
   * @param position the new horizontal scroll position, in pixels
   */
  void setHorizontalScrollPosition(int position);
}
