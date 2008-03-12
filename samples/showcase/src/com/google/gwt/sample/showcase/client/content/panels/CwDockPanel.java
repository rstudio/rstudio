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
package com.google.gwt.sample.showcase.client.content.panels;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 * 
 * @gwt.CSS .cw-DockPanel
 */
public class CwDockPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   * 
   * @gwt.SRC
   */
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {

    String cwDockPanelCenter();

    String cwDockPanelDescription();

    String cwDockPanelEast();

    String cwDockPanelName();

    String cwDockPanelNorth1();

    String cwDockPanelNorth2();

    String cwDockPanelSouth1();

    String cwDockPanelSouth2();

    String cwDockPanelWest();
  }

  /**
   * An instance of the constants.
   * 
   * @gwt.DATA
   */
  private CwConstants constants;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwDockPanel(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwDockPanelDescription();
  }

  @Override
  public String getName() {
    return constants.cwDockPanelName();
  }

  /**
   * Initialize this example.
   * 
   * @gwt.SRC
   */
  @Override
  public Widget onInitialize() {
    // Create a Dock Panel
    DockPanel dock = new DockPanel();
    dock.setStyleName("cw-DockPanel");
    dock.setSpacing(4);
    dock.setHorizontalAlignment(DockPanel.ALIGN_CENTER);

    // Add text all around
    dock.add(new HTML(constants.cwDockPanelNorth1()), DockPanel.NORTH);
    dock.add(new HTML(constants.cwDockPanelSouth1()), DockPanel.SOUTH);
    dock.add(new HTML(constants.cwDockPanelEast()), DockPanel.EAST);
    dock.add(new HTML(constants.cwDockPanelWest()), DockPanel.WEST);
    dock.add(new HTML(constants.cwDockPanelNorth2()), DockPanel.NORTH);
    dock.add(new HTML(constants.cwDockPanelSouth2()), DockPanel.SOUTH);

    // Add scrollable text in the center
    HTML contents = new HTML(constants.cwDockPanelCenter());
    ScrollPanel scroller = new ScrollPanel(contents);
    scroller.setSize("400px", "100px");
    dock.add(scroller, DockPanel.CENTER);

    // Return the content
    dock.ensureDebugId("cwDockPanel");
    return dock;
  }
}
