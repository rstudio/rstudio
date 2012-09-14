/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.aria.client;
/////////////////////////////////////////////////////////
// This is auto-generated code.  Do not manually edit! //
/////////////////////////////////////////////////////////

import com.google.gwt.dom.client.Element;

/**
 * A type that represents the <a href="http://www.w3.org/TR/wai-aria/roles#option">option</a>
 * role in the ARIA specification.
 *
 * @see Role
 * @see Roles
 */
public interface OptionRole extends InputRole {
  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-checked">
   * aria-checked</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaCheckedState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-posinset">
   * aria-posinset</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaPosinsetProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-selected">
   * aria-selected</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaSelectedState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-setsize">
   * aria-setsize</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaSetsizeProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-checked">
   * aria-checked</a> attribute from the {@code element}.
   */
  void removeAriaCheckedState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-posinset">
   * aria-posinset</a> attribute from the {@code element}.
   */
  void removeAriaPosinsetProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-selected">
   * aria-selected</a> attribute from the {@code element}.
   */
  void removeAriaSelectedState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-setsize">
   * aria-setsize</a> attribute from the {@code element}.
   */
  void removeAriaSetsizeProperty(Element element);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-checked">
   * aria-checked</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaCheckedState(Element element, CheckedValue value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-posinset">
   * aria-posinset</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaPosinsetProperty(Element element, int value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-selected">
   * aria-selected</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaSelectedState(Element element, SelectedValue value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-setsize">
   * aria-setsize</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaSetsizeProperty(Element element, int value);
}
