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

/**
 * Builds an image element.
 */
public interface ImageBuilder extends ElementBuilderBase<ImageBuilder> {

  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">W3C
   *      HTML Specification</a>
   */
  ImageBuilder alt(String alt);

  /**
   * Height of the image in pixels.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C
   *      HTML Specification</a>
   */
  ImageBuilder height(int height);

  /**
   * Use server-side image map.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-ismap">W3C
   *      HTML Specification</a>
   */
  ImageBuilder isMap();

  /**
   * URI designating the source of this image.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-src-IMG">W3C
   *      HTML Specification</a>
   */
  ImageBuilder src(String src);

  /**
   * The width of the image in pixels.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C
   *      HTML Specification</a>
   */
  ImageBuilder width(int width);
}
