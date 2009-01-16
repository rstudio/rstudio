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
package com.google.gwt.user.client;

/**
 * Implement this interface to receive scroll events from the browser window.
 * 
 * @see com.google.gwt.user.client.Window#addWindowScrollListener(WindowScrollListener)
 * @deprecated use
 *             {@link Window#addWindowScrollHandler(com.google.gwt.user.client.Window.ScrollHandler)}
 *             instead instead
 */
@Deprecated
public interface WindowScrollListener extends java.util.EventListener {

  /**
   * Called when the browser window is scrolled.
   * 
   * @param scrollLeft the left scroll position
   * @param scrollTop the top scroll position
   */
  @Deprecated
  void onWindowScrolled(int scrollLeft, int scrollTop);
}
