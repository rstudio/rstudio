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
 * DOM implementation differences for older version of Mozilla (mostly the
 * hosted mode browser on linux). The main difference is due to changes in
 * getBoxObjectFor in later versions of mozilla. The relevant bugzilla issues:
 * https://bugzilla.mozilla.org/show_bug.cgi?id=328881
 * https://bugzilla.mozilla.org/show_bug.cgi?id=330619
 */
 class DOMImplMozillaOld extends DOMImplMozilla {

  @Override
  public native int getAbsoluteLeft(Element elem) /*-{
    var style = $doc.defaultView.getComputedStyle(elem, null);
    var left = $doc.getBoxObjectFor(elem).x - Math.round(
        style.getPropertyCSSValue('border-left-width').getFloatValue(
        CSSPrimitiveValue.CSS_PX));
        
    var parent = elem.parentNode;
    while (parent) {
      // Sometimes get NAN.
      if (parent.scrollLeft > 0) {
        left -= parent.scrollLeft;
      }
      parent = parent.parentNode;
    }

    return left +
      @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollLeft;
  }-*/;

  @Override
  public native int getAbsoluteTop(Element elem) /*-{
    var style = $doc.defaultView.getComputedStyle(elem, null);
    var top = $doc.getBoxObjectFor(elem).y - Math.round(
        style.getPropertyCSSValue('border-top-width').getFloatValue(
        CSSPrimitiveValue.CSS_PX));
      
    var parent = elem.parentNode;
    while (parent) {
      // Sometimes get NAN.
      if (parent.scrollTop > 0) {
        top -= parent.scrollTop;
      }
      parent = parent.parentNode;
    }

    return top +
      @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollTop;
  }-*/;
  
  @Override
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
  
  @Override
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

}
