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
 * Mozilla implementation of StandardBrowser.
 */
class DOMImplMozilla extends DOMImplStandard {

  @Override
  public native int getAbsoluteLeft(Element elem) /*-{
    // Firefox 3 is actively throwing errors when getBoxObjectFor() is called,
    // so we use getBoundingClientRect() whenever possible (but it's not
    // supported on older versions). If changing this code, make sure to check
    // the museum entry for issue 1932.
    // (x) | 0 is used to coerce the value to an integer
    if (Element.prototype.getBoundingClientRect) {
      return (elem.getBoundingClientRect().left +
        @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollLeft) | 0;
    } else {
      // We cannot use DOMImpl here because offsetLeft/Top return erroneous
      // values when overflow is not visible.  We have to difference screenX
      // here due to a change in getBoxObjectFor which causes inconsistencies
      // on whether the calculations are inside or outside of the element's
      // border.
      return $doc.getBoxObjectFor(elem).screenX -
        $doc.getBoxObjectFor($doc.documentElement).screenX;
    }
  }-*/;

  @Override
  public native int getAbsoluteTop(Element elem) /*-{
    // Firefox 3 is actively throwing errors when getBoxObjectFor() is called,
    // so we use getBoundingClientRect() whenever possible (but it's not
    // supported on older versions). If changing this code, make sure to check
    // the museum entry for issue 1932.
    // (x) | 0 is used to coerce the value to an integer
    if (Element.prototype.getBoundingClientRect) {
      return (elem.getBoundingClientRect().top +
        @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollTop) | 0;
    } else {
      // We cannot use DOMImpl here because offsetLeft/Top return erroneous
      // values when overflow is not visible.  We have to difference screenX
      // here due to a change in getBoxObjectFor which causes inconsistencies
      // on whether the calculations are inside or outside of the element's
      // border.
      return $doc.getBoxObjectFor(elem).screenY -
        $doc.getBoxObjectFor($doc.documentElement).screenY;
    }
  }-*/;

  @Override
  public native int getBodyOffsetLeft() /*-{
    var style = $wnd.getComputedStyle($doc.documentElement, '');
    return parseInt(style.marginLeft) + parseInt(style.borderLeftWidth);
  }-*/;
  
  @Override
  public native int getBodyOffsetTop() /*-{
    var style = $wnd.getComputedStyle($doc.documentElement, '');
    return parseInt(style.marginTop) + parseInt(style.borderTopWidth);
  }-*/;
  
  @Override
  public native String getInnerText(Element elem) /*-{
    return elem.textContent;
  }-*/;

  @Override
  public native boolean isOrHasChild(Element parent, Element child) /*-{
    // For more information about compareDocumentPosition, see:
    // http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
    return (parent === child) || !!(parent.compareDocumentPosition(child) & 16);  
  }-*/;
  
  @Override
  public native void setInnerText(Element elem, String text) /*-{
    elem.textContent = text || '';
  }-*/;

  @Override
  public native String toString(Element elem) /*-{
    // Basic idea is to use the innerHTML property by copying the node into a
    // div and getting the innerHTML
    var temp = elem.cloneNode(true);
    var tempDiv = $doc.createElement("DIV");
    tempDiv.appendChild(temp);
    outer = tempDiv.innerHTML;
    temp.innerHTML = "";
    return outer;
  }-*/;
}
