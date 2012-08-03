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
 * <p>Allows ARIA Accessibility attributes to be added to widgets so that they can be identified by
 * assistive technology.</p>
 *
 * <p>ARIA roles define widgets and page structure that can be interpreted by a reader
 * application/device. There is a set of abstract roles which are used as
 * building blocks of the roles hierarchy structural and define the common properties and states
 * for the concrete roles. Abstract roles cannot be set to HTML elements.</p>
 *
 * <p>This class defines some of the supported operations for a role -- set/get/remove
 * role to/from a DOM element.</p>
 *
 * <p>For more details about ARIA roles check <a href="http://www.w3.org/TR/wai-aria/roles">
 * The Roles Model </a>.</p>
 */
class RoleImpl {
  private static final String ATTR_NAME_ROLE = "role";

  private final String roleName;

  RoleImpl(String roleName) {
    assert roleName != null : "Role name cannot be null";
    this.roleName = roleName;
  }

  /**
   * Gets the role for the element {@code element}. If none is set, "" is returned.
   *
   * @param element HTML element
   * @return The role attribute value
   */
  public String get(Element element) {
    assert element != null : "Element cannot be null.";
    return element.getAttribute(ATTR_NAME_ROLE);
  }

  /**
   * Gets the role name
   *
   * @return The role name
   */
  public String getName() {
    return roleName;
  }

  /**
   * Removes the role for element {@code element}
   *
   * @param element HTML element
   */
  public void remove(Element element) {
    assert element != null : "Element cannot be null.";
    element.removeAttribute(ATTR_NAME_ROLE);
  }

  /**
   * Sets the role to element {@code element}
   *
   * @param element HTML element
   */
  public void set(Element element) {
    assert element != null : "Element cannot be null.";
    element.setAttribute(ATTR_NAME_ROLE, roleName);
  }
}
