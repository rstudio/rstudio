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

/**
 * Native implementation class used with
 * {@link com.google.gwt.user.client.ui.Accessibility}.
 */
public class AccessibilityImpl {

  public String getRole(Element elem) {
    return "";
  }

  public String getState(Element elem, String stateName) {
    return "";
  }

  public void removeState(Element elem, String stateName) {    
  }

  public void setRole(Element elem, String roleName) {
  }

  public void setState(Element elem, String stateName, String stateValue) {
  }
}
