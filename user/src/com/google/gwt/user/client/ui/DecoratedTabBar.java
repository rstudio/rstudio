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

/**
 * <p>
 * A {@link TabBar} that wraps each tab in a 2x3 grid (six box), which allows
 * users to add rounded corners.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedTabBar { the tab bar itself }</li>
 * <li>.gwt-DecoratedTabBar .gwt-TabBarFirst { the left edge of the bar }</li>
 * <li>.gwt-DecoratedTabBar .gwt-TabBarRest { the right edge of the bar }</li>
 * <li>.gwt-DecoratedTabBar .gwt-TabBarItem { unselected tabs }</li>
 * <li>.gwt-DecoratedTabBar .gwt-TabBarItem-wrapper { table cell around tab }</li>
 * <li>.gwt-DecoratedTabBar .gwt-TabBarItem-selected { additional style for
 * selected tabs } </li>
 * <li>.gwt-DecoratedTabBar .gwt-TabBarItem-wrapper-selected { table cell
 * around selected tab }</li>
 * <li>.gwt-DecoratedTabBar .tabTopLeft { top left corner of the tab}</li>
 * <li>.gwt-DecoratedTabBar .tabTopLeftInner { the inner element of the cell}</li>
 * <li>.gwt-DecoratedTabBar .tabTopCenter { top center of the tab}</li>
 * <li>.gwt-DecoratedTabBar .tabTopCenterInner { the inner element of the cell}</li>
 * <li>.gwt-DecoratedTabBar .tabTopRight { top right corner of the tab}</li>
 * <li>.gwt-DecoratedTabBar .tabTopRightInner { the inner element of the cell}</li>
 * <li>.gwt-DecoratedTabBar .tabMiddleLeft { left side of the tab }</li>
 * <li>.gwt-DecoratedTabBar .tabMiddleLeftInner { the inner element of the
 * cell}</li>
 * <li>.gwt-DecoratedTabBar .tabMiddleCenter { center of the tab, where the tab
 * text or widget resides }</li>
 * <li>.gwt-DecoratedTabBar .tabMiddleCenterInner { the inner element of the
 * cell}</li>
 * <li>.gwt-DecoratedTabBar .tabMiddleRight { right side of the tab }</li>
 * <li>.gwt-DecoratedTabBar .tabMiddleRightInner { the inner element of the
 * cell}</li>
 * </ul>
 */
public class DecoratedTabBar extends TabBar {
  static String[] TAB_ROW_STYLES = {"tabTop", "tabMiddle"};

  static final String STYLENAME_DEFAULT = "gwt-DecoratedTabBar";

  /**
   * Creates an empty {@link DecoratedTabBar}.
   */
  public DecoratedTabBar() {
    super();
    setStylePrimaryName(STYLENAME_DEFAULT);
  }

  @Override
  protected SimplePanel createTabTextWrapper() {
    return new DecoratorPanel(TAB_ROW_STYLES, 1);
  }
}
