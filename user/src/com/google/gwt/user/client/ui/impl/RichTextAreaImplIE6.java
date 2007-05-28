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

  private static native void detachEvents(Element elem) /*-{
    var body = elem.contentWindow.document.body;
    body.onkeydown =
    body.onkeyup =
    body.onkeypress =
    body.onmousedown =
    body.onmouseup =
    body.onmousemove =
    body.onmouseover =
    body.onmouseout =
    body.onclick = null;
  }-*/;

  private static native String getText(Element elem) /*-{
    return elem.contentWindow.document.body.innerText;
  }-*/;

  private static native void initEvents(Element elem) /*-{
    var handler = function() {
      if (elem.__listener) {
        // Weird: this code has the context of the script frame, but we need the
        // event from the edit iframe's window.
        var evt = elem.contentWindow.event;
        elem.__listener.@com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

    var body = elem.contentWindow.document.body;
    body.onkeydown =
    body.onkeyup =
    body.onkeypress =
    body.onmousedown =
    body.onmouseup =
    body.onmousemove =
    body.onmouseover =
    body.onmouseout =
    body.onclick = handler;
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

      // Initialize event handling.
      @com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6::initEvents(Lcom/google/gwt/user/client/Element;)(elem);

      // Send notification that the iframe has reached design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    }, 1);
  }-*/;

  public void unhookEvents() {
    super.unhookEvents();
    detachEvents(elem);
  }

  protected String getTextImpl() {
    return getText(elem);
  }

  protected void setTextImpl(String text) {
    setText(elem, text);
  }
  
  boolean isRichEditingActive(Element elem) {
    return true;
  }
}
