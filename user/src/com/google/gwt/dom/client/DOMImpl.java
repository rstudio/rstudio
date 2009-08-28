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

import com.google.gwt.core.client.GWT;

abstract class DOMImpl {

  static final DOMImpl impl = GWT.create(DOMImpl.class);

  public native void buttonClick(ButtonElement button) /*-{
    button.click();
  }-*/;

  public native Element createElement(Document doc, String tag) /*-{
    return doc.createElement(tag);
  }-*/;

  public abstract NativeEvent createHtmlEvent(Document doc, String type, boolean canBubble,
      boolean cancelable);

  public native InputElement createInputElement(Document doc, String type) /*-{
    var e = doc.createElement("INPUT");
    e.type = type;
    return e;
  }-*/;

  public abstract InputElement createInputRadioElement(Document doc, String name);

  public abstract NativeEvent createKeyEvent(Document doc, String type,
      boolean canBubble, boolean cancelable, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode, int charCode);

  public abstract NativeEvent createMouseEvent(Document doc, String type,
      boolean canBubble, boolean cancelable, int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int button, Element relatedTarget);

  public ScriptElement createScriptElement(Document doc, String source) {
    ScriptElement elem = (ScriptElement) createElement(doc, "script");
    elem.setText(source);
    return elem;
  }

  public SelectElement createSelectElement(Document doc, boolean multiple) {
    SelectElement select = (SelectElement) createElement(doc, "select");
    if (multiple) {
      select.setMultiple(true);
    }
    return select;
  }

  public abstract void dispatchEvent(Element target, NativeEvent evt);

  public native boolean eventGetAltKey(NativeEvent evt) /*-{
    return !!evt.altKey;
  }-*/;

  public native int eventGetButton(NativeEvent evt) /*-{
    return evt.button || 0;
  }-*/;

  public native int eventGetClientX(NativeEvent evt) /*-{
    return evt.clientX || 0;
  }-*/;

  public native int eventGetClientY(NativeEvent evt) /*-{
    return evt.clientY || 0;
  }-*/;

  public native boolean eventGetCtrlKey(NativeEvent evt) /*-{
    return !!evt.ctrlKey;
  }-*/;

  public native EventTarget eventGetCurrentTarget(NativeEvent event) /*-{
    return event.currentTarget;
  }-*/;

  public final native int eventGetKeyCode(NativeEvent evt) /*-{
    // 'which' gives the right key value, except when it doesn't -- in which
    // case, keyCode gives the right value on all browsers.
    // If all else fails, return an error code
    return evt.which || evt.keyCode || 0;
  }-*/;

  public native boolean eventGetMetaKey(NativeEvent evt) /*-{
    return !!evt.metaKey;
  }-*/;

  public abstract int eventGetMouseWheelVelocityY(NativeEvent evt);

  public abstract EventTarget eventGetRelatedTarget(NativeEvent nativeEvent);

  public native int eventGetScreenX(NativeEvent evt) /*-{
    return evt.screenX || 0;
  }-*/;

  public native int eventGetScreenY(NativeEvent evt) /*-{
    return evt.screenY || 0;
  }-*/;

  public native boolean eventGetShiftKey(NativeEvent evt) /*-{
    return !!evt.shiftKey;
  }-*/;

  public abstract EventTarget eventGetTarget(NativeEvent evt);

  public final native String eventGetType(NativeEvent evt) /*-{
    return evt.type;
  }-*/;

  public abstract void eventPreventDefault(NativeEvent evt);

  public native void eventSetKeyCode(NativeEvent evt, char key) /*-{
    evt.keyCode = key;
  }-*/;

  public native void eventStopPropagation(NativeEvent evt) /*-{
    evt.stopPropagation();
  }-*/;

  public abstract String eventToString(NativeEvent evt);

  public native int getAbsoluteLeft(Element elem) /*-{
    var left = 0;
    var curr = elem;
    // This intentionally excludes body which has a null offsetParent.    
    while (curr.offsetParent) {
      left -= curr.scrollLeft;
      curr = curr.parentNode;
    }
    while (elem) {
      left += elem.offsetLeft;
      elem = elem.offsetParent;
    }
    return left;
  }-*/;

  public native int getAbsoluteTop(Element elem) /*-{
    var top = 0;
    var curr = elem;
    // This intentionally excludes body which has a null offsetParent.    
    while (curr.offsetParent) {
      top -= curr.scrollTop;
      curr = curr.parentNode;
    }
    while (elem) {
      top += elem.offsetTop;
      elem = elem.offsetParent;
    }
    return top;
  }-*/;

  public native String getAttribute(Element elem, String name) /*-{
    return elem.getAttribute(name) || '';
  }-*/;

  public native int getBodyOffsetLeft(Document doc) /*-{
    return 0;
  }-*/;

