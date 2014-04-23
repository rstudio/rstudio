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

import com.google.gwt.dom.client.Element;

/**
 * Mozilla implementation of StandardBrowser.
 */
class DOMImplMozilla extends DOMImplStandard {

  static {
    addMozillaCaptureEventDispatchers();
  }

  private static native void addMozillaCaptureEventDispatchers() /*-{
    @com.google.gwt.user.client.impl.DOMImplStandard::captureEventDispatchers['DOMMouseScroll'] =
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*);
  }-*/;

  @Override
  public void sinkEvents(Element elem, int bits) {
    super.sinkEvents(elem, bits);
    sinkEventsMozilla(elem, bits);
  }

  @SuppressWarnings("deprecation")
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
      $entry(function(evt) {
        var cap = @com.google.gwt.user.client.impl.DOMImplStandard::captureElem; 
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
      }),
      true
    );
  }-*/;
}
