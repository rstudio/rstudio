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

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * An interface for UI elements that can be built by first generating a piece
 * of HTML and afterwards wrapping a root widget.
 *
 * This interface is very experimental and in active development, so the exact
 * API is likely to change. Very likely. In fact, it will definitely change.
 * You've been warned.
 */
public interface IsRenderable {

  /**
   * Perform any initialization needed when the widget is not attached to
   * the document. Assumed to be called after {@link #wrapElement}.
   */
  void performDetachedInitialization();

  /**
   * @see #render(String, SafeHtmlBuilder)
   * TODO(rdcastro): Remove this once UiBinder doesn't rely on it anymore.
   */
  SafeHtml render(String id);

  /**
   * Tells this object to render itself as HTML and append it to the given builder.
   * The root element of the HTML must be identifies by the given id.
   */
  void render(String id, SafeHtmlBuilder builder);

  /**
   * Replace the previous contents of the receiver with the given element,
   * presumed to have been created via a previous call to {@link #render}.
   * Assumes the element is attached to the document.
   */
  void wrapElement(Element element);
}
