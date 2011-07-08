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
 * Builds an select element.
 */
public interface SelectBuilder extends ElementBuilderBase<SelectBuilder> {

  /**
   * Disable the select box.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C
   *      HTML Specification</a>
   */
  SelectBuilder disabled();

  /**
   * Allow multiple options to be selected.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-multiple">W3C
   *      HTML Specification</a>
   */
  SelectBuilder multiple();

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-SELECT">W3C
   *      HTML Specification</a>
   */
  SelectBuilder name(String name);

  /**
   * The ordinal index of the selected option, starting from 0. The value -1 is
   * returned if no element is selected. If multiple options are selected, the
   * index of the first selected option is returned.
   */
  SelectBuilder selectedIndex(int index);

  /**
   * Number of visible rows.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-size-SELECT">W3C
   *      HTML Specification</a>
   */
  SelectBuilder size(int size);

  /**
   * The type of this form control. This is the string "select-multiple" when
   * the multiple attribute is true and the string "select-one" when false.
   */
  SelectBuilder type(String type);

  /**
   * The current form control value (i.e., the value of the currently selected
   * option), if multiple options are selected this is the value of the first
   * selected option.
   */
  SelectBuilder value(String value);
}
