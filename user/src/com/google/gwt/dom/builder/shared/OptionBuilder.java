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
 * Builds an option element.
 */
public interface OptionBuilder extends ElementBuilderBase<OptionBuilder> {

  /**
   * Represents the value of the HTML selected attribute. The value of this
   * attribute does not change if the state of the corresponding form control,
   * in an interactive user agent, changes.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-selected">W3C
   *      HTML Specification</a>
   */
  OptionBuilder defaultSelected();

  /**
   * Prevents the user from selecting this option.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C
   *      HTML Specification</a>
   */
  OptionBuilder disabled();

  /**
   * Option label for use in menus.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTION">W3C
   *      HTML Specification</a>
   */
  OptionBuilder label(String label);

  /**
   * Represents the current state of the corresponding form control, in an
   * interactive user agent. Changing this attribute changes the state of the
   * form control, but does not change the value of the HTML selected attribute
   * of the element.
   */
  OptionBuilder selected();

  /**
   * The current form control value.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-OPTION">W3C
   *      HTML Specification</a>
   */
  OptionBuilder value(String value);
}
