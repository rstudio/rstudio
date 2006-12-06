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
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplIE6 extends DOMImpl {

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

  public native Element eventGetTarget(Event evt) /*-{
    var elem = evt.srcElement;
    return elem ? elem : null;
  }-*/;

  public native Element eventGetToElement(Event evt) /*-{
    return evt.toElement ? evt.toElement : null;
  }-*/;

  public native void eventPreventDefault(Event evt) /*-{
    evt.returnValue = false;
  }-*/;

  public native String eventToString(Event evt) /*-{
    if (evt.toString) return evt.toString();
      return "[object Event]";
  }-*/;

  public native int getAbsoluteLeft(Element elem) /*-{
    // Standard mode used documentElement.scrollLeft. Quirks mode uses 
    // document.body.scrollLeft. So we take the max of the two.  
    var scrollLeft = $doc.documentElement.scrollLeft;
    if(scrollLeft == 0){
      scrollLeft = $doc.body.scrollLeft
    }
    
    // Offset needed as IE starts the window's upper left at
    // 2,2 rather than 0,0.
    return (elem.getBoundingClientRect().left + scrollLeft) - 2;
  }-*/;

  public native int getAbsoluteTop(Element elem) /*-{
    // Standard mode used documentElement.scrollTop. Quirks mode uses 
    // document.body.scrollTop. So we take the max of the two.
    var scrollTop = $doc.documentElement.scrollTop;
    if(scrollTop == 0){
      scrollTop = $doc.body.scrollTop
    } 
    
    // Offset needed as IE starts the window's upper left as 2,2 
    // rather than 0,0.
    return (elem.getBoundingClientRect().top +  scrollTop) - 2;
   }-*/;

  public native Element getChild(Element elem, int index) /*-{
    var child = elem.children[index];
    return child ? child : null;
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
    return child ? child : null;
  }-*/;

  public native String getInnerText(Element elem) /*-{
    var ret = elem.innerText;
    return (ret == null) ? null : ret;
  }-*/;

  public native Element getNextSibling(Element elem) /*-{
    var sib = elem.nextSibling;
    return sib ? sib : null;
  }-*/;

  public native Element getParent(Element elem) /*-{
    var parent = elem.parentElement;
    return parent ? parent : null;
  }-*/;

  public native String iframeGetSrc(Element elem) /*-{
    return elem.src;
  }-*/;

  public native void init() /*-{
    // Set up event dispatchers.
    $wnd.__dispatchEvent = function() {
      if ($wnd.event.returnValue == null) {
        $wnd.event.returnValue = true;
        if (!@com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)($wnd.event))
          return;
      }
  
      var listener, curElem = this;
      while (curElem && !(listener = curElem.__listener))
      curElem = curElem.parentElement;
  
      if (listener)
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)($wnd.event, curElem, listener);
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
    $doc.body.onkeydown     =
    $doc.body.onkeypress    =
    $doc.body.onkeyup       =
    $doc.body.onfocus       =
    $doc.body.onblur        =
    $doc.body.ondblclick    = $wnd.__dispatchEvent;
  }-*/;

  public native void insertChild(Element parent, Element child, int index) /*-{
    if (index == parent.children.length)
      parent.appendChild(child);
    else
      parent.insertBefore(child, parent.children[index]);
  }-*/;

  public native void insertListItem(Element select, String text, String value,
      int index) /*-{
    // When we try to pass the populated option into this method, IE
    // chokes, so we create the option here instead.
    var newOption = document.createElement("Option");
    if(index==-1){
      select.add(newOption);
    } else{
      select.add(newOption,index);
    }
    newOption.text=text;
    newOption.value=value;
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
  }-*/;

  public native String toString(Element elem) /*-{
    return elem.outerHTML;
  }-*/;
}
