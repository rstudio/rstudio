/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;

/**
 * Firefox 1.5+ implementation of {@link AccessibilityImpl}.
 */
public class AccessibilityImplMozilla extends AccessibilityImpl {

  private static final String ATTR_NAME_ROLE = "role";

  public String getRole(Element elem) {
    return DOM.getElementAttribute(elem, ATTR_NAME_ROLE);
  }

  public String getState(Element elem, String stateName) {
    return DOM.getElementAttribute(elem, stateName);
  }

  public void removeState(Element elem, String stateName) {
    DOM.removeElementAttribute(elem, stateName);
  }
  
  public void setRole(Element elem, String roleName) {
    DOM.setElementAttribute(elem, ATTR_NAME_ROLE, roleName);
  }

  public void setState(Element elem, String stateName, String stateValue) {
    DOM.setElementAttribute(elem, stateName, stateValue);
  }
}
