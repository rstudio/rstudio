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
 * A generic ARIA Role. This interface defines generic methods for setting, getting, and removing
 * attributes on DOM Elements so that they can be identified by screen readers. Subtypes
 * define methods for specific roles.
 *
 * <p>The ARIA specification defines a hierarchy of roles, which is mirrored here as
 * a hierarchy of Java interfaces. Some roles are abstract and define methods that are common to
 * their children. Only concrete roles (available via methods in {@link Roles}) should be used to
 * modify HTML elements.</p>
 *
 * <p>For more details, see <a href="http://www.w3.org/TR/wai-aria/roles">The Roles Model</a>
 * in the ARIA specification.</p>
 */
public interface Role {
  /**
   * Returns the value of the 'role' attribute for the {@code element}
   * or the empty string if no 'role' attribute is present.
   *
   * @see <a href="http://www.w3.org/TR/wai-aria/roles">Roles documentation</a>
   */
   String get(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-atomic">
   * aria-atomic</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaAtomicProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-busy">
   * aria-busy</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaBusyState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-controls">
   * aria-controls</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaControlsProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-describedby">
   * aria-describedby</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaDescribedbyProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-disabled">
   * aria-disabled</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaDisabledState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-dropeffect">
   * aria-dropeffect</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaDropeffectProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-flowto">
   * aria-flowto</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaFlowtoProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-grabbed">
   * aria-grabbed</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaGrabbedState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-haspopup">
   * aria-haspopup</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaHaspopupProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-hidden">
   * aria-hidden</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaHiddenState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-invalid">
   * aria-invalid</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaInvalidState(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-labelledby">
   * aria-labelledby</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaLabelledbyProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-label">
   * aria-label</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaLabelProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-live">
   * aria-live</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaLiveProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-owns">
   * aria-owns</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaOwnsProperty(Element element);

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-relevant">
   * aria-relevant</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getAriaRelevantProperty(Element element);

  /**
   * Gets the ARIA 'role' attribute name as defined in the
   * <a href="http://www.w3.org/TR/wai-aria">WAI-ARIA</a> standard.
   *
   * @see <a href="http://www.w3.org/TR/wai-aria/roles">Roles documentation</a>
   */
  String getName();

  /**
   * Returns the value of the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#tabIndex">
   * tabIndex</a> attribute for the {@code element} or "" if no
   * such attribute is present.
   */
  String getTabindexExtraAttribute(Element element);

  /**
   * Removes the 'role' attribute from the {@code element}.
   *
   * @see <a href="http://www.w3.org/TR/wai-aria/roles">Roles documentation</a>
   */
  void remove(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-atomic">
   * aria-atomic</a> attribute from the {@code element}.
   */
  void removeAriaAtomicProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-busy">
   * aria-busy</a> attribute from the {@code element}.
   */
  void removeAriaBusyState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-controls">
   * aria-controls</a> attribute from the {@code element}.
   */
  void removeAriaControlsProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-describedby">
   * aria-describedby</a> attribute from the {@code element}.
   */
  void removeAriaDescribedbyProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-disabled">
   * aria-disabled</a> attribute from the {@code element}.
   */
  void removeAriaDisabledState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-dropeffect">
   * aria-dropeffect</a> attribute from the {@code element}.
   */
  void removeAriaDropeffectProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-flowto">
   * aria-flowto</a> attribute from the {@code element}.
   */
  void removeAriaFlowtoProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-grabbed">
   * aria-grabbed</a> attribute from the {@code element}.
   */
  void removeAriaGrabbedState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-haspopup">
   * aria-haspopup</a> attribute from the {@code element}.
   */
  void removeAriaHaspopupProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-hidden">
   * aria-hidden</a> attribute from the {@code element}.
   */
  void removeAriaHiddenState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-invalid">
   * aria-invalid</a> attribute from the {@code element}.
   */
  void removeAriaInvalidState(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-labelledby">
   * aria-labelledby</a> attribute from the {@code element}.
   */
  void removeAriaLabelledbyProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-label">
   * aria-label</a> attribute from the {@code element}.
   */
  void removeAriaLabelProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-live">
   * aria-live</a> attribute from the {@code element}.
   */
  void removeAriaLiveProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-owns">
   * aria-owns</a> attribute from the {@code element}.
   */
  void removeAriaOwnsProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-relevant">
   * aria-relevant</a> attribute from the {@code element}.
   */
  void removeAriaRelevantProperty(Element element);

  /**
   * Removes the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#tabIndex">
   * tabIndex</a> attribute from the {@code element}.
   */
  void removeTabindexExtraAttribute(Element element);

  /**
   * Sets the 'role' attribute of the given {@code element} to the appropriate
   * value for this role.
   *
   * @see <a href="http://www.w3.org/TR/wai-aria/roles">Roles documentation</a>
   */
  void set(Element element);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-atomic">
   * aria-atomic</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaAtomicProperty(Element element, boolean value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-busy">
   * aria-busy</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaBusyState(Element element, boolean value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-controls">
   * aria-controls</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaControlsProperty(Element element, Id... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-describedby">
   * aria-describedby</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaDescribedbyProperty(Element element, Id... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-disabled">
   * aria-disabled</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaDisabledState(Element element, boolean value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-dropeffect">
   * aria-dropeffect</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaDropeffectProperty(Element element, DropeffectValue... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-flowto">
   * aria-flowto</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaFlowtoProperty(Element element, Id... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-grabbed">
   * aria-grabbed</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaGrabbedState(Element element, GrabbedValue value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-haspopup">
   * aria-haspopup</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaHaspopupProperty(Element element, boolean value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-hidden">
   * aria-hidden</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaHiddenState(Element element, boolean value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-invalid">
   * aria-invalid</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaInvalidState(Element element, InvalidValue value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-labelledby">
   * aria-labelledby</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaLabelledbyProperty(Element element, Id... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-label">
   * aria-label</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaLabelProperty(Element element, String value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-live">
   * aria-live</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaLiveProperty(Element element, LiveValue value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-owns">
   * aria-owns</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaOwnsProperty(Element element, Id... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#aria-relevant">
   * aria-relevant</a> attribute for the {@code element} to the given {@code value}.
   */
  void setAriaRelevantProperty(Element element, RelevantValue... value);

  /**
   * Sets the
   * <a href="http://www.w3.org/TR/wai-aria/states_and_properties#tabIndex">
   * tabIndex</a> attribute for the {@code element} to the given {@code value}.
   */
  void setTabindexExtraAttribute(Element element, int value);
}
