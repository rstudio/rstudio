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

  public native Element createElement(String tag) /*-{
    return $doc.createElement(tag);
  }-*/;

  public native InputElement createInputElement(String type) /*-{
    var e = $doc.createElement("INPUT");
    e.type = type;
    return e;
  }-*/;

  public abstract InputElement createInputRadioElement(String name);

  public SelectElement createSelectElement(boolean multiple) {
    SelectElement select = (SelectElement) createElement("select");
    if (multiple) {
      select.setMultiple(true);
    }
    return select;
  }

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

  public native int getBodyOffsetLeft() /*-{
    return 0;
  }-*/;

  public native int getBodyOffsetTop() /*-{
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

  public native Element getParentElement(Element elem) /*-{
    var parent = elem.parentNode;
    if (parent == null) {
      return null;
    }
    if (parent.nodeType != 1)
      parent = null;
    return parent;
  }-*/;

  public native String imgGetSrc(Element img) /*-{
    return img.src;
  }-*/;

  public native void imgSetSrc(Element img, String src) /*-{
    img.src = src;
  }-*/;

  public abstract boolean isOrHasChild(Element parent, Element child);

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
      elem.appendChild($doc.createTextNode(text));
    }
  }-*/;

  public native String toString(Element elem) /*-{
    return elem.outerHTML;
  }-*/;
}
