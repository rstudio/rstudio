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
  public native int eventGetClientX(Event evt) /*-{
    var htmlOffset;
    // Firefox 3 is actively throwing errors when getBoxObjectFor() is called,
    // so we use getBoundingClientRect() whenever possible (but it's not
    // supported on older versions). If changing this code, make sure to check
    // the museum entry for issue 1932.
    if (Element.prototype.getBoundingClientRect) {
      // getBoundingClientRect().left is off by the document element's
      // border-left-width.
      var style = $wnd.getComputedStyle($doc.documentElement, '')
      htmlOffset = $doc.documentElement.getBoundingClientRect().left +
        parseInt(style.borderLeftWidth);
    } else {
      htmlOffset = $doc.getBoxObjectFor($doc.documentElement).x || 0;
    }
    return evt.clientX - htmlOffset;
  }-*/;

  @Override
  public native int eventGetClientY(Event evt) /*-{
    var htmlOffset;
    // Firefox 3 is actively throwing errors when getBoxObjectFor() is called,
    // so we use getBoundingClientRect() whenever possible (but it's not
    // supported on older versions). If changing this code, make sure to check
    // the museum entry for issue 1932.
    if (Element.prototype.getBoundingClientRect) {
      // getBoundingClientRect().top is off by the document element's
      // border-top-width.
      var style = $wnd.getComputedStyle($doc.documentElement, '')
      htmlOffset = $doc.documentElement.getBoundingClientRect().top +
        parseInt(style.borderTopWidth);
    } else {
      htmlOffset = $doc.getBoxObjectFor($doc.documentElement).y || 0;
    }
    return evt.clientY - htmlOffset;
  }-*/;

  @Override
  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return evt.detail || 0;
  }-*/;

  @Override
  public void sinkEvents(Element elem, int bits) {
    super.sinkEvents(elem, bits);
    sinkEventsMozilla(elem, bits);
  }

  public native void sinkEventsMozilla(Element elem, int bits) /*-{
    if (bits & 0x20000) {
      elem.addEventListener('DOMMouseScroll', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent, false);
    }
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

    $wnd.addEventListener('DOMMouseScroll', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent,
      true);
  }-*/;
}
