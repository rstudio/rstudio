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
import com.google.gwt.core.client.GWT;

/**
 * Uses a combination of a clear image and a background image to clip all except
 * a desired portion of an underlying image.
 *
 * Do not use this class - it is used for implementation only, and its methods
 * may change in the future.
 */
public class ClippedImageImpl {

  public void adjust(Element img, String url, int left, int top, int width,
                     int height) {
    String style = "url(" + url + ") no-repeat " + (-left + "px ")
        + (-top + "px");
    DOM.setStyleAttribute(img, "background", style);
    DOM.setStyleAttribute(img, "width", width + "px");
    DOM.setStyleAttribute(img, "height", height + "px");
  }

  public Element createStructure(String url, int left, int top, int width,
                                 int height) {
    Element tmp = DOM.createSpan();
    DOM.setInnerHTML(tmp, getHTML(url, left, top, width, height));
    return DOM.getFirstChild(tmp);
  }

  public String getHTML(String url, int left, int top, int width, int height) {
    String style = "width: " + width + "px; height: " + height
        + "px; background: url(" + url + ") no-repeat " + (-left + "px ")
        + (-top + "px");

    String clippedImgHtml = "<img src='" + GWT.getModuleBaseURL() +
        "clear.cache.gif' style='" + style + "' border='0'>";

    return clippedImgHtml;
  }
}
