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
package com.google.gwt.dom.builder.shared;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Builds an iframe element.
 */
public interface IFrameBuilder extends ElementBuilderBase<IFrameBuilder> {

  /**
   * Request frame borders.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-frameborder">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder frameBorder(int frameBorder);

  /**
   * Throws {@link UnsupportedOperationException}.
   * 
   * <p>
   * Appending children or content directly to an iframe isn't supported. You
   * must use the src attribute to specify the url of the content to load, or
   * wait until the document is loaded.
   * </p>
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  IFrameBuilder html(SafeHtml html);

  /**
   * Frame margin height, in pixels.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginheight">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder marginHeight(int marginHeight);

  /**
   * Frame margin width, in pixels.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginwidth">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder marginWidth(int marginWidth);

  /**
   * The frame name (object of the target attribute).
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-name-FRAME">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder name(String name);

  /**
   * Forbid user from resizing frame.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-noresize">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder noResize();

  /**
   * Specify whether or not the frame should have scrollbars.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-scrolling">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder scrolling(String scrolling);

  /**
   * A URI designating the initial frame contents.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder src(SafeUri src);

  /**
   * A URI designating the initial frame contents.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C
   *      HTML Specification</a>
   */
  IFrameBuilder src(String src);

  /**
   * Throws {@link UnsupportedOperationException}.
   * 
   * <p>
   * Appending children or content directly to an iframe isn't supported. You
   * must use the src attribute to specify the url of the content to load, or
   * wait until the document is loaded.
   * </p>
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  IFrameBuilder text(String html);
}
