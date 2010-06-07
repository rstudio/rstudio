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

/**
 * Mozilla-specific implementation of rich-text editing.
 */
public class RichTextAreaImplMozilla extends RichTextAreaImplStandard {

  /**
   * Indicates that the RichTextArea has never received focus after
   * initialization.
   */
  boolean isFirstFocus;

  @Override
  public native void initElement() /*-{
    // Mozilla doesn't allow designMode to be set reliably until the iframe is
    // fully loaded.
    var _this = this;
    var iframe = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitializing()();
    _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla::isFirstFocus = true;

    iframe.onload = $entry(function() {
      // Some Mozillae have the nasty habit of calling onload again when you set
      // designMode, so let's avoid doing it more than once.
      iframe.onload = null;

      // Don't set designMode until the RTA is targeted by an event. This is
      // necessary because editing won't work on Mozilla if the iframe is
      // *hidden, but attached*. Waiting for an event gets around this issue.
      //
      // Note: These events will not conflict with the
      // addEventListener('oneventtype', ...) in RichTextAreaImplStandard.
      iframe.contentWindow.onfocus = function() {
        iframe.contentWindow.onfocus = null;
        iframe.contentWindow.onmouseover = null;
        iframe.contentWindow.document.designMode = 'On';
      };

      // Issue 1441: we also need to catch the onmouseover event because focus
      // occurs after mouse down, so the cursor will not appear until the user
      // clicks twice, making the RichTextArea look uneditable. Catching the
      // mouseover event allows us to set design mode earlier. The focus event
      // is still needed to handle tab selection.
      iframe.contentWindow.onmouseover = iframe.contentWindow.onfocus;

      // Send notification that the iframe has finished loading.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    });
  }-*/;

  @Override
  public void setBackColor(String color) {
    // Gecko uses 'BackColor' for the *entire area's* background. 'HiliteColor'
    // does what we actually want.
    execCommand("HiliteColor", color);
  }

  /**
   * Firefox will not display the caret the first time a RichTextArea is
   * programmatically focused, so we need to focus, blur, and refocus the
   * RichTextArea. This only needs to be done the first time after the
   * RichTextArea is initialized. See issue 3503.
   */
  protected native void setFirstFocusImpl() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var wnd = elem.contentWindow;

    // Remove event listeners so we don't generate extra focus and blur events.
    wnd.removeEventListener('focus', elem.__gwt_focusHandler, true);
    wnd.removeEventListener('blur', elem.__gwt_blurHandler, true);
    wnd.focus();
    wnd.blur();
    wnd.focus();

    // Add the event listeners now that we have focus and a caret.
    wnd.addEventListener('focus', elem.__gwt_focusHandler, true);
    wnd.addEventListener('blur', elem.__gwt_blurHandler, true);

    // Fire a synthetic focus event. We can't move the last call to wnd.focus()
    // here because firefox will not fire the focus event reliably.
    var evt = document.createEvent('HTMLEvents');
    evt.initEvent('focus', false, false);
    wnd.dispatchEvent(evt);
  }-*/;

  @Override
  protected void setFocusImpl(boolean focused) {
    if (isFirstFocus) {
      isFirstFocus = false;
      setFirstFocusImpl();
    } else {
      super.setFocusImpl(focused);
    }
  }
}
