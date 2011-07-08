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
 * Builds an input element.
 */
public interface InputBuilder extends ElementBuilderBase<InputBuilder> {

  /**
   * A comma-separated list of content types that a server processing this form
   * will handle correctly.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accept">W3C
   *      HTML Specification</a>
   */
  InputBuilder accept(String accept);

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C
   *      HTML Specification</a>
   */
  InputBuilder accessKey(String accessKey);

  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">W3C
   *      HTML Specification</a>
   */
  InputBuilder alt(String alt);

  /**
   * Set the state of the form control to <code>true</code> when type attribute
   * of the element has the value "radio" or "checkbox".
   */
  InputBuilder checked();

  /**
   * Set the default state of the form control to <code>true</code> when type
   * attribute of the element has the value "radio" or "checkbox".
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-checked">W3C
   *      HTML Specification</a>
   */
  InputBuilder defaultChecked();

  /**
   * When the type attribute of the element has the value "text", "file" or
   * "password", this represents the HTML value attribute of the element. The
   * value of this attribute does not change if the contents of the
   * corresponding form control, in an interactive user agent, changes.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-INPUT">W3C
   *      HTML Specification</a>
   */
  InputBuilder defaultValue(String defaultValue);

  /**
   * Disable the control.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C
   *      HTML Specification</a>
   */
  InputBuilder disabled();

  /**
   * Maximum number of characters for text fields, when type has the value
   * "text" or "password".
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-maxlength">W3C
   *      HTML Specification</a>
   */
  InputBuilder maxLength(int maxLength);

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT">W3C
   *      HTML Specification</a>
   */
  InputBuilder name(String name);

  /**
   * Make the control read-only. Relevant only when type has the value "text" or
   * "password".
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly">W3C
   *      HTML Specification</a>
   */
  InputBuilder readOnly();

  /**
   * Size information. The precise meaning is specific to each type of field.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-size-INPUT">W3C
   *      HTML Specification</a>
   */
  InputBuilder size(int size);

  /**
   * When the type attribute has the value "image", this attribute specifies the
   * location of the image to be used to decorate the graphical submit button.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-src">W3C
   *      HTML Specification</a>
   */
  InputBuilder src(String src);

  /**
   * When the type attribute of the element has the value "text", "file" or
   * "password", this represents the current contents of the corresponding form
   * control, in an interactive user agent. Changing this attribute changes the
   * contents of the form control, but does not change the value of the HTML
   * value attribute of the element. When the type attribute of the element has
   * the value "button", "hidden", "submit", "reset", "image", "checkbox" or
   * "radio", this represents the HTML value attribute of the element.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-INPUT">W3C
   *      HTML Specification</a>
   */
  InputBuilder value(String value);

}
