/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.cell.client;

/**
 * Indicates that a UI component can be collapsed next to another UI component,
 * thus sharing a common border. This allows UI components to appear flush
 * against each other without extra thick borders.
 * 
 * <p>
 * Before collapse:
 * 
 * <pre>
 *   ---------    ----------    ---------
 *  | ButtonA |  |  ButtonB |  | ButtonC |
 *   ---------    ----------    ---------
 * </pre>
 * 
 * <p>
 * After collapse:
 * 
 * <pre>
 *   -----------------------------
 *  | ButtonA | ButtonB | ButtonC |
 *   -----------------------------
 * </pre>
 * 
 * <p>
 * In the above example, ButtonA has right-side collapsed, ButtonB has both left
 * and right-side collapsed, and ButtonC has left-side collapsed.
 */
public interface IsCollapsible {

  /**
   * Check whether or not the left-side of the UI component is collapsed
   * (sharing border with the component to its left).
   * 
   * @return true if collapsed, false if not
   */
  boolean isCollapseLeft();

  /**
   * right Check whether or not the left-side of the UI component is collapsed
   * (sharing border with the component to its left).
   * 
   * @return true if collapsed, false if not
   */
  boolean isCollapseRight();

  /**
   * Sets whether the left-side of the UI component is collapsed (sharing border
   * with the component to its left).
   * 
   * @param isCollapsed true if collapsed, false if not
   */
  void setCollapseLeft(boolean isCollapsed);

  /**
   * Sets whether the right-side of the UI component is collapsed (sharing
   * border with the component to its right).
   * 
   * @param isCollapsed true if collapsed, false if not
   */
  void setCollapseRight(boolean isCollapsed);
}
