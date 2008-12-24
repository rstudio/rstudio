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
 * IE version of HyperlinkImpl. IE6 and IE7 actually have different
 * behavior; both have special behavior for shift-click, but IE7 also opens
 * in a new tab on ctrl-click. IE6 treats ctrl-click as a standard click.
 */
public class HyperlinkImplIE extends HyperlinkImpl {
  
  private static boolean ctrlisModifier = (getInternetExplorerVersion() >= 7);

  /**
   * Returns the version of Internet Explorer or a -1, (indicating the use of
   * another browser). Based on code from MSDN.
   * http://msdn2.microsoft.com/en-us/library/ms537509.aspx
   */
  private static native int getInternetExplorerVersion() /*-{
    var rv = -1; // Assume that we're not IE.
           
    if (navigator.appName == 'Microsoft Internet Explorer') {
      var ua = navigator.userAgent;
      var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null)
        rv = parseFloat( RegExp.$1 );
    }
    
    return rv;
  }-*/;
  
  @Override
  public boolean handleAsClick(Event event) {
    int mouseButtons = event.getButton();
    boolean ctrl = event.getCtrlKey();
    boolean shift = event.getShiftKey();
    boolean middle = mouseButtons == Event.BUTTON_MIDDLE;
    boolean right = mouseButtons == Event.BUTTON_RIGHT;
    boolean modifiers;
    
    if (ctrlisModifier) {
      modifiers = shift || ctrl;
    } else {
      modifiers = shift;  
    }

    return !modifiers && !middle && !right;
  }
}
