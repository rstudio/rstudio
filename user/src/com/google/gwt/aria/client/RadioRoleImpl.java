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
 * <p>Implements {@link RadioRole}.</p>
 */
class RadioRoleImpl extends RoleImpl implements RadioRole {
  RadioRoleImpl(String roleName) {
    super(roleName);
  }

  @Override
  public String getAriaCheckedState(Element element) {
    return State.CHECKED.get(element);
  }

  @Override
  public String getAriaPosinsetProperty(Element element) {
    return Property.POSINSET.get(element);
  }

  @Override
  public String getAriaSelectedState(Element element) {
    return State.SELECTED.get(element);
  }

  @Override
  public String getAriaSetsizeProperty(Element element) {
    return Property.SETSIZE.get(element);
  }

  @Override
  public void removeAriaCheckedState(Element element) {
    State.CHECKED.remove(element);
  }

  @Override
  public void removeAriaPosinsetProperty(Element element) {
    Property.POSINSET.remove(element);
  }

  @Override
  public void removeAriaSelectedState(Element element) {
    State.SELECTED.remove(element);
  }

  @Override
  public void removeAriaSetsizeProperty(Element element) {
    Property.SETSIZE.remove(element);
  }

  @Override
  public void setAriaCheckedState(Element element, CheckedValue value) {
    State.CHECKED.set(element, value);
  }

  @Override
  public void setAriaPosinsetProperty(Element element, int value) {
    Property.POSINSET.set(element, value);
  }

  @Override
  public void setAriaSelectedState(Element element, SelectedValue value) {
    State.SELECTED.set(element, value);
  }

  @Override
  public void setAriaSetsizeProperty(Element element, int value) {
    Property.SETSIZE.set(element, value);
  }
}
