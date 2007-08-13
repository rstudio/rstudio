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

  private static native String getText(Element elem) /*-{
    return elem.contentWindow.document.body.innerText;
  }-*/;

  private static native void setText(Element elem, String text) /*-{
    elem.contentWindow.document.body.innerText = text;
  }-*/;

  public Element createElement() {
    Element elem = super.createElement();
    DOM.setElementProperty(elem, "src", "javascript:''");
    return elem;
  }

  public native void initElement() /*-{
    var _this = this;
    window.setTimeout(function() {
      var elem = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
      var doc = elem.contentWindow.document;
      doc.write('<html><body CONTENTEDITABLE="true"></body></html>');

      // Send notification that the iframe has reached design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    }, 1);
  }-*/;

  protected String getTextImpl() {
    return getText(elem);
  }

  protected native void hookEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var body = elem.contentWindow.document.body;

    var handler = function() {
      if (elem.__listener) {
        // Weird: this code has the context of the script frame, but we need the
        // event from the edit iframe's window.
        var evt = elem.contentWindow.event;
        elem.__listener.@com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

    body.onkeydown =
    body.onkeyup =
    body.onkeypress =
    body.onmousedown =
    body.onmouseup =
    body.onmousemove =
    body.onmouseover =
    body.onmouseout =
    body.onclick = handler;
    
    elem.contentWindow.onfocus =
    elem.contentWindow.onblur = handler;
  }-*/;

  protected void setTextImpl(String text) {
    setText(elem, text);
  }

  protected native void unhookEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var body = elem.contentWindow.document.body;

    if (body) {
      // The body can be undefined in the relatively obscure case that the RTA
      // is attached and detached before it has a chance to finish initializing.
      body.onkeydown =
      body.onkeyup =
      body.onkeypress =
      body.onmousedown =
      body.onmouseup =
      body.onmousemove =
      body.onmouseover =
      body.onmouseout =
      body.onclick = null;

      elem.contentWindow.onfocus =
      elem.contentWindow.onblur = null;
    }
  }-*/;
  
  boolean isRichEditingActive(Element elem) {
    return true;
  }
}
