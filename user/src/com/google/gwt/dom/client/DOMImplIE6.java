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
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplIE6 extends DOMImplTrident {

  private static boolean isIE6;
  private static boolean isIE6Detected;

  /**
   * Check if the browser is IE6 or IE7.
   * 
   * @return <code>true</code> if the browser is IE6, <code>false</code> if IE7
   *         or any other browser
   */
  static boolean isIE6() {
    if (!isIE6Detected) {
      isIE6 = isIE6Impl();
      isIE6Detected = true;
    }
    return isIE6;
  }

  // Stolen and modified from UsernmAgent.gwt.xml.
  // TODO(jgw): Get rid of this method, and switch to using soft permutations
  // once they land in trunk.
  private static native boolean isIE6Impl() /*-{
    function makeVersion(result) {
      return (parseInt(result[1]) * 1000) + parseInt(result[2]);
    }

    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("msie") != -1) {
      var result = /msie ([0-9]+)\.([0-9]+)/.exec(ua);
      if (result && result.length == 3) {
        var v = makeVersion(result);
        if (v < 7000) {
          return true;
        }
      }
    }

    return false;
  }-*/;

  @Override
  public native void cssClearOpacity(Style style) /*-{
    style.filter = '';
  }-*/;

  @Override
  public native void cssSetOpacity(Style style, double value) /*-{
    style.filter = 'alpha(opacity=' + (value * 100) + ')';
  }-*/;

  @Override
  public int getAbsoluteLeft(Element elem) {
    Document doc = elem.getOwnerDocument();
    return (int) Math.floor(getBoundingClientRectLeft(elem)
        / getZoomMultiple(doc) + doc.getScrollLeft());
  }

  @Override
  public int getAbsoluteTop(Element elem) {
    Document doc = elem.getOwnerDocument();
    return (int) Math.floor(getBoundingClientRectTop(elem)
        / getZoomMultiple(doc) + doc.getScrollTop());
  }

  @Override
  public int getScrollLeft(Element elem) {
    if (isRTL(elem)) {
      return super.getScrollLeft(elem) - (elem.getScrollWidth() - elem.getClientWidth());
    }
    return super.getScrollLeft(elem);
  }

  /*
   * The src may not be set yet because of funky logic in setImgSrc(). See
   * setImgSrc().
   */
  @Override
  public String imgGetSrc(Element img) {
    if (isIE6()) {
      return ImageSrcIE6.getImgSrc(img);
    } else {
      return super.imgGetSrc(img);
    }
  }

  /**
   * Works around an IE problem where multiple images trying to load at the same
   * time will generate a request per image. We fix this by only allowing the
   * first image of a given URL to set its source immediately, but simultaneous
   * requests for the same URL don't actually get their source set until the
   * original load is complete.
   */
  @Override
  public void imgSetSrc(Element img, String src) {
    if (isIE6()) {
      ImageSrcIE6.setImgSrc(img, src);
    } else {
      super.imgSetSrc(img, src);
    }
  }

  @Override
  public void setScrollLeft(Element elem, int left) {
    if (isRTL(elem)) {
      left += elem.getScrollWidth() - elem.getClientWidth();
    }
    super.setScrollLeft(elem, left);
  }

  /**
   * Get the zoom multiple based on the current IE zoom level. A multiple of 2.0
   * means that the user has zoomed in to 200%.
   * 
   * @return the zoom multiple
   */
  private double getZoomMultiple(Document doc) {
    if (doc.getCompatMode().equals("CSS1Compat")) {
      return 1;
    } else {
      // TODO(FINDBUGS): is integer division correct?
      int bodyOffset = doc.getBody().getOffsetWidth();
      return bodyOffset == 0 ? 1
          : doc.getBody().getParentElement().getOffsetWidth() / bodyOffset;
    }
  }
}
