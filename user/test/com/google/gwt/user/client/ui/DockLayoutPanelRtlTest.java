/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;

/**
 * Tests for {@link DockLayoutPanel}.
 */
public class DockLayoutPanelRtlTest extends WidgetTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTestRtl";
  }

  public void testGetResolvedDirection() {
    DockLayoutPanel panel = createDockLayoutPanel();
    assertEquals(
        Direction.EAST, panel.getResolvedDirection(Direction.LINE_START));
    assertEquals(
        Direction.WEST, panel.getResolvedDirection(Direction.LINE_END));
  }

  protected DockLayoutPanel createDockLayoutPanel() {
    return new DockLayoutPanel(Unit.PX);
  }
}
