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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Image;

/**
 * Implements the clipped image as a IMG inside a custom tag because we can't
 * use the IE PNG transparency filter on background-image images.
 *
 * Do not use this class - it is used for implementation only, and its methods
 * may change in the future.
 */
public class ClippedImageImplIE6 extends ClippedImageImpl {

  private static String moduleBaseUrlProtocol =
      GWT.getHostPageBaseURL().startsWith("https") ?  "https://" : "http://";

  private static native void injectGlobalHandler() /*-{
    $wnd.__gwt_transparentImgHandler = function (elem) {
      elem.onerror = null;
      @com.google.gwt.user.client.DOM::setImgSrc(Lcom/google/gwt/user/client/Element;Ljava/lang/String;)(elem, @com.google.gwt.core.client.GWT::getModuleBaseURL()() + "clear.cache.gif");
    };
  }-*/;

  private static native boolean isIE6() /*-{
    return @com.google.gwt.dom.client.DOMImplIE6::isIE6()();
  }-*/;

  public ClippedImageImplIE6() {
    injectGlobalHandler();
  }

  @Override
  public void adjust(Element clipper, SafeUri url, int left, int top, int width, int height) {
    if (!isIE6()) {
      super.adjust(clipper, url, left, top, width, height);
      return;
    }

    clipper.getStyle().setPropertyPx("width", width);
    clipper.getStyle().setPropertyPx("height", height);

    // Update the nested image's url.
    Element img = clipper.getFirstChildElement();
    img.getStyle().setProperty("filter",
        "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + url.asString()
            + "',sizingMethod='crop')");
    img.getStyle().setPropertyPx("marginLeft", -left);
    img.getStyle().setPropertyPx("marginTop", -top);

    // AlphaImageLoader requires that we size the image explicitly.
    // It really only needs to be enough to show the revealed portion.
    int imgWidth = left + width;
    int imgHeight = top + height;
    img.setPropertyInt("width", imgWidth);
    img.setPropertyInt("height", imgHeight);
  }

  @Override
  public Element createStructure(SafeUri url, int left, int top, int width, int height) {
    if (!isIE6()) {
      return super.createStructure(url, left, top, width, height);
    }

    // We need to explicitly sink ONLOAD on the child image element, because it
    // can't be fired on the clipper.
    Element clipper = super.createStructure(url, left, top, width, height);
    Element img = clipper.getFirstChildElement();
    Event.sinkEvents(img, Event.ONLOAD);
    return clipper;
  }

  @Override
  public Element getImgElement(Image image) {
    if (!isIE6()) {
      return super.getImgElement(image);
    }
    return image.getElement().getFirstChildElement();
  }

  @Override
  public SafeHtml getSafeHtml(SafeUri url, int left, int top, int width, int height) {
    if (!isIE6()) {
      return super.getSafeHtml(url, left, top, width, height);
    }
    // We cannot use a SafeHtmlTemplates because there are parameters in javascript context.
    String clipperStyle = "overflow: hidden; width: " + width + "px; height: "
        + height + "px; padding: 0px; zoom: 1";

    String imgStyle = "filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"
        + url.asString()
        + "',sizingMethod='crop'); margin-left: "
        + -left
        + "px; margin-top: " + -top + "px; border: none";

    /*
     * We initially set the image URL to an invalid value to force onerror to be
     * fired when this string is turned into a DOM structure. At this point, we
     * can set the image src using DOM.setImgSrc, which is used to get around
     * issue #282. The invalid image URL is either http:// or https://,
     * depending on the module's base URL. We have to match the invalid image
     * URL's protocol with that of the module's base URL's protocol due to issue
     * #1200.
     */
    String clippedImgHtml = "<gwt:clipper style=\""
        + clipperStyle
        + "\"><img onload='this.__gwtLastUnhandledEvent=\"load\";' src='"
        + moduleBaseUrlProtocol
        + "' onerror='if(window.__gwt_transparentImgHandler)window.__gwt_transparentImgHandler(this);else this.src=\"" + GWT.getModuleBaseURL() + "clear.cache.gif\"' style=\""
        + imgStyle + "\" width=" + (left + width) + " height=" + (top + height)
        + " border='0'></gwt:clipper>";

    return SafeHtmlUtils.fromTrustedString(clippedImgHtml);
  }
}
