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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.Disposable;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * Native implementation associated with {@link com.google.gwt.user.client.DOM}.
 */
public abstract class DOMImpl {

  protected static boolean eventSystemIsInitialized;

  /**
   * Registers a raw DOM event listener to be cleaned up when the module is unloaded.
   */
  public static native void addDisposableEvent(com.google.gwt.dom.client.Element elem, String event,
                                               JavaScriptObject handler, boolean capture) /*-{
      elem.__gwt_disposeEvent = elem.__gwt_disposeEvent || [];
      elem.__gwt_disposeEvent.push({event: event, handler: handler, capture: capture});
  }-*/;

  /**
   * Scan all DOM elements looking for event listeners from our module and remove event listeners from them.
   * @param dom
   */
  public static void cleanupDOM(DOMImpl dom) {
    NodeList<com.google.gwt.dom.client.Element> allElements = Document.get().getElementsByTagName("*");
    for (int i = 0; i < allElements.getLength(); i++) {
      com.google.gwt.dom.client.Element elem = allElements.getItem(i);
      Element userElem = (Element) elem;
      if (dom.getEventsSunk(userElem) != 0) {
        dom.sinkEvents(userElem, 0);
      }
      EventListener listener = dom.getEventListener(userElem);
      // nulls out event listener if and only if it was assigned from our module
      if (GWT.isScript() && listener != null && isMyListener(listener)) {
        dom.setEventListener(userElem, null);
      }
      // cleans up DOM-style addEventListener registered handlers
      maybeRemoveDisposableEvent(elem);
    }
  }

  /**
   * Returns <code>true</code>if the object is an instance of EventListener and
   * the object belongs to this module.
   */
  protected static boolean isMyListener(Object object) {
    /*
     * The first test ensures the Object belongs to this module in Production
     * Mode by ensuring this is not a JavaScriptObject. In Production Mode,
     * foreign Java objects appear to be JavaScriptObject. See
     * Cast.isJavaScriptObject().
     * 
     * The second test then checks the exact type.
     * 
     * TODO: make the generated code smaller!
     */
    return !(object instanceof JavaScriptObject)
        && (object instanceof com.google.gwt.user.client.EventListener);
  }

  private static native void maybeRemoveDisposableEvent(com.google.gwt.dom.client.Element elem) /*-{
    var diEvents = elem.__gwt_disposeEvent;
    if (diEvents) {
      for (var i = 0, l = diEvents.length; i < l; i++) {
        var diEvent = diEvents[i];
        elem.removeEventListener(diEvent.event, diEvent.handler, diEvent.capture);
        elem.__gwt_disposeEvent = null;
      }
    }
  }-*/;

  public native void eventCancelBubble(Event evt, boolean cancel) /*-{
    evt.cancelBubble = cancel;
  }-*/;

  public abstract Element eventGetFromElement(Event evt);
  
  public native boolean eventGetRepeat(Event evt) /*-{
    return !!evt.repeat;
  }-*/; 
 
  public abstract Element eventGetToElement(Event evt);

  public final int eventGetTypeInt(Event evt) {
    return eventGetTypeInt(evt.getType());
  }
  
  public native int eventGetTypeInt(String eventType) /*-{
    switch (eventType) {
    case "blur": return 0x01000;
    case "change": return 0x00400;
    case "click": return 0x00001;
    case "dblclick": return 0x00002;
    case "focus": return 0x00800;
    case "keydown": return 0x00080;
    case "keypress": return 0x00100;
    case "keyup": return 0x00200;
    case "load": return 0x08000;
    case "losecapture": return 0x02000;
    case "mousedown": return 0x00004;
    case "mousemove": return 0x00040;
    case "mouseout": return 0x00020;
    case "mouseover": return 0x00010;
    case "mouseup": return 0x00008;
    case "scroll": return 0x04000;
    case "error": return 0x10000;
    case "mousewheel": return 0x20000;
    case "DOMMouseScroll": return 0x20000;
    case "contextmenu": return 0x40000;
    case "paste": return 0x80000;
    case "touchstart": return 0x100000;
    case "touchmove": return 0x200000;
    case "touchend": return 0x400000;
    case "touchcancel": return 0x800000;
    case "gesturestart": return 0x1000000;
    case "gesturechange": return 0x2000000;
    case "gestureend": return 0x4000000;
    default: return -1;
    }
  }-*/; 
  
  public native void eventSetKeyCode(Event evt, char key) /*-{
    evt.keyCode = key;
  }-*/;
 
  public abstract Element getChild(Element elem, int index);

  public abstract int getChildCount(Element elem);

  public abstract int getChildIndex(Element parent, Element child);

  public native EventListener getEventListener(Element elem) /*-{
    return elem.__listener;
  }-*/;

  public native int getEventsSunk(Element elem) /*-{
    return elem.__eventBits || 0;
  }-*/;

  public abstract void insertChild(Element parent, Element child, int index);

  public void maybeInitializeEventSystem() {
    if (!eventSystemIsInitialized) {
      initEventSystem();
      Impl.scheduleDispose(new Disposable() {
        @Override
        public void dispose() {
          disposeEventSystem();
          cleanupDOM(DOMImpl.this);
        }
      });
      eventSystemIsInitialized = true;
    }
  }

  public abstract void releaseCapture(Element elem);

  public abstract void setCapture(Element elem);

  public native void setEventListener(Element elem, EventListener listener) /*-{
    elem.__listener = listener;
  }-*/;

  public abstract void sinkBitlessEvent(Element elem, String eventTypeName);

  public abstract void sinkEvents(Element elem, int eventBits);

  protected abstract void disposeEventSystem();

  /**
   * Initializes the event dispatch system.
   */
  protected abstract void initEventSystem();
}
