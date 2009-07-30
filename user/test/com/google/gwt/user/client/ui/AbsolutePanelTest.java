/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;

/**
 * TODO: document me.
 */
public class AbsolutePanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new AbsolutePanel());
  }

  @DoNotRunWith(Platform.Htmlunit)
  public void testPositioning() {
    // Make an absolute panel with a label at (3, 7).
    AbsolutePanel abs = new AbsolutePanel();
    abs.setSize("128px", "128px");
    Label lbl = new Label("foo");
    abs.add(lbl, 3, 7);

    // Put the panel in a grid that will place it at (100, 200) within the grid.
    Grid g = new Grid(2, 2);
    g.setBorderWidth(0);
    g.setCellPadding(0);
    g.setCellSpacing(0);
    g.getCellFormatter().setWidth(0, 0, "100px");
    g.getCellFormatter().setHeight(0, 0, "200px");
    g.setWidget(1, 1, abs);

    // Clear the margin so that absolute position is predictable.
    Window.setMargin("0px");
    RootPanel.get().add(g);

    // Make sure that the label's position, both relative to the absolute panel
    // and relative to the page, is correct. It is important to test both of
    // these, because an incorrectly constructed AbsolutePanel will lead to
    // wacky positioning of its children.
    int x = abs.getWidgetLeft(lbl);
    int y = abs.getWidgetTop(lbl);
    int absX = lbl.getAbsoluteLeft() - Document.get().getBodyOffsetLeft();
    int absY = lbl.getAbsoluteTop() - Document.get().getBodyOffsetTop();
    assertEquals(3, x);
    assertEquals(7, y);
    assertEquals("absX should be 103. This will fail in WebKit if run headless", 
        3 + 100, absX);
    assertEquals(7 + 200, absY);
  }
}
