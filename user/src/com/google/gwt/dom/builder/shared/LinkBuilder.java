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
 * Builds an link element.
 */
public interface LinkBuilder extends ElementBuilderBase<LinkBuilder> {

  /**
   * Disable the link. This is currently only used for style sheet links, and
   * may be used to deactivate style sheets.
   */
  LinkBuilder disabled();

  /**
   * The URI of the linked resource.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C
   *      HTML Specification</a>
   */
  LinkBuilder href(String href);

  /**
   * Language code of the linked resource.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">W3C
   *      HTML Specification</a>
   */
  LinkBuilder hreflang(String hreflang);

  /**
   * Designed for use with one or more target media.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C
   *      HTML Specification</a>
   */
  LinkBuilder media(String media);

  /**
   * Forward link type.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">W3C
   *      HTML Specification</a>
   */
  LinkBuilder rel(String rel);

  /**
   * Frame to render the resource in.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C
   *      HTML Specification</a>
   */
  LinkBuilder target(String target);

  /**
   * Advisory content type.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">W3C
   *      HTML Specification</a>
   */
  LinkBuilder type(String type);
}
