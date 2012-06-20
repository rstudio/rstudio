/*
 * Copyright 2011 Google Inc.
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
 * IE9 based implementation of {@link com.google.gwt.dom.client.DOMImplStandardBase}.
 */
class DOMImplIE9 extends DOMImplStandardBase {

  @Override
  public int getAbsoluteLeft(Element elem) {
    int left = getBoundingClientRectLeft(elem) + getDocumentScrollLeftImpl();
    if (isRTL(elem)) { // in RTL, account for the scroll bar shift if present
      left += getParentOffsetDelta(elem);
    }
    return left;
  }

  @Override
  public int getAbsoluteTop(Element elem) {
    return getBoundingClientRectTop(elem) + getDocumentScrollTopImpl();
  }

  /**
   * Coerce numeric values a string. In IE, some values can be stored as numeric
   * types.
   */
  @Override
  public native String getNumericStyleProperty(Style style, String name) /*-{
    return typeof(style[name]) == "number" ? "" + style[name] : style[name];
  }-*/;

  @Override
  public int getScrollLeft(Document doc) {
    return getDocumentScrollLeftImpl();
  }

  @Override
  public int getScrollLeft(Element elem) {
    int left = getScrollLeftImpl(elem);
    if (isRTL(elem)) {
      left = -left;
    }
    return left;
  }

  @Override
  public int getScrollTop(Document doc) {
    return getDocumentScrollTopImpl();
  }

  @Override
  public native int getTabIndex(Element elem) /*-{ 
    return elem.tabIndex < 65535 ? elem.tabIndex : -(elem.tabIndex % 65535) - 1;
  }-*/;

  @Override
  public boolean isOrHasChild(Node parent, Node child) {
    // IE9 still behaves like IE6-8 for this method
    return DOMImplTrident.isOrHasChildImpl(parent, child);
  }

  @Override
  public native void selectRemoveOption(SelectElement select, int index) /*-{
    try {
      // IE9 throws if elem at index is an optgroup
      select.remove(index);
    } catch(e) {
      select.removeChild(select.childNodes[index]);
    }
  }-*/;

  @Override
  public void setScrollLeft(Element elem, int left) {
    if (isRTL(elem)) {
      left = -left;
    }
    setScrollLeftImpl(elem, left);
  }

  protected native int getBoundingClientRectLeft(Element elem) /*-{
  // getBoundingClientRect() throws a JS exception if the elem is not attached
  // to the document, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().left;
    } catch (e) {
      // if not attached return 0
      return 0;
    }
  }-*/;

  protected native int getBoundingClientRectTop(Element elem) /*-{
    // getBoundingClientRect() throws a JS exception if the elem is not attached
    // to the document, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().top;
    } catch (e) {
      // if not attached return 0
      return 0;
    }
  }-*/;

  private native int getDocumentScrollLeftImpl() /*-{
    return $wnd.pageXOffset;
  }-*/;

  private native int getDocumentScrollTopImpl() /*-{
    return $wnd.pageYOffset;
  }-*/;

  private native int getParentOffsetDelta(Element elem) /*-{
    var offsetParent = elem.offsetParent;
    if (offsetParent) {
      return offsetParent.offsetWidth - offsetParent.clientWidth;
    }
    return 0;
  }-*/;

  private native int getScrollLeftImpl(Element elem) /*-{
    return elem.scrollLeft || 0;
  }-*/; 

  private native void setScrollLeftImpl(Element elem, int left) /*-{
    elem.scrollLeft = left;
  }-*/; 
}
