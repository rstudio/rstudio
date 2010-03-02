/*
 * Copyright 2010 Google Inc.
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
 * Implementation of {@link com.google.gwt.user.client.ui.impl.FocusImpl} that
 * uses a hidden input element to serve as a 'proxy' for accesskeys, which are
 * only supported on form elements in most browsers.
 */
public class FocusImplStandard extends FocusImpl {

  /**
   * Single focusHandler shared by all focusable.
   */
  static JavaScriptObject focusHandler;

  private static native Element createFocusable0(JavaScriptObject focusHandler) /*-{
    // Divs are focusable in all browsers, but only IE supports the accessKey
    // property on divs. We use the infamous 'hidden input' trick to add an
    // accessKey to the focusable div. Note that the input is only used to
    // capture focus when the accessKey is pressed. Focus is forwarded to the
    // div immediately.
    var div = $doc.createElement('div');
    div.tabIndex = 0;

    var input = $doc.createElement('input');
    input.type = 'text';
    input.tabIndex = -1;
    var style = input.style;
    style.opacity = 0;
    style.height = '1px';
    style.width = '1px';
    style.zIndex = -1;
    style.overflow = 'hidden';
    style.position = 'absolute';

    // Note that we're using isolated lambda methods as the event listeners
    // to avoid creating a memory leaks. (Lambdas here would create cycles
    // involving the div and input).  This also allows us to share a single
    // set of handlers among every focusable item.
    input.addEventListener('focus', focusHandler, false);

    div.appendChild(input);
    return div;
  }-*/;

  @Override
  public Element createFocusable() {
    return createFocusable0(ensureFocusHandler());
  }

  @Override
  public native void setAccessKey(Element elem, char key) /*-{
    elem.firstChild.accessKey = String.fromCharCode(key);
  }-*/;

  /**
   * Use an isolated method call to create the handler to avoid creating memory
   * leaks via handler-closures-element.
   */
  private native JavaScriptObject createFocusHandler() /*-{
    return function(evt) {
      // This function is called directly as an event handler, so 'this' is
      // set up by the browser to be the input on which the event is fired. We
      // call focus() in a timeout or the element may be blurred when this event
      // ends.
      var div = this.parentNode;
      if (div.onfocus) {
        $wnd.setTimeout(function() {
          div.focus();
        }, 0);
      } 
    };
  }-*/;

  private JavaScriptObject ensureFocusHandler() {
    return focusHandler != null ? focusHandler : (focusHandler = createFocusHandler());
  }
}
