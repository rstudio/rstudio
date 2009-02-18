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
package com.google.gwt.dom.client;

/**
 * Safari implementation of {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplSafari extends DOMImplStandard {

  @Override
  public native NativeEvent createKeyEvent(Document doc, String type, boolean canBubble,
      boolean cancelable, boolean ctrlKey, boolean altKey, boolean shiftKey,
      boolean metaKey, int keyCode, int charCode) /*-{
    // The spec calls for KeyEvents/initKeyEvent(), but that doesn't exist on WebKit.
    var evt = doc.createEvent('HTMLEvents');
    evt.initEvent(type, canBubble, cancelable);
    evt.ctrlKey = ctrlKey;
    evt.altKey = altKey;
    evt.shiftKey = shiftKey;
    evt.metaKey = metaKey;
    evt.keyCode = keyCode;
    evt.charCode = charCode;

    return evt;
  }-*/;

  /**
   * Safari 2 does not support {@link ScriptElement#setText(String)}.
   */
  @Override
  public ScriptElement createScriptElement(String source) {
    ScriptElement elem = (ScriptElement) createElement("script");
    elem.setInnerText(source);
    return elem;
  }

  @Override
  public native int eventGetClientX(NativeEvent evt) /*-{
    // In Safari2: clientX is wrong and pageX is returned instead.
    // $wnd.devicePixelRatio identifies Safari 3 from Safari 2
    if ($wnd.devicePixelRatio) {
      return evt.clientX || 0;
    } else {
      // Subtract the margin and border of the HTML element in Safari 2 
      // TODO: Remove this code when we drop Safari 2 support
      var style = document.defaultView.getComputedStyle($doc.getElementsByTagName('html')[0], '');
      return evt.pageX - $doc.body.scrollLeft
          - parseInt(style.getPropertyValue('margin-left'))
          - parseInt(style.getPropertyValue('border-left-width')) || 0;
    }
  }-*/;

  @Override
  public native int eventGetClientY(NativeEvent evt) /*-{
    // In Safari2: clientY is wrong and pageY is returned instead.
    // $wnd.devicePixelRatio identifies Safari 3 from Safari 2
    if ($wnd.devicePixelRatio) {
      return evt.clientY || 0;
    } else {
      // Subtract the margin and border of the HTML element in Safari 2 
      // TODO: Remove this code when we drop Safari 2 support
      var style = document.defaultView.getComputedStyle($doc.getElementsByTagName('html')[0], '');
      return evt.pageY - $doc.body.scrollTop
          - parseInt(style.getPropertyValue('margin-top'))
          - parseInt(style.getPropertyValue('border-top-width')) || 0;
    }
  }-*/;

  @Override
  public native int eventGetMouseWheelVelocityY(NativeEvent evt) /*-{
    return Math.round(-evt.wheelDelta / 40) || 0;
  }-*/;

  @Override
  public native int getAbsoluteLeft(Element elem) /*-{
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetLeft.
    if (elem.offsetLeft == null) {
      return 0;
    }

    var left = 0;
    var curr = elem.parentNode;
    if (curr) {
      // This intentionally excludes body which has a null offsetParent.
      while (curr.offsetParent) {
        left -= curr.scrollLeft;

        // In RTL mode, offsetLeft is relative to the left edge of the
        // scrollable area when scrolled all the way to the right, so we need
        // to add back that difference.
        if ($doc.defaultView.getComputedStyle(curr, '').getPropertyValue('direction') == 'rtl') {
          left += (curr.scrollWidth - curr.clientWidth);
        }

        curr = curr.parentNode;
      }
    }
    
    while (elem) {
      left += elem.offsetLeft;

      // Safari 3 does not include borders with offsetLeft, so we need to add
      // the borders of the parent manually.
      var parent = elem.offsetParent;
      if (parent && $wnd.devicePixelRatio) {
        left += parseInt($doc.defaultView.getComputedStyle(parent, '').getPropertyValue('border-left-width'));
      }

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return left;
  }-*/;
  
  @Override
  public native int getAbsoluteTop(Element elem) /*-{
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetTop.
    if (elem.offsetTop == null) {
      return 0;
    }

    var top = 0;
    var curr = elem.parentNode;
    if (curr) {
      // This intentionally excludes body which has a null offsetParent.
      while (curr.offsetParent) {
        top -= curr.scrollTop;
        curr = curr.parentNode;
      }
    }
    
    while (elem) {
      top += elem.offsetTop;

      // Safari 3 does not include borders with offsetTop, so we need to add the
      // borders of the parent manually.
      var parent = elem.offsetParent;
      if (parent && $wnd.devicePixelRatio) {
        top += parseInt($doc.defaultView.getComputedStyle(parent, '').getPropertyValue('border-top-width'));
      }

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return top;
  }-*/;

  @Override
  public native boolean isOrHasChild(Element parent, Element child) /*-{
    while (child) {
      if (parent == child) {
        return true;
      }
      child = child.parentNode;
      if (child && (child.nodeType != 1)) {
        child = null;
      }
    }
    return false;
  }-*/;

  /*
   * The 'options' array cannot be used due to a bug in the version of WebKit
   * that ships with GWT (http://bugs.webkit.org/show_bug.cgi?id=10472). The
   * 'children' array, which is common for all DOM elements in Safari, does not
   * suffer from the same problem. Ideally, the 'children' array should be used
   * in all of the traversal methods in the DOM classes. Unfortunately, due to a
   * bug in Safari 2 (http://bugs.webkit.org/show_bug.cgi?id=3330), this will
   * not work. However, this bug does not cause problems in the case of <SELECT>
   * elements, because their descendent elements are only one level deep.
   */
  @Override
  public native void selectClear(SelectElement select) /*-{
    select.innerText = '';
  }-*/;

  @Override
  public native int selectGetLength(SelectElement select) /*-{
    return select.children.length;
  }-*/;

  @Override
  public native NodeList<OptionElement> selectGetOptions(SelectElement select) /*-{
    return select.children;
  }-*/;

  @Override
  public native void selectRemoveOption(SelectElement select, int index) /*-{
    select.removeChild(select.children[index]);
  }-*/;
}
