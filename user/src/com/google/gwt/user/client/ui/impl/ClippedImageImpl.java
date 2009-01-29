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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Image;

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
    img.getStyle().setProperty("background", style);
    img.getStyle().setPropertyPx("width", width);
    img.getStyle().setPropertyPx("height", height);
  }

  public Element createStructure(String url, int left, int top, int width,
                                 int height) {
    Element tmp = Document.get().createSpanElement();
    tmp.setInnerHTML(getHTML(url, left, top, width, height));
    return tmp.getFirstChildElement();
  }

  public String getHTML(String url, int left, int top, int width, int height) {
    String style = "width: " + width + "px; height: " + height
        + "px; background: url(" + url + ") no-repeat " + (-left + "px ")
        + (-top + "px");

    String clippedImgHtml = "<img src='" + GWT.getModuleBaseURL() +
        "clear.cache.gif' style='" + style + "' border='0'>";

    return clippedImgHtml;
  }

  public void fireSyntheticLoadEvent(final Image image) {
    /*
     * We need to synthesize a load event, because the native events that are
     * fired would correspond to the loading of clear.cache.gif, which is
     * incorrect. A native event would not even fire in Internet Explorer,
     * because the root element is a wrapper element around the <img> element.
     * Since we are synthesizing a load event, we do not need to sink the
     * onload event.
     * 
     * We use a deferred command here to simulate the native version of the
     * load event as closely as possible. In the native event case, it is
     * unlikely that a second load event would occur while you are in the load
     * event handler.
     */
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        NativeEvent evt = Document.get().createLoadEvent();
        image.getElement().dispatchEvent(evt);
      }
    });
  }
}
