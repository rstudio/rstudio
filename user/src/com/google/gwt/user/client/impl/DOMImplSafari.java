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
package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Safari implementation of {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplSafari extends DOMImplStandard {
  public native int eventGetClientX(Event evt) /*-{
    // In Safari2: clientX is wrong and pageX is returned instead.
    return evt.pageX - $doc.body.scrollLeft;
  }-*/;

  public native int eventGetClientY(Event evt) /*-{
    // In Safari2: clientY is wrong and pageY is returned instead.
    return evt.pageY - $doc.body.scrollTop;
  }-*/;

  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return Math.round(-evt.wheelDelta / 40);
  }-*/;

  public native int getAbsoluteLeft(Element elem) /*-{
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetLeft.
    if (elem.offsetLeft == null) {
      return 0;
    }

    var left = 0;
    var curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
      left -= curr.scrollLeft;
      curr = curr.parentNode;
    }
    while (elem) {
      left += elem.offsetLeft;

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      var parent = elem.offsetParent;
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return left;
  }-*/;

  public native int getAbsoluteTop(Element elem) /*-{
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetTop.
    if (elem.offsetTop == null) {
      return 0;
    }

    var top = 0;
    var curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
      top -= curr.scrollTop;
      curr = curr.parentNode;
    }
    while (elem) {
      top += elem.offsetTop;

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      var parent = elem.offsetParent;
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return top;
  }-*/;

  /**
   * Gets the height of the browser window's client area excluding the
   * scroll bar.
   * 
   * @return the window's client height
   */
  public native int windowGetClientHeight() /*-{
    return $wnd.innerHeight;
  }-*/;

  /**
   * Gets the width of the browser window's client area excluding the
   * vertical scroll bar.
   * 
   * @return the window's client width
   */
  public native int windowGetClientWidth() /*-{
    return $wnd.innerWidth;
  }-*/;
}