  public native int getBodyOffsetTop(Document doc) /*-{
    return 0;
  }-*/;

  public native Element getFirstChildElement(Element elem) /*-{
    var child = elem.firstChild;
    while (child && child.nodeType != 1)
      child = child.nextSibling;
    return child;
  }-*/;

  public native String getInnerHTML(Element elem) /*-{
    return elem.innerHTML;
  }-*/;

  public native String getInnerText(Element node) /*-{
    // To mimic IE's 'innerText' property in the W3C DOM, we need to recursively
    // concatenate all child text nodes (depth first).
    var text = '', child = node.firstChild;
    while (child) {
      // 1 == Element node
      if (child.nodeType == 1) {
        text += this.@com.google.gwt.dom.client.DOMImpl::getInnerText(Lcom/google/gwt/dom/client/Element;)(child);
      } else if (child.nodeValue) {
        text += child.nodeValue;
      }
      child = child.nextSibling;
    }
    return text;
  }-*/;

  public native Element getNextSiblingElement(Element elem) /*-{
    var sib = elem.nextSibling;
    while (sib && sib.nodeType != 1)
      sib = sib.nextSibling;
    return sib;
  }-*/;

  public native int getNodeType(Node node) /*-{
    return node.nodeType;
  }-*/;

  public native Element getParentElement(Node node) /*-{
    var parent = node.parentNode;
    if (!parent || parent.nodeType != 1) {
      parent = null;
    }
    return parent;
  }-*/;

  public int getScrollLeft(Document doc) {
    return doc.getViewportElement().getScrollLeft();
  }

  public native int getScrollLeft(Element elem) /*-{
    return elem.scrollLeft || 0;
  }-*/;

  public int getScrollTop(Document doc) {
    return doc.getViewportElement().getScrollTop();
  }

  public native String getTagName(Element elem) /*-{
    return elem.tagName;
  }-*/;

  public native boolean hasAttribute(Element elem, String name) /*-{
    return elem.hasAttribute(name);
  }-*/;

  public native String imgGetSrc(Element img) /*-{
    return img.src;
  }-*/;

  public native void imgSetSrc(Element img, String src) /*-{
    img.src = src;
  }-*/;

  public abstract boolean isOrHasChild(Node parent, Node child);

  public native void scrollIntoView(Element elem) /*-{
    var left = elem.offsetLeft, top = elem.offsetTop;
    var width = elem.offsetWidth, height = elem.offsetHeight;

    if (elem.parentNode != elem.offsetParent) {
      left -= elem.parentNode.offsetLeft;
      top -= elem.parentNode.offsetTop;
    }

    var cur = elem.parentNode;
    while (cur && (cur.nodeType == 1)) {
      if (left < cur.scrollLeft) {
        cur.scrollLeft = left;
      }
      if (left + width > cur.scrollLeft + cur.clientWidth) {
        cur.scrollLeft = (left + width) - cur.clientWidth;
      }
      if (top < cur.scrollTop) {
        cur.scrollTop = top;
      }
      if (top + height > cur.scrollTop + cur.clientHeight) {
        cur.scrollTop = (top + height) - cur.clientHeight;
      }

      var offsetLeft = cur.offsetLeft, offsetTop = cur.offsetTop;
      if (cur.parentNode != cur.offsetParent) {
        offsetLeft -= cur.parentNode.offsetLeft;
        offsetTop -= cur.parentNode.offsetTop;
      }

      left += offsetLeft - cur.scrollLeft;
      top += offsetTop - cur.scrollTop;
      cur = cur.parentNode;
    }
  }-*/;

  public native void selectAdd(SelectElement select, OptionElement option,
      OptionElement before) /*-{
    select.add(option, before);
  }-*/;

  public native void selectClear(SelectElement select) /*-{
    select.options.length = 0;
  }-*/;

  public native int selectGetLength(SelectElement select) /*-{
    return select.options.length;
  }-*/;

  public native NodeList<OptionElement> selectGetOptions(SelectElement select) /*-{
    return select.options;
  }-*/;

  public native void selectRemoveOption(SelectElement select, int index) /*-{
    select.remove(index);
  }-*/;

  public native void setInnerText(Element elem, String text) /*-{
    // Remove all children first.
    while (elem.firstChild) {
      elem.removeChild(elem.firstChild);
    }
    // Add a new text node.
    if (text != null) {
      elem.appendChild(elem.ownerDocument.createTextNode(text));
    }
  }-*/;

  public void setScrollLeft(Document doc, int left) {
    doc.getViewportElement().setScrollLeft(left);
  }

  public native void setScrollLeft(Element elem, int left) /*-{
    elem.scrollLeft = left;
  }-*/;

  public void setScrollTop(Document doc, int top) {
    doc.getViewportElement().setScrollTop(top);
  }

  public native String toString(Element elem) /*-{
    return elem.outerHTML;
  }-*/;
}
