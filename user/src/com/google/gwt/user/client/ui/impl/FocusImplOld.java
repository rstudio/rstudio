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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;

/**
 * Crazy implementation of {@link com.google.gwt.user.client.ui.impl.FocusImpl}
 * that uses a hidden anchor to serve as a 'proxy' for focus.
 */
public class FocusImplOld extends FocusImpl {

  static JavaScriptObject blurHandler = createBlurHandler();
  static JavaScriptObject focusHandler = createFocusHandler();
  static JavaScriptObject mouseHandler = createMouseHandler();

  private static native JavaScriptObject createBlurHandler() /*-{
    return function(evt) {
      // This function is called directly as an event handler, so 'this' is
      // set up by the browser to be the input on which the event is fired.
      if (this.parentNode.onblur) {
        this.parentNode.onblur(evt);
      } 
    };
  }-*/;

  private static native JavaScriptObject createFocusHandler() /*-{
    return function(evt) {
      // This function is called directly as an event handler, so 'this' is
      // set up by the browser to be the input on which the event is fired.
      if (this.parentNode.onfocus) {
        this.parentNode.onfocus(evt);
      } 
    };
  }-*/;

  private static native JavaScriptObject createMouseHandler() /*-{
    return function() {
      // This function is called directly as an event handler, so 'this' is
      // set up by the browser to be the div on which the event is fired.
      this.firstChild.focus();
    };
  }-*/;

  public native void blur(Element elem) /*-{
    elem.firstChild.blur();
  }-*/;

  public native Element createFocusable() /*-{
    // Use the infamous 'hidden input' trick to make a div effectively
    // focusable.
    var div = $doc.createElement('div');
    var input = $doc.createElement('input');
    input.type = 'text';
    input.style.width = input.style.height = 0;
    input.style.zIndex = -1;
    input.style.position = 'absolute';

    // Add a mousedown listener to the div to focuses the input (to mimic the
    // behavior of focusable elements on other browsers), and focus listeners
    // on the input to propagate focus events back to the div.

    // Note that we're using isolated lambda methods as the event listeners
    // to avoid creating a memory leaks. (Lambdas here would create cycles
    // involving the div and input).  This also allows us to share a single
    // set of handlers among every focusable item.

    input.addEventListener(
      'blur',
      @com.google.gwt.user.client.ui.impl.FocusImplOld::blurHandler,
      false);

    input.addEventListener(
      'focus',
      @com.google.gwt.user.client.ui.impl.FocusImplOld::focusHandler,
      false);

    div.addEventListener(
      'mousedown',
      @com.google.gwt.user.client.ui.impl.FocusImplOld::mouseHandler,
      false);

    div.appendChild(input);
    return div;
  }-*/;

  public native void focus(Element elem) /*-{
    elem.firstChild.focus();
  }-*/;

  public native int getTabIndex(Element elem) /*-{
    return elem.firstChild.tabIndex;
  }-*/;

  public native void setAccessKey(Element elem, char key) /*-{
    elem.firstChild.accessKey = key;
  }-*/;

  public native void setTabIndex(Element elem, int index) /*-{
    elem.firstChild.tabIndex = index;
  }-*/;
}
