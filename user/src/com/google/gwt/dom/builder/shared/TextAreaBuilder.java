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
 * Builds an textarea element.
 */
public interface TextAreaBuilder extends ElementBuilderBase<TextAreaBuilder> {

  String UNSUPPORTED_HTML =
      "TextArea elements do not support setting inner html.  Use text() instead.";

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C
   *      HTML Specification</a>
   */
  TextAreaBuilder accessKey(String accessKey);

  /**
   * Width of control (in characters).
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-cols-TEXTAREA">W3C
   *      HTML Specification</a>
   */
  TextAreaBuilder cols(int cols);

  /**
   * Represents the contents of the element. The value of this attribute does
   * not change if the contents of the corresponding form control, in an
   * interactive user agent, changes.
   */
  TextAreaBuilder defaultValue(String defaultValue);

  /**
   * Disable this control.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C
   *      HTML Specification</a>
   */
  TextAreaBuilder disabled();

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-TEXTAREA">W3C
   *      HTML Specification</a>
   */
  TextAreaBuilder name(String name);

  /**
   * Make control is read-only.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly">W3C
   *      HTML Specification</a>
   */
  TextAreaBuilder readOnly();

  /**
   * Number of text rows.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-rows-TEXTAREA">W3C
   *      HTML Specification</a>
   */
  TextAreaBuilder rows(int rows);

  /**
   * Represents the current contents of the corresponding form control, in an
   * interactive user agent. Changing this attribute changes the contents of the
   * form control, but does not change the contents of the element. If the
   * entirety of the data can not fit into a single string, the implementation
   * may truncate the data.
   */
  TextAreaBuilder value(String value);
}
