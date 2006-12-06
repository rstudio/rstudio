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

  public native int getAbsoluteLeft(Element elem) /*-{
    var left = $doc.getBoxObjectFor(elem).x;
    var parent = elem;

    while (parent) {
      // Sometimes get NAN.
      if (parent.scrollLeft > 0) {
        left = left -  parent.scrollLeft;
      }
      parent = parent.parentNode;
    }
 
  // Must cover both Standard and Quirks mode. 
    return left + $doc.body.scrollLeft + $doc.documentElement.scrollLeft;
  }-*/;

  public native int getAbsoluteTop(Element elem) /*-{
    var top = $doc.getBoxObjectFor(elem).y;
    var parent = elem;
    while (parent) {
      // Sometimes get NAN.
      if (parent.scrollTop > 0) {
        top -= parent.scrollTop;
      }
      parent = parent.parentNode;
    }
   
    // Must cover both Standard and Quirks mode.
    return top + $doc.body.scrollTop + $doc.documentElement.scrollTop;
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

  public native boolean isOrHasChild(Element parent, Element child) /*-{
    while (child) {
      if (parent.isSameNode(child)) {
        return true;
      }
      child = child.parentNode;
      if (child.nodeType != 1) {
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
}
