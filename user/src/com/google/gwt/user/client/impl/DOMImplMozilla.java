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
package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Mozilla implementation of StandardBrowser.
 */
class DOMImplMozilla extends DOMImplStandard {

  @Override
  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return evt.detail || -1;
  }-*/;

  @Override
  public native int getAbsoluteLeft(Element elem) /*-{
    // We cannot use DOMImpl here because offsetLeft/Top return erroneous
    // values when overflow is not visible.  We have to difference screenX
    // here due to a change in getBoxObjectFor which causes inconsistencies
    // on whether the calculations are inside or outside of the element's
    // border.
    try {
      return $doc.getBoxObjectFor(elem).screenX
          - $doc.getBoxObjectFor($doc.documentElement).screenX;
    } catch (e) {
      // This works around a bug in the FF3 betas. The bug
      // should be fixed before they release, so this can
      // be removed at a later date.
      // https://bugzilla.mozilla.org/show_bug.cgi?id=409111
      // DOMException.WRONG_DOCUMENT_ERR == 4
      if (e.code == 4) {
        return 0;
      }
      throw e;
    }
  }-*/;

  @Override
  public native int getAbsoluteTop(Element elem) /*-{
    // We cannot use DOMImpl here because offsetLeft/Top return erroneous
    // values when overflow is not visible.  We have to difference screenY
    // here due to a change in getBoxObjectFor which causes inconsistencies
    // on whether the calculations are inside or outside of the element's
    // border.
    try {
      return $doc.getBoxObjectFor(elem).screenY
          - $doc.getBoxObjectFor($doc.documentElement).screenY;
    } catch (e) {
      // This works around a bug in the FF3 betas. The bug
      // should be fixed before they release, so this can
      // be removed at a later date.
      // https://bugzilla.mozilla.org/show_bug.cgi?id=409111
      // DOMException.WRONG_DOCUMENT_ERR == 4
      if (e.code == 4) {
        return 0;
      }
      throw e;
    }
  }-*/;

  @Override
  public native boolean isOrHasChild(Element parent, Element child) /*-{
    // For more information about compareDocumentPosition, see:
    // http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
    return (parent === child) || !!(parent.compareDocumentPosition(child) & 16);  
  }-*/;

  @Override
  public void sinkEvents(Element elem, int bits) {
    super.sinkEvents(elem, bits);
    sinkEventsMozilla(elem, bits);
  }

  public native void sinkEventsMozilla(Element elem, int bits) /*-{
    if (bits & 0x20000) {
      elem.addEventListener('DOMMouseScroll', $wnd.__dispatchEvent, false);
    }
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

  @Override
  protected void initEventSystem() {
    super.initEventSystem();
    initSyntheticMouseUpEvents();
  }

  private native void initSyntheticMouseUpEvents() /*-{
    $wnd.addEventListener(
      'mouseout',
      function(evt) {
        var cap = $wnd.__captureElem;
        if (cap && !evt.relatedTarget) {
          // Mozilla has the interesting habit of sending a mouseout event
          // with an 'html' element as the target when the mouse is released
          // outside of the browser window.
          if ('html' == evt.target.tagName.toLowerCase()) {
            // When this occurs, we synthesize a mouseup event, which is
            // useful for all sorts of dragging code (like in DialogBox).
            var muEvent = $doc.createEvent('MouseEvents');
            muEvent.initMouseEvent('mouseup', true, true, $wnd, 0,
              evt.screenX, evt.screenY, evt.clientX, evt.clientY, evt.ctrlKey,
              evt.altKey, evt.shiftKey, evt.metaKey, evt.button, null);
            cap.dispatchEvent(muEvent);
          }
        }
      },
      true
    );

    $wnd.addEventListener('DOMMouseScroll', $wnd.__dispatchCapturedMouseEvent,
      true);
  }-*/;
}
