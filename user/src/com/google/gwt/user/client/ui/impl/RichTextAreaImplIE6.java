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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * IE6-specific implementation of rich-text editing.
 */
public class RichTextAreaImplIE6 extends RichTextAreaImplStandard {

  public Element createElement() {
    Element elem = super.createElement();
    DOM.setElementProperty(elem, "src", "javascript:''");
    return elem;
  }
  
  public native String getText() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerText;
  }-*/;

  public native void initElement() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var _this = this;
    elem.onload = function() {
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6::initEvents()();
    };
    elem.src = "RichTextIE.html";
  }-*/;

  native void initEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var handler = function(evt) {
      if (elem.__listener) {
        elem.__listener.
          @com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

    var body = elem.contentWindow.document.body;
    body.attachEvent('onkeydown', handler);
    body.attachEvent('onkeyup', handler);
    body.attachEvent('onkeypress', handler);
    body.attachEvent('onmousedown', handler);
    body.attachEvent('onmouseup', handler);
    body.attachEvent('onmousemove', handler);
    body.attachEvent('onmouseover', handler);
    body.attachEvent('onmouseout', handler);
    body.attachEvent('onclick', handler);
  }-*/;

  boolean isRichEditingActive(Element e) {
    return true;
  }
}
