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
 * RoletypeRole interface.
 * The interface defines methods for setting, getting, removing states and properties.
 * <p>Allows ARIA Accessibility attributes to be added to widgets so that they can be identified by
 * assistive technology.</p>
 *
 * <p>ARIA roles define widgets and page structure that can be interpreted by a reader
 * application/device. There is a set of abstract roles which are used as
 * building blocks of the roles hierarchy structural and define the common properties and states
 * for the concrete roles. Abstract roles cannot be set to HTML elements.</p>
 *
 * <p>There are states and properties that are defined for a role. As roles are organized in a
 * hierarchy, a role has inherited and own properties and states which can be set to the
 * element.</p>
 *
 * <p>For more details about ARIA roles check <a href="http://www.w3.org/TR/wai-aria/roles">
 * The Roles Model </a>.</p>
 */
public interface RoletypeRole {
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
