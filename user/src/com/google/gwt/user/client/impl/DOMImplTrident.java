/*
 * Copyright 2009 Google Inc.
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
 * Trident layout engine implementation of
 * {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
public abstract class DOMImplTrident extends DOMImpl {
  private static JavaScriptObject callDispatchEvent;

  private static JavaScriptObject callDispatchDblClickEvent;

  private static JavaScriptObject callDispatchOnLoadEvent;

  private static JavaScriptObject callDispatchUnhandledEvent;

  /**
   * Let every GWT app on the page preview the current event. If any app cancels
   * the event, the event will be canceled for all apps.
   * 
   * @return <code>false</code> to cancel the event
   */
  private static native boolean previewEventImpl() /*-{
    var isCancelled = false; 
    for (var i = 0; i < $wnd.__gwt_globalEventArray.length; i++) {
      if (!$wnd.__gwt_globalEventArray[i]()) {
        isCancelled = true;
      }
    }
    return !isCancelled;
  }-*/;

  @Override
  public native Element eventGetFromElement(Event evt) /*-{
    // Prefer 'relatedTarget' if it's set (see createMouseEvent(), which
    // explicitly sets relatedTarget when synthesizing mouse events).
    return evt.relatedTarget || evt.fromElement;
  }-*/;

  @Override
  public native Element eventGetToElement(Event evt) /*-{
    // Prefer 'relatedTarget' if it's set (see createMouseEvent(), which
    // explicitly sets relatedTarget when synthesizing mouse events).
    return evt.relatedTarget || evt.toElement;
  }-*/;

  @Override
  public native Element getChild(Element elem, int index) /*-{
    return elem.children[index];
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
    // All GWT apps on the page register themselves with the globalEventArray
    // so that the first app to handle an event can allow all apps on the page
    // to preview it. See issue 3892 for more details.
    //
    // Apps cannot just mark the event as they visit it for a few reasons.
    // First, window level event handlers fire last in IE, so the first app to
    // cancel the event will be the last to see it. Second, window events do
    // not support arbitrary attributes, and the only writable field is the
    // returnValue, which has another use. Finally, window events are not
    // comparable (ex. a=event; b=event; a!=b), so we cannot keep a list of
    // events that have already been previewed by the current app.
    if ($wnd.__gwt_globalEventArray == null) {
      $wnd.__gwt_globalEventArray = new Array();
    }
    $wnd.__gwt_globalEventArray[$wnd.__gwt_globalEventArray.length] = $entry(function() {
      return @com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)($wnd.event);
    });

    var dispatchEvent = $entry(function() {
      // IE doesn't define event.currentTarget, so we squirrel it away here. It
      // also seems that IE won't allow you to add expandos to the event object,
      // so we have to store it in a global. This is ok because only one event
      // can actually be dispatched at a time.
      var oldEventTarget = @com.google.gwt.dom.client.DOMImplTrident::currentEventTarget;
      @com.google.gwt.dom.client.DOMImplTrident::currentEventTarget = this;

      // The first GWT app on the page to handle the event allows all apps to
      // preview it before continuing or cancelling, which is consistent with
      // other browsers.
      if ($wnd.event.returnValue == null) {
        $wnd.event.returnValue = true;
        if (!@com.google.gwt.user.client.impl.DOMImplTrident::previewEventImpl()()) {
          @com.google.gwt.dom.client.DOMImplTrident::currentEventTarget = oldEventTarget;
          return;
        }
      }

      var listener, curElem = this;
      while (curElem && !(listener = curElem.__listener)) {
        curElem = curElem.parentElement;
      }

      if (listener) {
        if (@com.google.gwt.user.client.impl.DOMImpl::isMyListener(Ljava/lang/Object;)(listener)) {
          @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)($wnd.event, curElem, listener);
        }
      }

      @com.google.gwt.dom.client.DOMImplTrident::currentEventTarget = oldEventTarget;
    });

    var dispatchDblClickEvent = $entry(function() {
      var newEvent = $doc.createEventObject();
      // Synthesize a click event if one hasn't already been synthesized.
      // Issue 4027: fireEvent is undefined on disabled input elements.
      if ($wnd.event.returnValue == null && $wnd.event.srcElement.fireEvent) {
        $wnd.event.srcElement.fireEvent('onclick', newEvent);
      }
      if (this.__eventBits & 2) {
        dispatchEvent.call(this);
      } else if ($wnd.event.returnValue == null) {
        // Ensure that we preview the event even if we aren't handling it.
        $wnd.event.returnValue = true;
        @com.google.gwt.user.client.impl.DOMImplTrident::previewEventImpl()();
      }
    });

    var dispatchUnhandledEvent = $entry(function() {
      this.__gwtLastUnhandledEvent = $wnd.event.type;
      dispatchEvent.call(this);
    });

    // Hook the above functions on the current window object, and
    // create a caller function for each one that indirects via
    // the global object. Make sure that the caller function has
    // no direct reference to the GWT program.
    var moduleName = @com.google.gwt.core.client.GWT::getModuleName()().replace(/\./g,'_');

    $wnd['__gwt_dispatchEvent_' + moduleName] = dispatchEvent;
    @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent = new Function('w',
      'return function() { w.__gwt_dispatchEvent_' + moduleName + '.call(this) }')($wnd);

    $wnd['__gwt_dispatchDblClickEvent_' + moduleName] = dispatchDblClickEvent;
    @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchDblClickEvent = new Function('w',
      'return function() { w.__gwt_dispatchDblClickEvent_' + moduleName + '.call(this)}')($wnd);

    $wnd['__gwt_dispatchUnhandledEvent_' + moduleName] = dispatchUnhandledEvent;
    @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchUnhandledEvent = new Function('w',
      'return function() { w.__gwt_dispatchUnhandledEvent_' + moduleName + '.call(this)}')($wnd);

    @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchOnLoadEvent = new Function('w',
      'return function() { w.__gwt_dispatchUnhandledEvent_' + moduleName + '.call(w.event.srcElement)}')($wnd);

    // We need to create these delegate functions to fix up the 'this' context.
    // Normally, 'this' is the firing element, but this is only true for
    // 'onclick = ...' event handlers, not for handlers setup via attachEvent().
    var bodyDispatcher = $entry(function() { dispatchEvent.call($doc.body); });
    var bodyDblClickDispatcher = $entry(function() { dispatchDblClickEvent.call($doc.body); });

    $doc.body.attachEvent('onclick', bodyDispatcher);
    $doc.body.attachEvent('onmousedown', bodyDispatcher);
    $doc.body.attachEvent('onmouseup', bodyDispatcher);
    $doc.body.attachEvent('onmousemove', bodyDispatcher);
    $doc.body.attachEvent('onmousewheel', bodyDispatcher);
    $doc.body.attachEvent('onkeydown', bodyDispatcher);
    $doc.body.attachEvent('onkeypress', bodyDispatcher);
    $doc.body.attachEvent('onkeyup', bodyDispatcher);
    $doc.body.attachEvent('onfocus', bodyDispatcher);
    $doc.body.attachEvent('onblur', bodyDispatcher);
    $doc.body.attachEvent('ondblclick', bodyDblClickDispatcher);
    $doc.body.attachEvent('oncontextmenu', bodyDispatcher);
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
  public void sinkBitlessEvent(Element elem, String eventTypeName) {
    // not supported
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
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    // Add a ondblclick handler if onclick is desired to ensure that 
    // a user's double click will result in two onclick events.
    if (chMask & (0x00003)) elem.ondblclick  = (bits & (0x00003)) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchDblClickEvent : null;
    if (chMask & 0x00004) elem.onmousedown   = (bits & 0x00004) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00008) elem.onmouseup     = (bits & 0x00008) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00010) elem.onmouseover   = (bits & 0x00010) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00020) elem.onmouseout    = (bits & 0x00020) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00040) elem.onmousemove   = (bits & 0x00040) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00080) elem.onkeydown     = (bits & 0x00080) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00100) elem.onkeypress    = (bits & 0x00100) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00200) elem.onkeyup       = (bits & 0x00200) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00400) elem.onchange      = (bits & 0x00400) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x00800) elem.onfocus       = (bits & 0x00800) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x01000) elem.onblur        = (bits & 0x01000) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x02000) elem.onlosecapture = (bits & 0x02000) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x04000) elem.onscroll      = (bits & 0x04000) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x08000) {
      if (elem.nodeName == "IFRAME") {
        if (bits & 0x08000) {
          elem.attachEvent('onload', @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchOnLoadEvent);
        } else {
          elem.detachEvent('onload', @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchOnLoadEvent);
        }
      } else {
       elem.onload = (bits & 0x08000) ?
          @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchUnhandledEvent : null;
      }
    }
    if (chMask & 0x10000) elem.onerror       = (bits & 0x10000) ?
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x20000) elem.onmousewheel  = (bits & 0x20000) ? 
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x40000) elem.oncontextmenu = (bits & 0x40000) ? 
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
    if (chMask & 0x80000) elem.onpaste       = (bits & 0x80000) ? 
        @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent : null;
  }-*/;
}
