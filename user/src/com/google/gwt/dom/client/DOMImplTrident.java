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
package com.google.gwt.dom.client;

abstract class DOMImplTrident extends DOMImpl {

  /**
   * This field *must* be filled in from JSNI code before dispatching an event
   * on IE. It should be set to the 'this' context of the handler that receives
   * the event, then restored to its initial value when the dispatcher is done.
   * See {@link com.google.gwt.user.client.impl.DOMImplTrident#initEventSystem()}
   * for an example of how this should be done.
   */
  private static EventTarget currentEventTarget;

  @Override
  public native NativeEvent createHtmlEvent(Document doc, String type, boolean canBubble,
      boolean cancelable) /*-{
    // NOTE: IE doesn't support changing bubbling and canceling behavior (this
    // is documented publicly in Document.createHtmlEvent()).
    var evt = doc.createEventObject();
    evt.type = type;
    return evt;
  }-*/;

  @Override
  public native InputElement createInputRadioElement(Document doc, String name) /*-{
    return doc.createElement("<INPUT type='RADIO' name='" + name + "'>");
  }-*/;

  @Override
  public native NativeEvent createKeyEvent(Document doc, String type, boolean canBubble,
      boolean cancelable, boolean ctrlKey, boolean altKey, boolean shiftKey,
      boolean metaKey, int keyCode, int charCode) /*-{
    // NOTE: IE doesn't support changing bubbling and canceling behavior (this
    // is documented publicly in Document.createKeyEvent()).
    var evt = doc.createEventObject();
    evt.type = type;
    evt.ctrlKey = ctrlKey;
    evt.altKey = altKey;
    evt.shiftKey = shiftKey;
    evt.metaKey = metaKey;
    evt.keyCode = keyCode;
    evt.charCode = charCode;

    return evt;
  }-*/;

  @Override
  public native NativeEvent createMouseEvent(Document doc, String type, boolean canBubble,
      boolean cancelable, int detail, int screenX, int screenY, int clientX,
      int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey,
      boolean metaKey, int button, Element relatedTarget) /*-{
    // NOTE: IE doesn't support changing bubbling and canceling behavior (this
    // is documented publicly in Document.createMouseEvent()).
    var evt = doc.createEventObject();
    evt.type = type;
    evt.detail = detail;
    evt.screenX = screenX;
    evt.screenY = screenY;
    evt.clientX = clientX;
    evt.clientY = clientY;
    evt.ctrlKey = ctrlKey;
    evt.altKey = altKey;
    evt.shiftKey = shiftKey;
    evt.metaKey = metaKey;
    evt.button = button;

    // It would make sense to set evt.[fromElement | toElement] here, because
    // that's what IE uses. However, setting these properties has no effect for
    // some reason. So instead we set releatedTarget, and explicitly check for
    // its existence in eventGetFromElement() and eventGetToElement().
    evt.relatedTarget = relatedTarget;

    return evt;
  }-*/;

  /**
   * Supports creating a select control with the multiple attribute to work
   * around a bug in IE6 where changing the multiple attribute in a setAttribute
   * call can cause subsequent setSelected calls to misbehave. Although this bug
   * is fixed in IE7, this DOMImpl specialization is used for both IE6 and IE7,
   * but it should be harmless.
   */
  @Override
  public native SelectElement createSelectElement(Document doc, boolean multiple) /*-{
    var html = multiple ? "<SELECT MULTIPLE>" : "<SELECT>"; 
    return doc.createElement(html);
  }-*/;

  public native void dispatchEvent(Element target, NativeEvent evt) /*-{
    target.fireEvent("on" + evt.type, evt);
  }-*/;

  @Override
  public EventTarget eventGetCurrentTarget(NativeEvent event) {
    return currentEventTarget;
  }

  @Override
  public native int eventGetMouseWheelVelocityY(NativeEvent evt) /*-{
    return Math.round(-evt.wheelDelta / 40) || 0;
  }-*/;

