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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle({
    ".gwt-DecoratedTabBar", "html>body .gwt-DecoratedTabBar", "* html .gwt-DecoratedTabBar",
    ".gwt-TabPanel"})
public class CwTabLayoutPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwTabPanelDescription();

    String cwTabPanelName();

    String cwTabPanelTab0();

    String cwTabPanelTab2();

    String[] cwTabPanelTabs();
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
  public CwTabLayoutPanel(CwConstants constants) {
    super(constants.cwTabPanelName(), constants.cwTabPanelDescription(), true);
    this.constants = constants;
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
    // Create a tab panel
    TabLayoutPanel tabPanel = new TabLayoutPanel(2.5, Unit.EM);
    tabPanel.setAnimationDuration(1000);
    tabPanel.getElement().getStyle().setMarginBottom(10.0, Unit.PX);

    // Add a home tab
    String[] tabTitles = constants.cwTabPanelTabs();
    HTML homeText = new HTML(constants.cwTabPanelTab0());
    tabPanel.add(homeText, tabTitles[0]);

    // Add a tab with an image
    SimplePanel imageContainer = new SimplePanel();
    imageContainer.setWidget(new Image(Showcase.images.gwtLogo()));
    tabPanel.add(imageContainer, tabTitles[1]);

    // Add a tab
    HTML moreInfo = new HTML(constants.cwTabPanelTab2());
    tabPanel.add(moreInfo, tabTitles[2]);

    // Return the content
    tabPanel.selectTab(0);
    tabPanel.ensureDebugId("cwTabPanel");

    return tabPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwTabLayoutPanel.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
