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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Implements the clipped image as a IMG inside a custom tag because we can't
 * use the IE PNG transparency filter on background-image images.
 *
 * Do not use this class - it is used for implementation only, and its methods
 * may change in the future.
 */
public class ClippedImageImplIE6 extends ClippedImageImpl {
  
  public void adjust(Element clipper, String url, int left, int top, int width,
      int height) {

    DOM.setStyleAttribute(clipper, "width", width + "px");
    DOM.setStyleAttribute(clipper, "height", height + "px");

    // Update the nested image's url.
    Element img = DOM.getFirstChild(clipper);
    DOM.setStyleAttribute(img, "filter",
        "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + url
            + "',sizingMethod='crop')");
    DOM.setStyleAttribute(img, "marginLeft", -left + "px");
    DOM.setStyleAttribute(img, "marginTop", -top + "px");

    // AlphaImageLoader requires that we size the image explicitly.
    // It really only needs to be enough to show the revealed portion.
    int imgWidth = left + width;
    int imgHeight = top + height;
    DOM.setElementPropertyInt(img, "width", imgWidth);
    DOM.setElementPropertyInt(img, "height", imgHeight);
  }

  public String getHTML(String url, int left, int top, int width, int height) {
    String clipperStyle = "overflow: hidden; width: " + width + "px; height: "
        + height + "px; padding: 0px";

    String imgStyle =
        "filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"
        + url + "',sizingMethod='crop'); margin-left: "
        + -left + "px; margin-top: " + -top + "px" + "border: 0px";

    String clippedImgHtml = "<gwt:clipper style=\""
        + clipperStyle + "\"><img src='clear.cache.gif' style=\"" + imgStyle
        + "\" width=" + (left + width) + " height=" + (top + height)
        + " border='0'></gwt:clipper>";

    return clippedImgHtml;
  }
}
