/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * Native implementation associated with {@link com.google.gwt.user.client.DOM}.
 */
public abstract class DOMImpl {

  public native void appendChild(Element parent, Element child) /*-{
    parent.appendChild(child);
  }-*/;

  public abstract boolean compare(Element elem1, Element elem2);

  public native Element createElement(String tag) /*-{
    return $doc.createElement(tag);
  }-*/;

  public native Element createInputElement(String type) /*-{
    var e = $doc.createElement("INPUT");
    e.type = type;
    return e;
  }-*/;

  public abstract Element createInputRadioElement(String group);

  public native void eventCancelBubble(Event evt, boolean cancel) /*-{
    evt.cancelBubble = cancel;
  }-*/;

  public native boolean eventGetAltKey(Event evt) /*-{
    return evt.altKey;
  }-*/;

  public native int eventGetButton(Event evt) /*-{
    return evt.button;
  }-*/;

  public native int eventGetClientX(Event evt) /*-{
    return evt.clientX;
  }-*/;

  public native int eventGetClientY(Event evt) /*-{
    return evt.clientY;
  }-*/;

  public native boolean eventGetCtrlKey(Event evt) /*-{
    return evt.ctrlKey;
  }-*/;

  public native Element eventGetFromElement(Event evt) /*-{
    return evt.fromElement ? evt.fromElement : null;
  }-*/;

  public native int eventGetKeyCode(Event evt) /*-{
    // 'which' gives the right key value, except when it doesn't -- in which
    // case, keyCode gives the right value on all browsers.
    return evt.which ? evt.which : evt.keyCode;
  }-*/;

  public native boolean eventGetRepeat(Event evt) /*-{
    return evt.repeat;
  }-*/;

  public native int eventGetScreenX(Event evt) /*-{
    return evt.screenX;
  }-*/;

  public native int eventGetScreenY(Event evt) /*-{
    return evt.screenY;
  }-*/;

  public native boolean eventGetShiftKey(Event evt) /*-{
    return evt.shiftKey;
  }-*/;

  public abstract Element eventGetTarget(Event evt);

  public abstract Element eventGetToElement(Event evt);

  public native String eventGetType(Event evt) /*-{
    return evt.type;
  }-*/;

  public native int eventGetTypeInt(Event evt) /*-{
    switch (evt.type) {
      case "blur": return 0x01000;
      case "change": return 0x00400;
      case "click": return 0x00001;
      case "dblclick": return 0x00002;
      case "focus": return 0x00800;
      case "keydown": return 0x00080;
      case "keypress": return 0x00100;
      case "keyup": return 0x00200;
      case "load": return 0x08000;
      case "losecapture": return 0x02000;
      case "mousedown": return 0x00004;
      case "mousemove": return 0x00040;
      case "mouseout": return 0x00020;
      case "mouseover": return 0x00010;
      case "mouseup": return 0x00008;
      case "scroll": return 0x04000;
      case "error": return 0x10000;
    }
  }-*/;

  public abstract void eventPreventDefault(Event evt);

  public native void eventSetKeyCode(Event evt, char key) /*-{
    evt.keyCode = key;
  }-*/;

  public abstract String eventToString(Event evt);

  public native int getAbsoluteLeft(Element elem) /*-{
    var left = 0;
    while (elem) {
      left += elem.offsetLeft - elem.scrollLeft;
      elem = elem.offsetParent;
    }
    return left + $doc.body.scrollLeft;
  }-*/;
  
  public native int getAbsoluteTop(Element elem) /*-{
    var top = 0;
    while (elem) {
      top += elem.offsetTop - elem.scrollTop;
      elem = elem.offsetParent;
    }
    return top + $doc.body.scrollTop;
  }-*/;

  public native String getAttribute(Element elem, String attr) /*-{
    var ret = elem[attr];
    return (ret == null) ? null : String(ret);
  }-*/;

  public native boolean getBooleanAttribute(Element elem, String attr) /*-{
    return !!elem[attr];
  }-*/;

  public abstract Element getChild(Element elem, int index);

  public abstract int getChildCount(Element elem);

  public abstract int getChildIndex(Element parent, Element child);

  public native Element getElementById(String id) /*-{
    var elem = $doc.getElementById(id);
    return elem ? elem : null;
  }-*/;

