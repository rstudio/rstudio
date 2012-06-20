/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * StandardBase implementation of {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplStandardBase extends DOMImplStandard {

  private static class ClientRect extends JavaScriptObject {
    
    protected ClientRect() {
    }

    public final native int getLeft() /*-{
      return this.left;
    }-*/;

    public final native int getTop() /*-{
      return this.top;
    }-*/;
  }

  private static native int getAbsoluteLeftUsingOffsets(Element elem) /*-{
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetLeft.
    if (elem.offsetLeft == null) {
      return 0;
    }

    var left = 0;
    var doc = elem.ownerDocument;
    var curr = elem.parentNode;
    if (curr) {
      // This intentionally excludes body which has a null offsetParent.
      while (curr.offsetParent) {
        left -= curr.scrollLeft;

        // In RTL mode, offsetLeft is relative to the left edge of the
        // scrollable area when scrolled all the way to the right, so we need
        // to add back that difference.
        if (doc.defaultView.getComputedStyle(curr, '').getPropertyValue('direction') == 'rtl') {
          left += (curr.scrollWidth - curr.clientWidth);
        }

        curr = curr.parentNode;
      }
    }

    while (elem) {
      left += elem.offsetLeft;

      if (doc.defaultView.getComputedStyle(elem, '')['position'] == 'fixed') {
        left += doc.body.scrollLeft;
        return left;
      }

      // Safari 3 does not include borders with offsetLeft, so we need to add
      // the borders of the parent manually.
      var parent = elem.offsetParent;
      if (parent && $wnd.devicePixelRatio) {
        left += parseInt(doc.defaultView.getComputedStyle(parent, '').getPropertyValue('border-left-width'));
      }

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return left;
  }-*/;

  private static native int getAbsoluteTopUsingOffsets(Element elem) /*-{
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetTop.
    if (elem.offsetTop == null) {
      return 0;
    }

    var top = 0;
    var doc = elem.ownerDocument;
    var curr = elem.parentNode;
    if (curr) {
      // This intentionally excludes body which has a null offsetParent.
      while (curr.offsetParent) {
        top -= curr.scrollTop;
        curr = curr.parentNode;
      }
    }

    while (elem) {
      top += elem.offsetTop;

      if (doc.defaultView.getComputedStyle(elem, '')['position'] == 'fixed') {
        top += doc.body.scrollTop;
        return top;
      }

      // Safari 3 does not include borders with offsetTop, so we need to add the
      // borders of the parent manually.
      var parent = elem.offsetParent;
      if (parent && $wnd.devicePixelRatio) {
        top += parseInt(doc.defaultView.getComputedStyle(parent, '').getPropertyValue('border-top-width'));
      }

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return top;
  }-*/;

  private static native ClientRect getBoundingClientRect(Element element) /*-{
    return element.getBoundingClientRect && element.getBoundingClientRect();
  }-*/;

  /**
   * The type property on a button element is read-only in safari, so we need to
   * set it using setAttribute.
   */
  @Override
  public native ButtonElement createButtonElement(Document doc, String type) /*-{
    var e = doc.createElement("BUTTON");
    e.setAttribute('type', type);
    return e;
  }-*/;

  @Override
  public native NativeEvent createKeyCodeEvent(Document doc, String type,
      boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
      int keyCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplStandardBase::createKeyEvent(Lcom/google/gwt/dom/client/Document;Ljava/lang/String;ZZZZZZ)(doc, type, true, true, ctrlKey, altKey, shiftKey, metaKey)
    evt.keyCode = keyCode;
    return evt;
  }-*/;

  @Override
  @Deprecated
  public native NativeEvent createKeyEvent(Document doc, String type,
      boolean canBubble, boolean cancelable, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode, int charCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplStandardBase::createKeyEvent(Lcom/google/gwt/dom/client/Document;Ljava/lang/String;ZZZZZZ)(doc, type, canBubble, cancelable, ctrlKey, altKey, shiftKey, metaKey)
    evt.keyCode = keyCode;
    evt.charCode = charCode;
    return evt;
  }-*/;

  @Override
  public native NativeEvent createKeyPressEvent(Document doc,
      boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
      int charCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplStandardBase::createKeyEvent(Lcom/google/gwt/dom/client/Document;Ljava/lang/String;ZZZZZZ)(doc, 'keypress', true, true, ctrlKey, altKey, shiftKey, metaKey)
    evt.charCode = charCode;
    return evt;
  }-*/;

  /**
   * Safari 2 does not support {@link ScriptElement#setText(String)}.
   */
  @Override
  public ScriptElement createScriptElement(Document doc, String source) {
    ScriptElement elem = (ScriptElement) createElement(doc, "script");
    elem.setInnerText(source);
    return elem;
  }

  @Override
  public native EventTarget eventGetCurrentTarget(NativeEvent event) /*-{
    return event.currentTarget || $wnd;
  }-*/;

  @Override
  public native int eventGetMouseWheelVelocityY(NativeEvent evt) /*-{
    return Math.round(-evt.wheelDelta / 40) || 0;
  }-*/;

  @Override
  public int getAbsoluteLeft(Element elem) {
    ClientRect rect = getBoundingClientRect(elem);
    return rect != null ? rect.getLeft()
        + elem.getOwnerDocument().getBody().getScrollLeft()
        : getAbsoluteLeftUsingOffsets(elem);
  }

  @Override
  public int getAbsoluteTop(Element elem) {
    ClientRect rect = getBoundingClientRect(elem);
    return rect != null ? rect.getTop()
        + elem.getOwnerDocument().getBody().getScrollTop()
        : getAbsoluteTopUsingOffsets(elem);
  }

  @Override
  public int getScrollLeft(Document doc) {
    // Safari always applies document scrolling to the body element, even in
    // strict mode.
    return doc.getBody().getScrollLeft();
  }

  @Override
  public int getScrollLeft(Element elem) {
    if (isRTL(elem)) {
      return super.getScrollLeft(elem)
          - (elem.getScrollWidth() - elem.getClientWidth());
    }
    return super.getScrollLeft(elem);
  }

  @Override
  public int getScrollTop(Document doc) {
    // Safari always applies document scrolling to the body element, even in
    // strict mode.
    return doc.getBody().getScrollTop();
  }

  @Override
  public native int getTabIndex(Element elem) /*-{ 
    // tabIndex is undefined for divs and other non-focusable elements prior to
    // Safari 4.
    return typeof elem.tabIndex != 'undefined' ? elem.tabIndex : -1;
  }-*/;

  @Override
  public void setScrollLeft(Document doc, int left) {
    // Safari always applies document scrolling to the body element, even in
    // strict mode.
    doc.getBody().setScrollLeft(left);
  }

  @Override
  public void setScrollLeft(Element elem, int left) {
    if (isRTL(elem)) {
      left += elem.getScrollWidth() - elem.getClientWidth();
    }
    super.setScrollLeft(elem, left);
  }

  @Override
  public void setScrollTop(Document doc, int top) {
    // Safari always applies document scrolling to the body element, even in
    // strict mode.
    doc.getBody().setScrollTop(top);
  }

  protected native boolean isRTL(Element elem) /*-{
    return elem.ownerDocument.defaultView.getComputedStyle(elem, '').direction == 'rtl';
  }-*/;

  private native NativeEvent createKeyEvent(Document doc, String type,
      boolean canBubble, boolean cancelable, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey) /*-{
    // WebKit's KeyboardEvent cannot set or even initialize charCode, keyCode, etc.
    // And UIEvent's charCode and keyCode are read-only.
    // So we "fake" an event using a raw Event and expandos
    var evt = doc.createEvent('Event');
    evt.initEvent(type, canBubble, cancelable);
    evt.ctrlKey = ctrlKey;
    evt.altKey = altKey;
    evt.shiftKey = shiftKey;
    evt.metaKey = metaKey;
    return evt;
  }-*/;
}

