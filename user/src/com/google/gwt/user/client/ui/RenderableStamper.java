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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.builder.shared.HtmlElementBuilderBase;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Used by {@link IsRenderable} to mark their root element in such a way that
 * they can be later retrieved. This class abstracts the exact details of how
 * the element is marked and retrieved, so that we can always use the best
 * method avaialable without having to change all implementations of
 * {@link IsRenderable}.
 * <p>
 * The expected flow is for the {@link IsRenderable} object to use one of the
 * {@link #stamp} methods below to mark their HTML. At a later point, its parent
 * widget will use the {@link #findStampedElement} to retrieve the right element.
 */
public class RenderableStamper {

  // The token used to stamp IsRenderable objects.
  private final String token;

  /**
   * Creates a stamper that will be use the given token, which is assumed
   * to be unique and will be escaped before being used.
   */
  public RenderableStamper(String token) {
    this.token = SafeHtmlUtils.htmlEscape(token);
  }

  /**
   * Finds the element that was previously stamped in the DOM.
   * For this to work properly the element must be attached to the document.
   */
  public Element findStampedElement() {
    // TODO(rdcastro): Add a DEV-only check to make sure the element is attached.
    return Document.get().getElementById(token);
  }

  /**
   * Stamps an HTML element in such a way that it can be later found in the DOM tree.
   * To be used by {@link IsRenderable} objects built using {@link SafeHtml} directly, this assumes
   * the element to be stamped is the first found in the given {@link SafeHtml}.
   * Returns safeHtml untouched if it does not being with a tag.
   */
  public SafeHtml stamp(SafeHtml safeHtml) {
    String html = safeHtml.asString().trim();
    if (!html.startsWith("<")) {
      return safeHtml;
    }

    int endOfFirstTag = html.indexOf('>');
    // TODO(rdcastro): Maybe add a DEV-only check to make sure endOfFirstTag != -1
    if (html.charAt(endOfFirstTag - 1) == '/') {
      endOfFirstTag--;
    }
    StringBuilder htmlBuilder = new StringBuilder()
        .append(html.substring(0, endOfFirstTag))
        .append(" id ='")
        .append(token)
        .append("'")
        .append(html.substring(endOfFirstTag));
    return SafeHtmlUtils.fromTrustedString(htmlBuilder.toString());
  }

  /**
   * Stamps an HTML element in such a way that it can be later found in the DOM tree.
   * To be used by {@link IsRenderable} objects built using ElementBuilder, this assumes
   * the given elementBuilder is for the root element that should later be claimed.
   */
  public <T extends HtmlElementBuilderBase<?>> T stamp(T elementBuilder) {
    elementBuilder.id(token);
    return elementBuilder;
  }
}
