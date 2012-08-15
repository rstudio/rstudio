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
 * <p>Implements {@link ProgressbarRole}.</p>
 */
class ProgressbarRoleImpl extends RoleImpl implements ProgressbarRole {
  ProgressbarRoleImpl(String roleName) {
    super(roleName);
  }

  @Override
  public String getAriaValuemaxProperty(Element element) {
    return Property.VALUEMAX.get(element);
  }

  @Override
  public String getAriaValueminProperty(Element element) {
    return Property.VALUEMIN.get(element);
  }

  @Override
  public String getAriaValuenowProperty(Element element) {
    return Property.VALUENOW.get(element);
  }

  @Override
  public String getAriaValuetextProperty(Element element) {
    return Property.VALUETEXT.get(element);
  }

  @Override
  public void removeAriaValuemaxProperty(Element element) {
    Property.VALUEMAX.remove(element);
  }

  @Override
  public void removeAriaValueminProperty(Element element) {
    Property.VALUEMIN.remove(element);
  }

  @Override
  public void removeAriaValuenowProperty(Element element) {
    Property.VALUENOW.remove(element);
  }

  @Override
  public void removeAriaValuetextProperty(Element element) {
    Property.VALUETEXT.remove(element);
  }

  @Override
  public void setAriaValuemaxProperty(Element element, Number value) {
    Property.VALUEMAX.set(element, value);
  }

  @Override
  public void setAriaValueminProperty(Element element, Number value) {
    Property.VALUEMIN.set(element, value);
  }

  @Override
  public void setAriaValuenowProperty(Element element, Number value) {
    Property.VALUENOW.set(element, value);
  }

  @Override
  public void setAriaValuetextProperty(Element element, String value) {
    Property.VALUETEXT.set(element, value);
  }
}
