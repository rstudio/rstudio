/*
 * Copyright 2007 Google Inc.
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
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplIE6 extends DOMImpl {

  /**
   * A native map of image source URL strings to Image objects. All Image
   * objects with values in this map are waiting on an asynchronous load to
   * complete and have event handlers hooked. The moment the image finishes
   * loading, it will be removed from this map.
   */
  JavaScriptObject srcImgMap;

  public native boolean compare(Element elem1, Element elem2) /*-{
    if (!elem1 && !elem2)
      return true;
    else if (!elem1 || !elem2)
      return false;
    return (elem1.uniqueID == elem2.uniqueID);
  }-*/;

  public native Element createInputRadioElement(String group) /*-{
    return $doc.createElement("<INPUT type='RADIO' name='" + group + "'>");
  }-*/;

  /**
   * Supports creating a select control with the multiple attribute to work
   * around a bug in IE6 where changing the multiple attribute in a
   * setAttribute call can cause subsequent setSelected calls to misbehave.
   * Although this bug is fixed in IE7, this DOMImpl specialization is used
   * for both IE6 and IE7, but it should be harmless.
   */
  public native Element createSelectElement(boolean multiple) /*-{
    var html = multiple ? "<SELECT MULTIPLE>" : "<SELECT>"; 
    return $doc.createElement(html);
  }-*/;

  public native Element eventGetFromElement(Event evt) /*-{
    return evt.fromElement ? evt.fromElement : null;
  }-*/;

  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return -evt.wheelDelta / 40;
  }-*/;

  public native Element eventGetTarget(Event evt) /*-{
    return evt.srcElement || null;
  }-*/;

  public native Element eventGetToElement(Event evt) /*-{
    return evt.toElement || null;
  }-*/;

  public native void eventPreventDefault(Event evt) /*-{
    evt.returnValue = false;
  }-*/;

  public native String eventToString(Event evt) /*-{
    if (evt.toString) return evt.toString();
      return "[object Event]";
  }-*/;

  public native int getAbsoluteLeft(Element elem) /*-{
    // Standard mode || Quirks mode.
    var scrollLeft = $doc.documentElement.scrollLeft || $doc.body.scrollLeft;

    // Standard mode use $doc.documentElement.clientLeft (= always 2)
    // Quirks mode use $doc.body.clientLeft (=BODY border width)
    return (elem.getBoundingClientRect().left + scrollLeft)
        - ($doc.documentElement.clientLeft || $doc.body.clientLeft);
  }-*/;

  public native int getAbsoluteTop(Element elem) /*-{
    // Standard mode || Quirks mode.
    var scrollTop = $doc.documentElement.scrollTop || $doc.body.scrollTop;

    // Standard mode use $doc.documentElement.clientTop (= always 2)
    // Quirks mode use $doc.body.clientTop (= BODY border width)
    return (elem.getBoundingClientRect().top +  scrollTop)
        - ($doc.documentElement.clientTop || $doc.body.clientTop);
   }-*/;

  public native Element getChild(Element elem, int index) /*-{
    var child = elem.children[index];
    return child || null;
  }-*/;

  public native int getChildCount(Element elem) /*-{
    return elem.children.length;
  }-*/;

  public native int getChildIndex(Element parent, Element child) /*-{
    var count = parent.children.length;
    for (var i = 0; i < count; ++i) {
      if (child.uniqueID == parent.children[i].uniqueID)
        return i;
    }
    return -1;
  }-*/;

  public native Element getFirstChild(Element elem) /*-{
    var child = elem.firstChild;
    return child || null;
  }-*/;

  /*
   * The src may not be set yet because of funky logic in setImgSrc(). See
   * setImgSrc().
   */
  public native String getImgSrc(Element img) /*-{
    return img.__targetSrc || img.src;
  }-*/;

  public native String getInnerText(Element elem) /*-{
    var ret = elem.innerText;
    return (ret == null) ? null : ret;
  }-*/;

  public native Element getNextSibling(Element elem) /*-{
    var sib = elem.nextSibling;
    return sib || null;
  }-*/;

  public native Element getParent(Element elem) /*-{
    var parent = elem.parentElement;
    return parent || null;
  }-*/;

  public native String iframeGetSrc(Element elem) /*-{
    return elem.src;
  }-*/;

  public native void init() /*-{
    // Fix IE background image refresh bug, present through IE6
    // see http://www.mister-pixel.com/#Content__state=is_that_simple
    // this only works with IE6 SP1+
    try {
      $doc.execCommand("BackgroundImageCache", false, true);
    } catch (e) {
      // ignore error on other browsers
    }
  
    // Initialize the URL -> Image map.
    this.@com.google.gwt.user.client.impl.DOMImplIE6::srcImgMap = {};

    // Set up event dispatchers.
    $wnd.__dispatchEvent = function() {
      if ($wnd.event.returnValue == null) {
        $wnd.event.returnValue = true;
        if (!@com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)($wnd.event))
          return;
      }

      if (this.__listener)
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)($wnd.event, this, this.__listener);
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

  public native void insertChild(Element parent, Element child, int index) /*-{
    if (index >= parent.children.length)
      parent.appendChild(child);
    else
      parent.insertBefore(child, parent.children[index]);
  }-*/;

  public native void insertListItem(Element select, String text, String value,
      int index) /*-{
    // When we try to pass the populated option into this method, IE
    // chokes, so we create the option here instead.
    var newOption = document.createElement("Option");
    if (index == -1) {
      select.add(newOption);
    } else{
      select.add(newOption, index);
    }
    newOption.text = text;   // assumed not null
    newOption.value = value; // assumed not null
  }-*/;

  public native boolean isOrHasChild(Element parent, Element child) /*-{
    while (child) {
      if (parent.uniqueID == child.uniqueID)
        return true;
      child = child.parentElement;
    }
    return false;
  }-*/;

  public native void releaseCapture(Element elem) /*-{
    elem.releaseCapture();
  }-*/; 
  
  public native void setCapture(Element elem) /*-{
    elem.setCapture();
  }-*/;

  /*
   * Works around an IE problem where multiple images trying to load at the same
   * time will generate a request per image. We fix this by only allowing the
   * first image of a given URL to set its source immediately, but simultaneous
   * requests for the same URL don't actually get their source set until the
   * original load is complete.
   * 
   * See comment on srcImgMap for the invariants that hold for the synthetic
   * Image objects which are values in the map.
   */
  public native void setImgSrc(Element img, String src) /*-{
    // Grab the URL -> Image map.
    var map = this.@com.google.gwt.user.client.impl.DOMImplIE6::srcImgMap;
    
    // See if there's already an image for this URL in the map (e.g. loading).
    var queuedImg = map[src];
    if (queuedImg) {
      // just add myself to its array, it will set my source later
      queuedImg.__kids.push(img);
      // record the desired src so it can be retreived synchronously
      img.__targetSrc = src;
      return; 
    }

    // No outstanding requests; load the image.
    img.src = src;

    // If the image was in cache, the load may have just happened synchronously.
    if (img.complete) {
      // We're done
      return;
    }

    // Image is loading asynchronously; put in map for chaining.
    var kids = img.__kids = [];
    map[src] = img;

    // Store all the old handlers
    var _onload = img.onload, _onerror = img.onerror, _onabort = img.onabort;

    // Same cleanup code matter what state we end up in.
    img.onload = function() {
      finish("onload");
    }
    img.onerror = function() {
      finish("onerror");
    }
    img.onabort = function() {
      finish("onabort");
    }

    function finish(whichEvent) {
      // Clean up: restore event handlers and remove from map.
      img.onload = _onload; img.onerror = _onerror; img.onabort = _onabort;
      delete map[src];
      
      // Set the source for all kids in a timer to ensure caching has happened.
      window.setTimeout(function() {
        for (var i = 0; i < kids.length; ++i) {
          kids[i].src = img.src;
          // clear the target src now that it's resolved
          kids[i].__targetSrc = null;
        }
      }, 0);      
      
      // call the original handler, if any
      if (img[whichEvent]) {
        img[whichEvent]();
      }
    }
  }-*/;

  public native void setInnerText(Element elem, String text) /*-{
    if (!text)
      text = '';
    elem.innerText = text;
  }-*/;

  public native void sinkEvents(Element elem, int bits) /*-{
    elem.__eventBits = bits;

    elem.onclick       = (bits & 0x00001) ? $wnd.__dispatchEvent : null;
    elem.ondblclick    = (bits & 0x00002) ? $wnd.__dispatchDblClickEvent : null;
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
    elem.onmousewheel  = (bits & 0x20000) ? $wnd.__dispatchEvent : null;
  }-*/;
}
