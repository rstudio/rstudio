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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".cw-DockPanel")
public class CwDockPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {

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
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwDockPanel(CwConstants constants) {
    super(
        constants.cwDockPanelName(), constants.cwDockPanelDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
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

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwDockPanel.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
