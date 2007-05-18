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
import com.google.gwt.user.client.ui.RichTextArea;

/**
 * IE6-specific implementation of rich-text editing.
 */
public class RichTextAreaImplIE6 extends RichTextAreaImplStandard {

  private static native void attachEvents(Element elem) /*-{
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

  private static native void writeHtml(Element elem) /*-{
    var doc = elem.contentWindow.document;
    doc.open();
    doc.write('<html><body CONTENTEDITABLE="true"></body></html>');
    doc.close();
  }-*/;

  public Element createElement() {
    Element elem = super.createElement();
    DOM.setElementProperty(elem, "src", "javascript:''");
    return elem;
  }

  public String getText() {
    return getText(elem);
  }

  public void hookEvents(RichTextArea owner) {
    attachEvents(elem);
    super.hookEvents(owner);
  }

  public void initElement() {
    writeHtml(elem);
  }

  public void unhookEvents() {
    super.unhookEvents();
    detachEvents(elem);
  }

  boolean isRichEditingActive(Element elem) {
    return true;
  }
}
