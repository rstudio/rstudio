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
 * Base implementation of {@link com.google.gwt.user.client.impl.DomImpl} shared
 * by those browsers that come a bit closer to supporting a common standard (ie,
 * not IE).
 */
abstract class DOMImplStandard extends DOMImpl {

  public native boolean compare(Element elem1, Element elem2) /*-{
    return (elem1 == elem2);
  }-*/;

  public native Element createInputRadioElement(String group) /*-{
    var elem = $doc.createElement("INPUT");
    elem.type = 'radio';
    elem.name = group;
    return elem;
  }-*/;

  public native Element eventGetTarget(Event evt) /*-{
    return evt.target ? evt.target : null;
  }-*/;

  public native Element eventGetToElement(Event evt) /*-{
    // Standard browsers use relatedTarget rather than toElement.
    return evt.relatedTarget ? evt.relatedTarget : null;
  }-*/;

  public native void eventPreventDefault(Event evt) /*-{
    evt.preventDefault();
  }-*/;

  public native String eventToString(Event evt) /*-{
    return evt.toString();
  }-*/;

  public native Element getChild(Element elem, int index) /*-{
    var count = 0, child = elem.firstChild;
    while (child) {
      var next = child.nextSibling;
      if (child.nodeType == 1) {
        if (index == count)
          return child;
        ++count;
      }
      child = next;
    }

    return null;
  }-*/;

  public native int getChildCount(Element elem) /*-{
    var count = 0, child = elem.firstChild;
    while (child) {
      if (child.nodeType == 1)
        ++count;
      child = child.nextSibling;
    }
    return count;
  }-*/;

  public native int getChildIndex(Element parent, Element toFind) /*-{
    var count = 0, child = parent.firstChild;
    while (child) {
      if (child == toFind)
        return count;
      if (child.nodeType == 1)
        ++count;
      child = child.nextSibling;
    }

    return -1;
  }-*/;

  public native Element getFirstChild(Element elem) /*-{
    var child = elem.firstChild;
    while (child && child.nodeType != 1)
      child = child.nextSibling;
    return child ? child : null;
  }-*/;

  public native Element getNextSibling(Element elem) /*-{
    var sib = elem.nextSibling;
    while (sib && sib.nodeType != 1)
      sib = sib.nextSibling;
    return sib ? sib : null;
  }-*/;

  public native Element getParent(Element elem) /*-{
    var parent = elem.parentNode;
    if(parent == null) {
      return null;
    }
    if (parent.nodeType != 1)
      parent = null;
    return parent ? parent : null;
  }-*/;

  public native String iframeGetSrc(Element elem) /*-{
    return elem.src;
  }-*/;

  public native void init() /*-{
    // Set up capture event dispatchers.
    $wnd.__dispatchCapturedMouseEvent = function(evt) {
      if ($wnd.__dispatchCapturedEvent(evt)) {
        var cap = $wnd.__captureElem;
        if (cap && cap.__listener) {
          @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, cap, cap.__listener);
          evt.stopPropagation();
        }
      }
    };

    $wnd.__dispatchCapturedEvent = function(evt) {
      if (!@com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)(evt)) {
        evt.stopPropagation();
        evt.preventDefault();
        return false;
      }

      return true;
    };

    $wnd.addEventListener(
      'mouseout',
      function(evt){
        var cap = $wnd.__captureElem;
        if (cap) {
          if (!evt.relatedTarget) {
            // When the mouse leaves the window during capture, release capture
            // and synthesize an 'onlosecapture' event.
            $wnd.__captureElem = null;
            if (cap.__listener) {
              var lcEvent = $doc.createEvent('UIEvent');
              lcEvent.initUIEvent('losecapture', false, false, $wnd, 0);
              @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(lcEvent, cap, cap.__listener);
            }
          }
        }
      },
      true
    );

    $wnd.addEventListener('click', $wnd.__dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('dblclick', $wnd.__dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mousedown', $wnd.__dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mouseup', $wnd.__dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('mousemove', $wnd.__dispatchCapturedMouseEvent, true);
    $wnd.addEventListener('keydown', $wnd.__dispatchCapturedEvent, true);
    $wnd.addEventListener('keyup', $wnd.__dispatchCapturedEvent, true);
    $wnd.addEventListener('keypress', $wnd.__dispatchCapturedEvent, true);

    // Set up normal event dispatcher.
    $wnd.__dispatchEvent = function(evt) {
      var listener, curElem = this;
      while (curElem && !(listener = curElem.__listener))
        curElem = curElem.parentNode;
      if (curElem && curElem.nodeType != 1)
        curElem = null;

      if (listener)
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, curElem, listener);
    };

    $wnd.__captureElem = null;
  }-*/;

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

  public native boolean isOrHasChild(Element parent, Element child) /*-{
    while (child) {
      if (parent == child)
        return true;
      child = child.parentNode;
      if (child.nodeType != 1)
        child = null;
    }
    return false;
  }-*/;

  public native void releaseCapture(Element elem) /*-{
    if (elem == $wnd.__captureElem)
      $wnd.__captureElem = null;
  }-*/;

  public native void setCapture(Element elem) /*-{
    $wnd.__captureElem = elem;
  }-*/;

  public native void sinkEvents(Element elem, int bits) /*-{
    elem.__eventBits = bits;

    elem.onclick       = (bits & 0x00001) ? $wnd.__dispatchEvent : null;
    elem.ondblclick    = (bits & 0x00002) ? $wnd.__dispatchEvent : null;
    elem.onmousedown   = (bits & 0x00004) ? $wnd.__dispatchEvent : null;
    elem.onmouseup     = (bits & 0x00008) ? $wnd.__dispatchEvent : null;
    elem.onmouseover   = (bits & 0x00010) ? $wnd.__dispatchEvent : null;
    elem.onmouseout    = (bits & 0x00020) ? $wnd.__dispatchEvent : null;
    elem.onmousemove   = (bits & 0x00040) ? $wnd.__dispatchEvent : null;
    elem.onkeydown     = (bits & 0x00080) ? $wnd.__dispatchEvent : null;
    elem.onkeypress    = (bits & 0x00100) ? $wnd.__dispatchEvent : null;
    elem.onkeyup       = (bits & 0x00200) ? $wnd.__dispatchEvent : null;
    elem.onchange      = (bits & 0x00400) ? $wnd.__dispatchEvent : null;
    elem.onfocus       = (bits & 0x00800) ? $wnd.__dispatchEvent : null;
    elem.onblur        = (bits & 0x01000) ? $wnd.__dispatchEvent : null;
    elem.onlosecapture = (bits & 0x02000) ? $wnd.__dispatchEvent : null;
    elem.onscroll      = (bits & 0x04000) ? $wnd.__dispatchEvent : null;
    elem.onload        = (bits & 0x08000) ? $wnd.__dispatchEvent : null;
    elem.onerror       = (bits & 0x10000) ? $wnd.__dispatchEvent : null;
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
}
