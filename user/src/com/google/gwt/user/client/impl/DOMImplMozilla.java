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
 * Mozilla implementation of StandardBrowser. The main difference between
 * Mozilla and others is that element comparison must be done using isSameNode()
 * (== comparison doesn't always give you the right answer, probably because of
 * its JavaScript wrappers for xpcom dom nodes).
 */
class DOMImplMozilla extends DOMImplStandard {

  public native boolean compare(Element elem1, Element elem2) /*-{
    if (!elem1 && !elem2) {
      return true;
    } else if (!elem1 || !elem2) {
      return false;
    }
    return (elem1.isSameNode(elem2));
  }-*/;

  public native int eventGetButton(Event evt) /*-{
    // Mozilla and IE disagree on what the button codes for buttons should be.
    // Translating to match IE standard.
    var button = evt.button;
    if(button == 0) {
      return 1;
    } else if (button == 1) {
      return 4;
    } else {
      return button;
    }
 }-*/;

  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return evt.detail;
  }-*/;

  public native int getChildIndex(Element parent, Element toFind) /*-{
    var count = 0, child = parent.firstChild;
    while (child) {
      if (child.isSameNode(toFind)) {
        return count;
      }
      if (child.nodeType == 1) {
        ++count;
      }
      child = child.nextSibling;
    }
    return -1;
  }-*/;

  public void init() {
    super.init();
    initMozilla();
  }

  public native boolean isOrHasChild(Element parent, Element child) /*-{
    while (child) {
      if (parent.isSameNode(child)) {
        return true;
      }

      try {
        child = child.parentNode;
      } catch(e) {
        // Give up on 'Permission denied to get property
        // HTMLDivElement.parentNode'
        // See https://bugzilla.mozilla.org/show_bug.cgi?id=208427
        return false;
      }

      if (child && (child.nodeType != 1)) {
        child = null;
      }
    }
    return false;
  }-*/;

  public native void releaseCapture(Element elem) /*-{
    if (elem.isSameNode($wnd.__captureElem)) {
      $wnd.__captureElem = null;
    }
  }-*/;

  public void sinkEvents(Element elem, int bits) {
    super.sinkEvents(elem, bits);
    sinkEventsMozilla(elem, bits);
  }

  public native void sinkEventsMozilla(Element elem, int bits) /*-{
    if (bits & 0x20000) {
      elem.addEventListener('DOMMouseScroll', $wnd.__dispatchEvent, false);
    }
  }-*/;

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

  protected native void initMozilla() /*-{
    $wnd.addEventListener('DOMMouseScroll', $wnd.__dispatchCapturedMouseEvent, true);
  }-*/;

}
