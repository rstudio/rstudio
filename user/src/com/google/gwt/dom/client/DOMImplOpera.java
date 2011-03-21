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
package com.google.gwt.dom.client;

/**
 * Opera implementation of {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplOpera extends DOMImplStandard {

  @Override
  public native NativeEvent createKeyCodeEvent(Document doc, String type,
      boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
      int keyCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplOpera::createKeyEvent(Lcom/google/gwt/dom/client/Document;Ljava/lang/String;ZZZZZZ)(doc, type, true, true, ctrlKey, altKey, shiftKey, metaKey);
    evt.keyCode = keyCode;
    return evt;
  }-*/;

  @Override
  @Deprecated
  public native NativeEvent createKeyEvent(Document doc, String type,
      boolean canBubble, boolean cancelable, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode, int charCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplOpera::createKeyEvent(Lcom/google/gwt/dom/client/Document;Ljava/lang/String;ZZZZZZ)(doc, type, canBubble, cancelable, ctrlKey, altKey, shiftKey, metaKey);
    evt.keyCode = keyCode;
    evt.which = charCode;
    return evt;
  }-*/;

  @Override
  public native NativeEvent createKeyPressEvent(Document doc,
      boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
      int charCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplOpera::createKeyEvent(Lcom/google/gwt/dom/client/Document;Ljava/lang/String;ZZZZZZ)(doc, 'keypress', true, true, ctrlKey, altKey, shiftKey, metaKey);
    evt.which = charCode;
    return evt;
  }-*/;

  @Override
  public native int eventGetCharCode(NativeEvent evt) /*-{
    return evt.which || 0;
  }-*/;

  @Override
  public native int eventGetMouseWheelVelocityY(NativeEvent evt) /*-{
    return evt.detail * 4 || 0;
  }-*/;

  @Override
  public native int getAbsoluteLeft(Element elem) /*-{
    var left = 0;

    // This intentionally excludes body and its ancestors
    var curr = elem.parentNode;
    while (curr && curr.offsetParent) {
      // see https://bugs.opera.com/show_bug.cgi?id=249965
      // The net effect is that TR and TBODY elemnts report the scroll offsets
      // of the BODY and HTML elements instead of 0.
      if (curr.tagName != 'TR' && curr.tagName != 'TBODY') {
        left -= curr.scrollLeft;
      }
      curr = curr.parentNode;
    }

    while (elem) {
      left += elem.offsetLeft;
      elem = elem.offsetParent;
    }
    return left;
  }-*/;

  @Override
  public native int getAbsoluteTop(Element elem) /*-{
    var top = 0;

    // This intentionally excludes body and its ancestors
    var curr = elem.parentNode;
    while (curr && curr.offsetParent) {
      // see getAbsoluteLeft()
      if (curr.tagName != 'TR' && curr.tagName != 'TBODY') {
        top -= curr.scrollTop;
      }
      curr = curr.parentNode;
    }

    while (elem) {
      top += elem.offsetTop;
      elem = elem.offsetParent;
    }
    return top;
  }-*/;

  @Override
  public native void scrollIntoView(Element elem) /*-{
    elem.scrollIntoView();
  }-*/;

  private native NativeEvent createKeyEvent(Document doc, String type,
      boolean canBubble, boolean cancelable, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey) /*-{
    // Opera fires KeyEvent instances but does not allow creating them.
    // The best approximation is UIEvent here.
    var evt = doc.createEvent('UIEvent');
    evt.initUIEvent(type, canBubble, cancelable, null, 0);
    evt.ctrlKey = ctrlKey;
    evt.altKey = altKey;
    evt.shiftKey = shiftKey;
    evt.metaKey = metaKey;
    return evt;
  }-*/;
}
