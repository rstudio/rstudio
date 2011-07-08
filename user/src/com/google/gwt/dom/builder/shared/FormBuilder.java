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
 * Builds an form element.
 */
public interface FormBuilder extends ElementBuilderBase<FormBuilder> {

  /**
   * List of character sets supported by the server.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accept-charset">W3C
   *      HTML Specification</a>
   */
  FormBuilder acceptCharset(String acceptCharset);

  /**
   * Server-side form handler.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-action">W3C
   *      HTML Specification</a>
   */
  FormBuilder action(String action);

  /**
   * The content type of the submitted form, generally
   * "application/x-www-form-urlencoded".
   * 
   * Note: The onsubmit even handler is not guaranteed to be triggered when
   * invoking this method. The behavior is inconsistent for historical reasons
   * and authors should not rely on a particular one.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-enctype">W3C
   *      HTML Specification</a>
   */
  FormBuilder enctype(String enctype);

  /**
   * HTTP method [IETF RFC 2616] used to submit form.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-method">W3C
   *      HTML Specification</a>
   */
  FormBuilder method(String method);

  /**
   * Names the form.
   */
  FormBuilder name(String name);

  /**
   * Frame to render the resource in.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C
   *      HTML Specification</a>
   */
  FormBuilder target(String target);
}