  @Override
  public native EventTarget eventGetRelatedTarget(NativeEvent evt) /*-{
    // Prefer 'relatedTarget' if it's set (see createMouseEvent(), which
    // explicitly sets relatedTarget when synthesizing mouse events).
    return evt.relatedTarget ||
           (evt.type == "mouseout" ? evt.toElement:evt.fromElement);
  }-*/;

  @Override
  public native EventTarget eventGetTarget(NativeEvent evt) /*-{
    return evt.srcElement;
  }-*/;

  @Override
  public native void eventPreventDefault(NativeEvent evt) /*-{
    evt.returnValue = false;
  }-*/;

  @Override
  public native void eventStopPropagation(NativeEvent evt) /*-{
    evt.cancelBubble = true;
  }-*/;

  @Override
  public native String eventToString(NativeEvent evt) /*-{
    if (evt.toString) return evt.toString();
    return "[event" + evt.type + "]";
  }-*/;

  @Override
  public int getAbsoluteLeft(Element elem) {
    return getBoundingClientRectLeft(elem);
  }

  @Override
  public int getAbsoluteTop(Element elem) {
    return getBoundingClientRectTop(elem);
  }

  /**
   * IE returns a numeric type for some attributes that are really properties,
   * such as offsetWidth.  We need to coerce these to strings to prevent a
   * runtime JS exception.
   */
  @Override
  public native String getAttribute(Element elem, String name) /*-{
    var attr = elem.getAttribute(name);
    return attr == null ? '' : attr + '';
  }-*/;

  @Override
  public int getBodyOffsetLeft(Document doc) {
    return getClientLeft(doc.getViewportElement());
  }

  @Override
  public int getBodyOffsetTop(Document doc) {
    return getClientTop(doc.getViewportElement());
  }

  @Override
  public native String getInnerText(Element elem) /*-{
    return elem.innerText;
  }-*/;

  @Override
  public native Element getParentElement(Element elem) /*-{
    return elem.parentElement;
  }-*/;

  @Override
  public int getScrollLeft(Element elem) {
    if (isRTL(elem)) {
      // IE8 returns increasingly *positive* values as you scroll left in RTL.
      return -super.getScrollLeft(elem);
    }
    return super.getScrollLeft(elem);
  }

  @Override
  public native boolean isOrHasChild(Element parent, Element child) /*-{
    // An extra equality check is required due to the fact that
    // elem.contains(elem) is false if elem is not attached to the DOM.
    return (parent === child) || parent.contains(child);
  }-*/;

  @Override
  public native void selectAdd(SelectElement select, OptionElement option,
      OptionElement before) /*-{
    // IE only accepts indices for the second argument.
    if (before) {
      select.add(option, before.index);
    } else {
      select.add(option);
    }
  }-*/;

  @Override
  public native void setInnerText(Element elem, String text) /*-{
    elem.innerText = text || '';
  }-*/;

  @Override
  public void setScrollLeft(Element elem, int left) {
    if (isRTL(elem)) {
      // IE8 returns increasingly *positive* values as you scroll left in RTL.
      left = -left;
    }
    super.setScrollLeft(elem, left);
  }

  private native int getBoundingClientRectLeft(Element elem) /*-{
    // getBoundingClientRect() throws a JS exception if the elem is not attached
    // to the document, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().left;
    } catch (e) {
      return 0;
    }
  }-*/;

  private native int getBoundingClientRectTop(Element elem) /*-{
    // getBoundingClientRect() throws a JS exception if the elem is not attached
    // to the document, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().top;
    } catch (e) {
      return 0;
    }
  }-*/;

  /**
   * clientLeft is non-standard and not implemented on all browsers.
   */
  private native int getClientLeft(Element elem) /*-{
    return elem.clientLeft;
  }-*/;

  /**
   * clientTop is non-standard and not implemented on all browsers.
   */
  private native int getClientTop(Element elem) /*-{
    return elem.clientTop;
  }-*/;

  protected native boolean isRTL(Element elem) /*-{
    return elem.currentStyle.direction == 'rtl';
  }-*/;
}
