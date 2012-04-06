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

class DOMImplIE8 extends DOMImplTrident {

  private static boolean isIE8;
  private static boolean isIE8Detected;

  /**
   * Check if the browser is IE8 or IE9.
   * 
   * @return <code>true</code> if the browser is IE8, <code>false</code> if IE9
   *         or any other browser
   */
  static boolean isIE8() {
    if (!isIE8Detected) {
      isIE8 = isIE8Impl();
      isIE8Detected = true;
    }
    return isIE8;
  }

  // Stolen and modified from UserAgentPropertyGenerator
  private static native boolean isIE8Impl() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("msie") != -1 && $doc.documentMode == 8) {
      return true;
    }
    return false;
  }-*/;

  @Override
  public void cssClearOpacity(Style style) {
    if (isIE8()) {
      cssClearOpacityImpl(style);
    } else {
      super.cssClearOpacity(style);
    }
  }

  @Override
  public void cssSetOpacity(Style style, double value) {
    if (isIE8()) {
      cssSetOpacityImpl(style, value);
    } else {
      super.cssSetOpacity(style, value);
    }
  }

  @Override
  public int getAbsoluteLeft(Element elem) {
    Document doc = elem.getOwnerDocument();
    return getBoundingClientRectLeft(elem) + doc.getScrollLeft();
  }

  @Override
  public int getAbsoluteTop(Element elem) {
    Document doc = elem.getOwnerDocument();
    return getBoundingClientRectTop(elem) + doc.getScrollTop();
  }

  @Override
  public int getScrollLeft(Element elem) {
    if (isRTL(elem)) {
      // IE8 returns increasingly *positive* values as you scroll left in RTL.
      return -super.getScrollLeft(elem);
    }
    return super.getScrollLeft(elem);
  }

  @Override
  public void setScrollLeft(Element elem, int left) {
    if (isRTL(elem)) {
      // IE8 returns increasingly *positive* values as you scroll left in RTL.
      left = -left;
    }
    super.setScrollLeft(elem, left);
  }

  private native void cssClearOpacityImpl(Style style) /*-{
    style.filter = '';
  }-*/;

  private native void cssSetOpacityImpl(Style style, double value) /*-{
    style.filter = 'alpha(opacity=' + (value * 100) + ')';
  }-*/;
}
