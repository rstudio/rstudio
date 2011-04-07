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
package com.google.gwt.sample.showcase.client.content.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".gwt-SplitLayoutPanel")
public class CwSplitLayoutPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwSplitLayoutPanelCenter();

    String cwSplitLayoutPanelDescription();

    String cwSplitLayoutPanelEast();

    String cwSplitLayoutPanelName();

    String cwSplitLayoutPanelNorth1();

    String cwSplitLayoutPanelNorth2();

    String cwSplitLayoutPanelSouth1();

    String cwSplitLayoutPanelSouth2();

    String cwSplitLayoutPanelWest();
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
  public CwSplitLayoutPanel(CwConstants constants) {
    super(constants.cwSplitLayoutPanelName(), constants
        .cwSplitLayoutPanelDescription(), true);
    this.constants = constants;
  }

  @Override
  public boolean hasMargins() {
    return false;
  }

  @Override
  public boolean hasScrollableContent() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a Split Panel
    SplitLayoutPanel splitPanel = new SplitLayoutPanel(5);
    splitPanel.ensureDebugId("cwSplitLayoutPanel");
    splitPanel.getElement().getStyle()
        .setProperty("border", "3px solid #e7e7e7");

    // Add text all around.
    splitPanel.addNorth(new Label(constants.cwSplitLayoutPanelNorth1()), 50);
    splitPanel.addSouth(new Label(constants.cwSplitLayoutPanelSouth1()), 50);
    splitPanel.addEast(new Label(constants.cwSplitLayoutPanelEast()), 100);
    splitPanel.addWest(new Label(constants.cwSplitLayoutPanelWest()), 100);
    splitPanel.addNorth(new Label(constants.cwSplitLayoutPanelNorth2()), 50);
    splitPanel.addSouth(new Label(constants.cwSplitLayoutPanelSouth2()), 50);

    // Add scrollable text to the center.
    String centerText = constants.cwSplitLayoutPanelCenter();
    for (int i = 0; i < 3; i++) {
      centerText += " " + centerText;
    }
    Label centerLabel = new Label(centerText);
    ScrollPanel centerScrollable = new ScrollPanel(centerLabel);
    splitPanel.add(centerScrollable);

    // Return the content
    return splitPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwSplitLayoutPanel.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
