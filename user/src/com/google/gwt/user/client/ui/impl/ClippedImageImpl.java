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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Image;

/**
 * Uses a combination of a clear image and a background image to clip all except
 * a desired portion of an underlying image.
 *
 * Do not use this class - it is used for implementation only, and its methods
 * may change in the future.
 */
public class ClippedImageImpl {

  interface DraggableTemplate extends SafeHtmlTemplates {
    @SafeHtmlTemplates.Template("<img onload='this.__gwtLastUnhandledEvent=\"load\";' src='{0}' "
        + "style='{1}' border='0' draggable='true'>")
    SafeHtml image(SafeUri clearImage, SafeStyles style);
  }

  interface Template extends SafeHtmlTemplates {
    @SafeHtmlTemplates.Template("<img onload='this.__gwtLastUnhandledEvent=\"load\";' src='{0}' "
        + "style='{1}' border='0'>")
    SafeHtml image(SafeUri clearImage, SafeStyles style);
  }

  protected static final SafeUri clearImage =
    UriUtils.fromTrustedString(GWT.getModuleBaseURL() + "clear.cache.gif");
  private static Template template;
  private static DraggableTemplate draggableTemplate;

  public void adjust(Element img, SafeUri url, int left, int top, int width, int height) {
    String style = "url(\"" + url.asString() + "\") no-repeat " + (-left + "px ") + (-top + "px");
    img.getStyle().setProperty("background", style);
    img.getStyle().setPropertyPx("width", width);
    img.getStyle().setPropertyPx("height", height);
  }

  public Element createStructure(SafeUri url, int left, int top, int width, int height) {
    Element tmp = Document.get().createSpanElement();
    tmp.setInnerHTML(getSafeHtml(url, left, top, width, height).asString());
    return tmp.getFirstChildElement();
  }

  public Element getImgElement(Image image) {
    return image.getElement();
  }

  public SafeHtml getSafeHtml(SafeUri url, int left, int top, int width, int height) {
    return getSafeHtml(url, left, top, width, height, false);
  }

  public SafeHtml getSafeHtml(SafeUri url, int left, int top, int width, int height,
      boolean isDraggable) {
    SafeStylesBuilder builder = new SafeStylesBuilder();
    builder.width(width, Unit.PX).height(height, Unit.PX).trustedNameAndValue("background",
        "url(" + url.asString() + ") " + "no-repeat " + (-left + "px ") + (-top + "px"));

    if (!isDraggable) {
      return getTemplate().image(clearImage,
        SafeStylesUtils.fromTrustedString(builder.toSafeStyles().asString()));
    } else {
      return getDraggableTemplate().image(clearImage,
          SafeStylesUtils.fromTrustedString(builder.toSafeStyles().asString()));
    }
  }

  private DraggableTemplate getDraggableTemplate() {
    // no need to synchronize, JavaScript in the browser is single-threaded
    if (draggableTemplate == null) {
      draggableTemplate = GWT.create(DraggableTemplate.class);
    }
    return draggableTemplate;
  }

  private Template getTemplate() {
    // no need to synchronize, JavaScript in the browser is single-threaded
    if (template == null) {
      template = GWT.create(Template.class);
    }
    return template;
  }
}