  public native int getEventsSunk(Element elem) /*-{
    return elem.__eventBits ? elem.__eventBits : 0;
  }-*/;

  public abstract Element getFirstChild(Element elem);

  public native String getInnerHTML(Element elem) /*-{
    var ret = elem.innerHTML;
    return (ret == null) ? null : ret;
  }-*/;

  public native String getInnerText(Element node) /*-{
    // To mimic IE's 'innerText' property in the W3C DOM, we need to recursively
    // concatenate all child text nodes (depth first).
    var text = '', child = node.firstChild;
    while (child) {
      // 1 == Element node
      if (child.nodeType == 1) { 
        text += this.@com.google.gwt.user.client.impl.DOMImpl::getInnerText(Lcom/google/gwt/user/client/Element;)(child);
      } else if (child.nodeValue) {
        text += child.nodeValue;
      }
      child = child.nextSibling;
    }
    return text;
  }-*/;

  public native int getIntAttribute(Element elem, String attr) /*-{
    var i = parseInt(elem[attr]);
    if (!i) {
      return 0;
    }
    return i;
  }-*/;

  public native int getIntStyleAttribute(Element elem, String attr) /*-{
    var i = parseInt(elem.style[attr]);
    if (!i) {
      return 0;
    }
    return i;
  }-*/;

  public abstract Element getNextSibling(Element elem);

  public abstract Element getParent(Element elem);

  public native String getStyleAttribute(Element elem, String attr) /*-{
    var ret = elem.style[attr];
    return (ret == null) ? null : ret;
  }-*/;

  public abstract void init();

  public abstract void insertChild(Element parent, Element child,
      int index);

  public void insertListItem(Element select, String item, String value,
      int index) {
    Element option = DOM.createElement("OPTION");
    DOM.setInnerText(option, item);
    if (value != null) {
      DOM.setAttribute(option, "value", value);
    }
    if (index == -1) {
      DOM.appendChild(select, option);
    } else {
      DOM.insertChild(select, option, index);
    }
  }

  public abstract boolean isOrHasChild(Element parent, Element child);

  public abstract void releaseCapture(Element elem);

  public native void removeChild(Element parent, Element child) /*-{
    parent.removeChild(child);
  }-*/;

  public native void scrollIntoView(Element elem) /*-{
    var left = elem.offsetLeft, top = elem.offsetTop;
    var width = elem.offsetWidth, height = elem.offsetHeight;

    if (elem.parentNode != elem.offsetParent) {
      left -= elem.parentNode.offsetLeft;
      top -= elem.parentNode.offsetTop;
    }

    var cur = elem.parentNode;
    while (cur && (cur.nodeType == 1)) {
      if ((cur.style.overflow == 'auto') || (cur.style.overflow == 'scroll')) {
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

  public native void setAttribute(Element elem, String attr, String value) /*-{
    elem[attr] = value;
  }-*/;

  public native void setBooleanAttribute(Element elem, String attr,
      boolean value) /*-{
    elem[attr] = value;
  }-*/;

  public abstract void setCapture(Element elem);

  public native void setEventListener(Element elem,
      EventListener listener) /*-{
    elem.__listener = listener;
  }-*/;

  public native void setInnerHTML(Element elem, String html) /*-{
    if (!html) {
      html = '';
    }
    elem.innerHTML = html;
  }-*/;

  public native void setInnerText(Element elem, String text) /*-{
    // Remove all children first.
    while (elem.firstChild) {
      elem.removeChild(elem.firstChild);
    }
    // Add a new text node.
    elem.appendChild($doc.createTextNode(text));
  }-*/;

  public native void setIntAttribute(Element elem, String attr, int value) /*-{
    elem[attr] = value;
  }-*/;

  public native void setIntStyleAttribute(Element elem, String attr, int value) /*-{
    elem.style[attr] = value;
  }-*/;

  public native void setOptionText(Element select, String text, int index) /*-{
    // IE doesn't properly update the screen when you use 
    // setAttribute("option", text), so we instead directly assign to the 
    // 'option' property, which works correctly on all browsers.
    var option = select.options[index];
    option.text = text;
  }-*/;

  public native void setStyleAttribute(Element elem, String attr,
      String value) /*-{
    elem.style[attr] = value;
  }-*/;

  public abstract void sinkEvents(Element elem, int eventBits);

  public abstract String toString(Element elem);
}
