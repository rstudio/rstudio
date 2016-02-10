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

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;

/**
 * Builds an frame element.
 */
public interface FrameBuilder extends ElementBuilderBase<FrameBuilder> {

  /**
   * Request frame borders.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-frameborder">W3C
   *      HTML Specification</a>
   */
  FrameBuilder frameBorder(int frameBorder);

  /**
   * URI designating a long description of this image or frame.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C
   *      HTML Specification</a>
   */
  FrameBuilder longDesc(SafeUri longDesc);

  /**
   * URI designating a long description of this image or frame.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C
   *      HTML Specification</a>
   */
  FrameBuilder longDesc(String longDesc);

  /**
   * Frame margin height, in pixels.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginheight">W3C
   *      HTML Specification</a>
   */
  FrameBuilder marginHeight(int marginHeight);

  /**
   * Frame margin width, in pixels.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginwidth">W3C
   *      HTML Specification</a>
   */
  FrameBuilder marginWidth(int marginWidth);

  /**
   * The frame name (object of the target attribute).
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-name-FRAME">W3C
   *      HTML Specification</a>
   */
  FrameBuilder name(String name);

  /**
   * Forbid user from resizing frame.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-noresize">W3C
   *      HTML Specification</a>
   */
  FrameBuilder noResize();

  /**
   * Specify whether or not the frame should have scrollbars.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-scrolling">W3C
   *      HTML Specification</a>
   */
  FrameBuilder scrolling(String scrolling);

  /**
   * A URI designating the initial frame contents.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C
   *      HTML Specification</a>
   */
  FrameBuilder src(@IsTrustedResourceUri SafeUri src);

  /**
   * A URI designating the initial frame contents.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C
   *      HTML Specification</a>
   */
  FrameBuilder src(@IsTrustedResourceUri String src);
}
