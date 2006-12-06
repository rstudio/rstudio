/*
 * Copyright 2006 Google Inc.
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
 * A widget that implements this interface sources the events defined by the
 * {@link com.google.gwt.user.client.ui.TabListener} interface.
 */
public interface SourcesTabEvents {

  /**
   * Adds a listener interface to receive click events.
   * 
   * @param listener the listener interface to add
   */
  public void addTabListener(TabListener listener);

  /**
   * Removes a previously added listener interface.
   * 
   * @param listener the listener interface to remove
   */
  public void removeTabListener(TabListener listener);
}
