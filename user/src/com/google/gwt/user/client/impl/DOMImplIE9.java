/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.user.client.Element;

/**
 * IE9 implementation of {@link com.google.gwt.user.client.impl.DOMImplStandardBase}.
 */
class DOMImplIE9 extends DOMImplStandardBase {

  /**
   * IE uses a non-standard way of handling drag events.
   */
  @Override
  protected void initEventSystem() {
    super.initEventSystem();
    initEventSystemIE();
  }

  @Override
  protected void sinkBitlessEventImpl(Element elem, String eventTypeName) {
    super.sinkBitlessEventImpl(elem, eventTypeName);

    if (BrowserEvents.DRAGOVER.equals(eventTypeName)) {
      /*
       * In IE, we have to sink dragenter with dragover in order to make an
       * element a drop target.
       */
      super.sinkBitlessEventImpl(elem, BrowserEvents.DRAGENTER);
    }
  }

  private native void initEventSystemIE() /*-{
    // In IE, drag events return false instead of calling preventDefault.
    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchDragEvent = $entry(function(evt) {
      @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent.call(this, evt);
      return false;
    });
  }-*/;
}
