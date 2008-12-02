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

package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Event;

/**
 * Methods that need browser-specific implementations for Hyperlink.
 * By default, we're very conservative and let the browser handle any clicks
 * with non-left buttons or with modifier keys. This happens to be the correct
 * behavior for Firefox.
 */
public class HyperlinkImpl {

  /**
   * Default version, useful for Firefox. Don't fire if it's a rightclick,
   * middleclick, or if any modifiers are held down.
   */
  public boolean handleAsClick(Event event) {   
    int mouseButtons = event.getButton();
    boolean alt = event.getAltKey();
    boolean ctrl = event.getCtrlKey();
    boolean meta = event.getMetaKey();
    boolean shift = event.getShiftKey();    
    boolean modifiers = alt || ctrl || meta || shift;
    boolean middle = mouseButtons == Event.BUTTON_MIDDLE;
    boolean right = mouseButtons == Event.BUTTON_RIGHT;

    return !modifiers && !middle && !right;
  }
}
