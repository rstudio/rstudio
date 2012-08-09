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
  String get(Element element);

  String getAriaAtomicProperty(Element element);

  String getAriaBusyState(Element element);

  String getAriaControlsProperty(Element element);

  String getAriaDescribedbyProperty(Element element);

  String getAriaDisabledState(Element element);

  String getAriaDropeffectProperty(Element element);

  String getAriaFlowtoProperty(Element element);

  String getAriaGrabbedState(Element element);

  String getAriaHaspopupProperty(Element element);

  String getAriaHiddenState(Element element);

  String getAriaInvalidState(Element element);

  String getAriaLabelledbyProperty(Element element);

  String getAriaLabelProperty(Element element);

  String getAriaLiveProperty(Element element);

  String getAriaOwnsProperty(Element element);

  String getAriaRelevantProperty(Element element);

  String getName();

  String getTabindexExtraAttribute(Element element);

  void remove(Element element);

  void removeAriaAtomicProperty(Element element);

  void removeAriaBusyState(Element element);

  void removeAriaControlsProperty(Element element);

  void removeAriaDescribedbyProperty(Element element);

  void removeAriaDisabledState(Element element);

  void removeAriaDropeffectProperty(Element element);

  void removeAriaFlowtoProperty(Element element);

  void removeAriaGrabbedState(Element element);

  void removeAriaHaspopupProperty(Element element);

  void removeAriaHiddenState(Element element);

  void removeAriaInvalidState(Element element);

  void removeAriaLabelledbyProperty(Element element);

  void removeAriaLabelProperty(Element element);

  void removeAriaLiveProperty(Element element);

  void removeAriaOwnsProperty(Element element);

  void removeAriaRelevantProperty(Element element);

  void removeTabindexExtraAttribute(Element element);

  void set(Element element);

  void setAriaAtomicProperty(Element element, boolean value);

  void setAriaBusyState(Element element, boolean value);

  void setAriaControlsProperty(Element element, IdReference... value);

  void setAriaDescribedbyProperty(Element element, IdReference... value);

  void setAriaDisabledState(Element element, boolean value);

  void setAriaDropeffectProperty(Element element, DropeffectValue... value);

  void setAriaFlowtoProperty(Element element, IdReference... value);

  void setAriaGrabbedState(Element element, GrabbedValue value);

  void setAriaHaspopupProperty(Element element, boolean value);

  void setAriaHiddenState(Element element, boolean value);

  void setAriaInvalidState(Element element, InvalidValue value);

  void setAriaLabelledbyProperty(Element element, IdReference... value);

  void setAriaLabelProperty(Element element, String value);

  void setAriaLiveProperty(Element element, LiveValue value);

  void setAriaOwnsProperty(Element element, IdReference... value);

  void setAriaRelevantProperty(Element element, RelevantValue... value);

  void setTabindexExtraAttribute(Element element, int value);
}
