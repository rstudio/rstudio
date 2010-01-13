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

  /*
   * Use isolated method calls to create all of the handlers to avoid creating
   * memory leaks via handler-closures-element.
   */
  JavaScriptObject focusHandler = createFocusHandler();

  @Override
  public native Element createFocusable() /*-{
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
    input.style.opacity = 0;
    input.style.height = '1px';
    input.style.width = '1px';
    input.style.zIndex = -1;
    input.style.overflow = 'hidden';
    input.style.position = 'absolute';

    // Note that we're using isolated lambda methods as the event listeners
    // to avoid creating a memory leaks. (Lambdas here would create cycles
    // involving the div and input).  This also allows us to share a single
    // set of handlers among every focusable item.
    input.addEventListener(
      'focus',
      this.@com.google.gwt.user.client.ui.impl.FocusImplStandard::focusHandler,
      false);

    div.appendChild(input);
    return div;
  }-*/;

  @Override
  public native void setAccessKey(Element elem, char key) /*-{
    elem.firstChild.accessKey = String.fromCharCode(key);
  }-*/;

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
}
