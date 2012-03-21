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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.Element;

/**
 * <p>Allows ARIA attributes to be added to widgets so that they can be
 * identified by assistive technologies. Firefox 3, Firefox 2 (via FireVox),
 * and Opera 9.5 are the only released browsers that currently support this
 * feature, but in the near future it will be available in Safari and Internet
 * Explorer 8. Individual screen reader applications may also support ARIA, to
 * varying extents.</p>
 *
 * <p>A 'role' describes the role a widget plays in a page: i.e. a checkbox
 * widget is assigned a "checkbox" role.</p>
 *
 * <p>A 'state' describes the current state of the widget. For example, a
 * checkbox widget has the state "checked", which is given a value of "true" or
 * "false" depending on whether it is currently checked or unchecked.</p>
 *
 * <p>See <a href="http://developer.mozilla.org/en/docs/Accessible_DHTML">the
 * MDC page on Accessible DHTML</a> for more information.</p>
 *
 * <p>Note that although this API is public, the ARIA specification is still
 * somewhat in flux. As a result, this API is subject to change as the
 * specification stabilizes; we will do our best to keep the community
 * updated on changes.</p>
 *
 * @deprecated Use the new GWT ARIA library with the factory class for the ARIA roles
 * {@link com.google.gwt.aria.client.Roles}. There are getters for all ARIA roles. For each
 * role there are get/set/remove methods defined for all (own and inherited) supported states
 * and properties.
 */
@Deprecated
public final class Accessibility {

  public static final String ROLE_TREE = "tree";
  public static final String ROLE_TREEITEM = "treeitem";
  public static final String ROLE_BUTTON = "button";
  public static final String ROLE_TABLIST = "tablist";
  public static final String ROLE_TAB = "tab";
  public static final String ROLE_TABPANEL = "tabpanel";
  public static final String ROLE_MENUBAR = "menubar";
  public static final String ROLE_MENUITEM = "menuitem";

  public static final String STATE_ACTIVEDESCENDANT = "aria-activedescendant";
  public static final String STATE_POSINSET = "aria-posinset";
  public static final String STATE_SETSIZE = "aria-setsize";
  public static final String STATE_SELECTED = "aria-selected";
  public static final String STATE_EXPANDED = "aria-expanded";
  public static final String STATE_LEVEL = "aria-level";
  public static final String STATE_HASPOPUP = "aria-haspopup";
  public static final String STATE_PRESSED = "aria-pressed";

  private static final String ATTR_NAME_ROLE = "role";

  /**
   * Requests the string value of the role with the specified namespace.
   *
   * @param elem the element which has the specified role
   * @return the value of the role, or an empty string if none exists
   */
  public static String getRole(Element elem) {
    return elem.getAttribute(ATTR_NAME_ROLE);
  }

  /**
   * Requests the string value of the state with the specified namespace.
   *
   * @param elem the element which has the specified state
   * @param stateName the name of the state
   * @return the value of the state, or an empty string if none exists
   */
  public static String getState(Element elem, String stateName) {
    return elem.getAttribute(stateName);
  }

  /**
   * Removes the state from the given element.
   *
   * @param elem the element which has the specified state
   * @param stateName the name of the state to remove
   */
  public static void removeState(Element elem, String stateName)  {
    elem.removeAttribute(stateName);
  }
  /**
   * Assigns the specified element the specified role and value for that role.
   *
   * @param elem the element to be given the specified role
   * @param roleName the name of the role
   */
  public static void setRole(Element elem, String roleName) {
    elem.setAttribute(ATTR_NAME_ROLE, roleName);
  }

  /**
   * Assigns the specified element the specified state and value for that state.
   *
   * @param elem the element to be given the specified state
   * @param stateName the name of the state
   * @param stateValue the value of the state
   */
  public static void setState(Element elem, String stateName, String stateValue) {
    elem.setAttribute(stateName, stateValue);
  }

  private Accessibility() {
  }
}