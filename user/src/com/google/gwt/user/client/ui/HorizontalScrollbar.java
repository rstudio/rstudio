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

import com.google.gwt.event.dom.client.HasScrollHandlers;

/**
 * Describes a horizontal scrollbar.
 */
public interface HorizontalScrollbar extends HasHorizontalScrolling, HasScrollHandlers, IsWidget {

  /**
   * Get the width in pixels of the scrollable content that the scrollbar
   * controls.
   * 
   * <p>
   * This is not the same as the maximum scroll left position. The maximum
   * scroll position equals the <code>scrollWidth - offsetWidth</code>.
   * 
   * @return the scroll width
   * @see #setScrollWidth(int)
   */
  int getScrollWidth();

  /**
   * Set the width in pixels of the scrollable content that the scrollbar
   * controls.
   * 
   * <p>
   * This is not the same as the maximum scroll left position. The maximum
   * scroll position equals the <code>scrollWidth - offsetWidth</code>.
   * 
   * @param width the size width pixels
   */
  void setScrollWidth(int width);
}
