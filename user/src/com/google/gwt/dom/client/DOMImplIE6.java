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
class DOMImplIE6 extends DOMImpl {

  @Override
  public native InputElement createInputRadioElement(String name) /*-{
    return $doc.createElement("<INPUT type='RADIO' name='" + name + "'>");
  }-*/;

  /**
   * Supports creating a select control with the multiple attribute to work
   * around a bug in IE6 where changing the multiple attribute in a setAttribute
   * call can cause subsequent setSelected calls to misbehave. Although this bug
   * is fixed in IE7, this DOMImpl specialization is used for both IE6 and IE7,
   * but it should be harmless.
   */
  @Override
  public native SelectElement createSelectElement(boolean multiple) /*-{
    var html = multiple ? "<SELECT MULTIPLE>" : "<SELECT>"; 
    return $doc.createElement(html);
  }-*/;

  @Override
  public native int getAbsoluteLeft(Element elem) /*-{
    return (elem.getBoundingClientRect().left /
      this.@com.google.gwt.dom.client.DOMImplIE6::getZoomMultiple()()) +
      @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollLeft;
  }-*/;

  @Override
  public native int getAbsoluteTop(Element elem) /*-{
    return (elem.getBoundingClientRect().top /
      this.@com.google.gwt.dom.client.DOMImplIE6::getZoomMultiple()()) +
      @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.scrollTop;
  }-*/;

  public native int getBodyOffsetLeft() /*-{
    return @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientLeft;
  }-*/;

  public native int getBodyOffsetTop() /*-{
    return @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientTop;
  }-*/;

  @Override
  public native String getInnerText(Element elem) /*-{
    return elem.innerText;
  }-*/;

  @Override
  public native Element getParentElement(Element elem) /*-{
    return elem.parentElement;
  }-*/;

  /*
   * The src may not be set yet because of funky logic in setImgSrc(). See
   * setImgSrc().
   */
  @Override
  public String imgGetSrc(Element img) {
    return ImageSrcIE6.getImgSrc(img);
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
    ImageSrcIE6.setImgSrc(img, src);
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

  /**
   * Get the zoom multiple based on the current IE zoom level.  A multiple of
   * 2.0 means that the user has zoomed in to 200%.
   * 
   * @return the zoom multiple
   */
  @SuppressWarnings("unused")
  private native double getZoomMultiple() /*-{
    return $doc.body.parentElement.offsetWidth / $doc.body.offsetWidth;
  }-*/;
}
