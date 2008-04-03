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
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplIE6 extends DOMImpl {

  /**
   * Referenced from JavaScript.
   */
  @SuppressWarnings("unused")
  private static Element currentEventTarget;

  @Override
  public native int eventGetClientX(Event evt) /*-{
    return evt.clientX -
        @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientLeft;
  }-*/;

  @Override
  public native int eventGetClientY(Event evt) /*-{
    return evt.clientY -
        @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientTop;
  }-*/;

  @Override
  public native Element eventGetCurrentTarget(Event evt) /*-{
    return @com.google.gwt.user.client.impl.DOMImplIE6::currentEventTarget;
  }-*/;

  @Override
  public native Element eventGetFromElement(Event evt) /*-{
    return evt.fromElement ? evt.fromElement : null;
  }-*/;

  @Override
  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return Math.round(-evt.wheelDelta / 40) || -1;
  }-*/;

  @Override
  public native Element eventGetTarget(Event evt) /*-{
    return evt.srcElement || null;
  }-*/;

  @Override
  public native Element eventGetToElement(Event evt) /*-{
    return evt.toElement || null;
  }-*/;

  @Override
  public native void eventPreventDefault(Event evt) /*-{
    evt.returnValue = false;
  }-*/;

  @Override
  public native String eventToString(Event evt) /*-{
    if (evt.toString) return evt.toString();
      return "[object Event]";
  }-*/;

  @Override
  public native Element getChild(Element elem, int index) /*-{
    var child = elem.children[index];
    return child || null;
  }-*/;

  @Override
  public native int getChildCount(Element elem) /*-{
    return elem.children.length;
  }-*/;

  @Override
  public native int getChildIndex(Element parent, Element child) /*-{
    var count = parent.children.length;
    for (var i = 0; i < count; ++i) {
      if (child === parent.children[i]) {
        return i;
      }
    }
    return -1;
  }-*/;

  @Override
  public native void initEventSystem() /*-{
    // Set up event dispatchers.
    $wnd.__dispatchEvent = function() {
      // IE doesn't define event.currentTarget, so we squirrel it away here. It
      // also seems that IE won't allow you to add expandos to the event object,
      // so we have to store it in a global. This is ok because only one event
      // can actually be dispatched at a time.
      var oldEventTarget = @com.google.gwt.user.client.impl.DOMImplIE6::currentEventTarget;
      @com.google.gwt.user.client.impl.DOMImplIE6::currentEventTarget = this;

      if ($wnd.event.returnValue == null) {
        $wnd.event.returnValue = true;
        if (!@com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)($wnd.event)) {
          @com.google.gwt.user.client.impl.DOMImplIE6::currentEventTarget = oldEventTarget;
          return;
        }
      }

      var listener, curElem = this;
      while (curElem && !(listener = curElem.__listener))
        curElem = curElem.parentElement;

      if (listener)
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)($wnd.event, curElem, listener);

      @com.google.gwt.user.client.impl.DOMImplIE6::currentEventTarget = oldEventTarget;
    };

    $wnd.__dispatchDblClickEvent = function() {
      var newEvent = $doc.createEventObject();
      this.fireEvent('onclick', newEvent);
      if (this.__eventBits & 2)
        $wnd.__dispatchEvent.call(this);
    };

    $doc.body.onclick       =
    $doc.body.onmousedown   =
    $doc.body.onmouseup     =
    $doc.body.onmousemove   =
    $doc.body.onmousewheel  =
    $doc.body.onkeydown     =
    $doc.body.onkeypress    =
    $doc.body.onkeyup       =
    $doc.body.onfocus       =
    $doc.body.onblur        =
    $doc.body.ondblclick    = $wnd.__dispatchEvent;
  }-*/;

  @Override
  public native void insertChild(Element parent, Element child, int index) /*-{
    if (index >= parent.children.length)
      parent.appendChild(child);
    else
      parent.insertBefore(child, parent.children[index]);
  }-*/;

  @Override
  public void releaseCapture(Element elem) {
    maybeInitializeEventSystem();
    releaseCaptureImpl(elem);
  }

  @Override
  public void setCapture(Element elem) {
    maybeInitializeEventSystem();
    setCaptureImpl(elem);
  }

  @Override
  public void sinkEvents(Element elem, int bits) {
    maybeInitializeEventSystem();
    sinkEventsImpl(elem, bits);
  }

  private native void releaseCaptureImpl(Element elem) /*-{
    elem.releaseCapture();
  }-*/;

  private native void setCaptureImpl(Element elem) /*-{
    elem.setCapture();
  }-*/;

  private native void sinkEventsImpl(Element elem, int bits) /*-{
    var chMask = (elem.__eventBits || 0) ^ bits;
    elem.__eventBits = bits;
    if (!chMask) return;
     
    if (chMask & 0x00001) elem.onclick       = (bits & 0x00001) ?
        $wnd.__dispatchEvent : null;
    // Add a ondblclick handler if onclick is desired to ensure that 
    // a user's double click will result in two onlclick events.
    if (chMask & (0x00003)) elem.ondblclick  = (bits & (0x00003)) ?
        $wnd.__dispatchDblClickEvent : null;
    if (chMask & 0x00004) elem.onmousedown   = (bits & 0x00004) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00008) elem.onmouseup     = (bits & 0x00008) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00010) elem.onmouseover   = (bits & 0x00010) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00020) elem.onmouseout    = (bits & 0x00020) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00040) elem.onmousemove   = (bits & 0x00040) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00080) elem.onkeydown     = (bits & 0x00080) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00100) elem.onkeypress    = (bits & 0x00100) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00200) elem.onkeyup       = (bits & 0x00200) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00400) elem.onchange      = (bits & 0x00400) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x00800) elem.onfocus       = (bits & 0x00800) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x01000) elem.onblur        = (bits & 0x01000) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x02000) elem.onlosecapture = (bits & 0x02000) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x04000) elem.onscroll      = (bits & 0x04000) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x08000) elem.onload        = (bits & 0x08000) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x10000) elem.onerror       = (bits & 0x10000) ?
        $wnd.__dispatchEvent : null;
    if (chMask & 0x20000) elem.onmousewheel  = (bits & 0x20000) ? 
        $wnd.__dispatchEvent : null;
  }-*/;
}
