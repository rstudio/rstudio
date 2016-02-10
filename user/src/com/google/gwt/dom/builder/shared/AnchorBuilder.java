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
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * Builds an anchor element.
 */
public interface AnchorBuilder extends ElementBuilderBase<AnchorBuilder> {

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder accessKey(String accessKey);

  /**
   * The absolute URI of the linked resource.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder href(SafeUri href);

  /**
   * The absolute URI of the linked resource.
   *
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder href(@IsSafeUri String href);

  /**
   * Language code of the linked resource.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder hreflang(String hreflang);

  /**
   * Anchor name.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder name(String name);

  /**
   * Forward link type.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder rel(String rel);

  /**
   * Frame to render the resource in.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder target(String target);

  /**
   * Advisory content type.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">W3C
   *      HTML Specification</a>
   */
  AnchorBuilder type(String type);
}
