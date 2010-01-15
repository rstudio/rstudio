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
 * A {@link TabPanel} that uses a {@link DecoratedTabBar} with rounded corners.
 * 
 * <p>
 * This widget will <em>only</em> work in quirks mode. If your application is in
 * Standards Mode, use {@link TabLayoutPanel} instead.
 * </p>
 * 
 * <p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedTabPanel { the tab panel itself }</li>
 * <li>.gwt-TabPanelBottom { the bottom section of the tab panel (the deck
 * containing the widget) }</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TabPanelExample}
 * </p>
 * 
 * @see TabLayoutPanel
 */
public class DecoratedTabPanel extends TabPanel {
  private static final String DEFAULT_STYLENAME = "gwt-DecoratedTabPanel";

  public DecoratedTabPanel() {
    setStylePrimaryName(DEFAULT_STYLENAME);
    getTabBar().setStylePrimaryName(DecoratedTabBar.STYLENAME_DEFAULT);
  }

  @Override
  protected SimplePanel createTabTextWrapper() {
    return new DecoratorPanel(DecoratedTabBar.TAB_ROW_STYLES, 1);
  }
}
