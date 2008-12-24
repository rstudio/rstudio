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
 * HyperlinkImpl for Safari and Google Chrome. Safari has special behavior for
 * all the modifier keys except shift, which behaves like a regular click.
 * Chrome, however, opens a new window on a shift-click.
 */
public class HyperlinkImplSafari extends HyperlinkImpl {

  private static boolean shiftIsModifier = onChrome();

  private static native boolean onChrome() /*-{
    return navigator.userAgent.indexOf("Chrome") != -1;
  }-*/;

  @Override
  public boolean handleAsClick(Event event) {
    int mouseButtons = event.getButton();
    boolean alt = event.getAltKey();
    boolean ctrl = event.getCtrlKey();
    boolean meta = event.getMetaKey();
    boolean shift = event.getShiftKey();
    boolean middle = mouseButtons == Event.BUTTON_MIDDLE;
    boolean right = mouseButtons == Event.BUTTON_RIGHT;

    boolean modifiers = alt || ctrl || meta;
    if (shiftIsModifier) {
      modifiers |= shift;
    }
    
    return !modifiers && !middle && !right;
  }
}
