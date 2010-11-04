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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Base implementation of {@link com.google.gwt.user.client.impl.DOMImpl} shared
 * by those browsers that come a bit closer to supporting a common standard (ie,
 * not IE).
 */
abstract class DOMImplStandard extends DOMImpl {

  @SuppressWarnings("unused")
  private static JavaScriptObject captureElem;

  @SuppressWarnings("unused")
  private static JavaScriptObject dispatchCapturedEvent;

  @SuppressWarnings("unused")
  private static JavaScriptObject dispatchCapturedMouseEvent;

  @SuppressWarnings("unused")
  private static JavaScriptObject dispatchEvent;

  @SuppressWarnings("unused")
  private static JavaScriptObject dispatchUnhandledEvent;

  @Override
  public Element eventGetFromElement(Event evt) {
    if (evt.getType().equals("mouseover")) {
      return evt.getRelatedEventTarget().cast();
    }

    if (evt.getType().equals("mouseout")) {
      return evt.getEventTarget().cast();
    }

    return null;  
  }
 
  @Override
  public Element eventGetToElement(Event evt) {
    if (evt.getType().equals("mouseover")) {
      return evt.getEventTarget().cast();
    }

    if (evt.getType().equals("mouseout")) {
      return evt.getRelatedEventTarget().cast();
    }

    return null;
  }
  
  @Override
  public native Element getChild(Element elem, int index) /*-{
    var count = 0, child = elem.firstChild;
    while (child) {
      if (child.nodeType == 1) {
        if (index == count)
          return child;
        ++count;
      }
      child = child.nextSibling;
    }

    return null;
  }-*/;

  @Override
  public native int getChildCount(Element elem) /*-{
    var count = 0, child = elem.firstChild;
    while (child) {
      if (child.nodeType == 1)
        ++count;
      child = child.nextSibling;
    }
    return count;
  }-*/;

  @Override
  public native int getChildIndex(Element parent, Element toFind) /*-{
    var count = 0, child = parent.firstChild;
    while (child) {
      if (child === toFind) {
        return count;
      }
      if (child.nodeType == 1) {
        ++count;
      }
      child = child.nextSibling;
    }
    return -1;
  }-*/;

  @Override
  public native void insertChild(Element parent, Element toAdd, int index) /*-{
    var count = 0, child = parent.firstChild, before = null;
    while (child) {
      if (child.nodeType == 1) {
        if (count == index) {
          before = child;
          break;
        }
        ++count;
      }
      child = child.nextSibling;
    }

    parent.insertBefore(toAdd, before);
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

  @Override
  protected native void initEventSystem() /*-{
    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent = $entry(function(evt) {
      if ((@com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent)(evt)) {
        var cap = @com.google.gwt.user.client.impl.DOMImplStandard::captureElem;
        if (cap && cap.__listener) {
          if (@com.google.gwt.user.client.impl.DOMImpl::isMyListener(Ljava/lang/Object;)(cap.__listener)) {
            @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, cap, cap.__listener);
            evt.stopPropagation();
          }
        }
      }  
    });

    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent = $entry(function(evt) {
      if (!@com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)(evt)) {
        evt.stopPropagation();
        evt.preventDefault();
        return false;
      }

      return true;
    });

    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchUnhandledEvent = $entry(function(evt) {
      this.__gwtLastUnhandledEvent = evt.type;
      @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent.call(this, evt);
    });

    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent = $entry(function(evt) {
      var listener, curElem = this;
      while (curElem && !(listener = curElem.__listener)) {
        curElem = curElem.parentNode;
      }

      if (curElem && curElem.nodeType != 1) {
        curElem = null;
      }
  
      if (listener) {
        if (@com.google.gwt.user.client.impl.DOMImpl::isMyListener(Ljava/lang/Object;)(listener)) {
          @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, curElem, listener);
        }
      }
    });

    $wnd.addEventListener('click', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('dblclick', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mousedown', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mouseup', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mousemove', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mouseover', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mouseout', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mousewheel', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('keydown', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent, true);
    $wnd.addEventListener('keyup', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent, true);
    $wnd.addEventListener('keypress', @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent, true);
  }-*/;

  protected native void sinkEventsImpl(Element elem, int bits) /*-{
    var chMask = (elem.__eventBits || 0) ^ bits;
    elem.__eventBits = bits;
    if (!chMask) return;
   
    if (chMask & 0x00001) elem.onclick       = (bits & 0x00001) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00002) elem.ondblclick    = (bits & 0x00002) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00004) elem.onmousedown   = (bits & 0x00004) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00008) elem.onmouseup     = (bits & 0x00008) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00010) elem.onmouseover   = (bits & 0x00010) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00020) elem.onmouseout    = (bits & 0x00020) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00040) elem.onmousemove   = (bits & 0x00040) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00080) elem.onkeydown     = (bits & 0x00080) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00100) elem.onkeypress    = (bits & 0x00100) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00200) elem.onkeyup       = (bits & 0x00200) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00400) elem.onchange      = (bits & 0x00400) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x00800) elem.onfocus       = (bits & 0x00800) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x01000) elem.onblur        = (bits & 0x01000) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x02000) elem.onlosecapture = (bits & 0x02000) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x04000) elem.onscroll      = (bits & 0x04000) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x08000) elem.onload        = (bits & 0x08000) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchUnhandledEvent : null;
    if (chMask & 0x10000) elem.onerror       = (bits & 0x10000) ?
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x20000) elem.onmousewheel  = (bits & 0x20000) ? 
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x40000) elem.oncontextmenu = (bits & 0x40000) ? 
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
    if (chMask & 0x80000) elem.onpaste       = (bits & 0x80000) ? 
        @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent : null;
  }-*/;

  private native void releaseCaptureImpl(Element elem) /*-{
    if (elem === @com.google.gwt.user.client.impl.DOMImplStandard::captureElem) {
      @com.google.gwt.user.client.impl.DOMImplStandard::captureElem = null;
    }
  }-*/;

  private native void setCaptureImpl(Element elem) /*-{
    @com.google.gwt.user.client.impl.DOMImplStandard::captureElem = elem;
  }-*/;
}
