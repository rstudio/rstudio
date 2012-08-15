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
 * <p>Implements {@link TextboxRole}.</p>
 */
class TextboxRoleImpl extends RoleImpl implements TextboxRole {
  TextboxRoleImpl(String roleName) {
    super(roleName);
  }

  @Override
  public String getAriaActivedescendantProperty(Element element) {
    return Property.ACTIVEDESCENDANT.get(element);
  }

  @Override
  public String getAriaAutocompleteProperty(Element element) {
    return Property.AUTOCOMPLETE.get(element);
  }

  @Override
  public String getAriaMultilineProperty(Element element) {
    return Property.MULTILINE.get(element);
  }

  @Override
  public String getAriaReadonlyProperty(Element element) {
    return Property.READONLY.get(element);
  }

  @Override
  public String getAriaRequiredProperty(Element element) {
    return Property.REQUIRED.get(element);
  }

  @Override
  public void removeAriaActivedescendantProperty(Element element) {
    Property.ACTIVEDESCENDANT.remove(element);
  }

  @Override
  public void removeAriaAutocompleteProperty(Element element) {
    Property.AUTOCOMPLETE.remove(element);
  }

  @Override
  public void removeAriaMultilineProperty(Element element) {
    Property.MULTILINE.remove(element);
  }

  @Override
  public void removeAriaReadonlyProperty(Element element) {
    Property.READONLY.remove(element);
  }

  @Override
  public void removeAriaRequiredProperty(Element element) {
    Property.REQUIRED.remove(element);
  }

  @Override
  public void setAriaActivedescendantProperty(Element element, IdReference value) {
    Property.ACTIVEDESCENDANT.set(element, value);
  }

  @Override
  public void setAriaAutocompleteProperty(Element element, AutocompleteValue value) {
    Property.AUTOCOMPLETE.set(element, value);
  }

  @Override
  public void setAriaMultilineProperty(Element element, boolean value) {
    Property.MULTILINE.set(element, value);
  }

  @Override
  public void setAriaReadonlyProperty(Element element, boolean value) {
    Property.READONLY.set(element, value);
  }

  @Override
  public void setAriaRequiredProperty(Element element, boolean value) {
    Property.REQUIRED.set(element, value);
  }
}
