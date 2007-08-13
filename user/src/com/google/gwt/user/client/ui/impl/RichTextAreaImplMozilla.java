/*
 * Copyright 2007 Google Inc.
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

  public native void initElement() /*-{
    // Mozilla doesn't allow designMode to be set reliably until the iframe is
    // fully loaded.
    var _this = this;
    var iframe = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;

    iframe.onload = function() {
      // Some Mozillae have the nasty habit of calling onload again when you set
      // designMode, so let's avoid doing it more than once.
      iframe.onload = null;

      // Turn on design mode.
      iframe.contentWindow.document.designMode = 'On';

      // Send notification that the iframe has reached design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    };
  }-*/;

  public void setBackColor(String color) {
    // Gecko uses 'BackColor' for the *entire area's* background. 'HiliteColor'
    // does what we actually want.
    execCommand("HiliteColor", color);
  }
}
