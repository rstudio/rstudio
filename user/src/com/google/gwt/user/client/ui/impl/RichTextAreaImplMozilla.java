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

  @Override
  public native void initElement() /*-{
    // Mozilla doesn't allow designMode to be set reliably until the iframe is
    // fully loaded.
    var _this = this;
    var iframe = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;

    iframe.onload = $entry(function() {
      // Some Mozillae have the nasty habit of calling onload again when you set
      // designMode, so let's avoid doing it more than once.
      iframe.onload = null;

      // Send notification that the iframe has finished loading.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();

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
      iframe.contentWindow.onmouseover = function() {
        iframe.contentWindow.onfocus = null;
        iframe.contentWindow.onmouseover = null;
        iframe.contentWindow.document.designMode = 'On';
      };
    });
  }-*/;

  @Override
  public void setBackColor(String color) {
    // Gecko uses 'BackColor' for the *entire area's* background. 'HiliteColor'
    // does what we actually want.
    execCommand("HiliteColor", color);
  }
}
