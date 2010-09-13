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
package com.google.gwt.sample.showcase.client.content.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle({".gwt-PushButton", ".gwt-ToggleButton"})
public class CwCustomButton extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwCustomButtonDescription();

    String cwCustomButtonName();

    String cwCustomButtonPush();

    String cwCustomButtonToggle();
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
  public CwCustomButton(CwConstants constants) {
    super(constants.cwCustomButtonName(), constants.cwCustomButtonDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a panel to layout the widgets
    VerticalPanel vpanel = new VerticalPanel();
    HorizontalPanel pushPanel = new HorizontalPanel();
    pushPanel.setSpacing(10);
    HorizontalPanel togglePanel = new HorizontalPanel();
    togglePanel.setSpacing(10);

    // Combine all the panels
    vpanel.add(new HTML(constants.cwCustomButtonPush()));
    vpanel.add(pushPanel);
    vpanel.add(new HTML("<br><br>" + constants.cwCustomButtonToggle()));
    vpanel.add(togglePanel);

    // Add a normal PushButton
    PushButton normalPushButton = new PushButton(
        new Image(Showcase.images.gwtLogo()));
    normalPushButton.ensureDebugId("cwCustomButton-push-normal");
    pushPanel.add(normalPushButton);

    // Add a disabled PushButton
    PushButton disabledPushButton = new PushButton(
        new Image(Showcase.images.gwtLogo()));
    disabledPushButton.ensureDebugId("cwCustomButton-push-disabled");
    disabledPushButton.setEnabled(false);
    pushPanel.add(disabledPushButton);

    // Add a normal ToggleButton
    ToggleButton normalToggleButton = new ToggleButton(
        new Image(Showcase.images.gwtLogo()));
    normalToggleButton.ensureDebugId("cwCustomButton-toggle-normal");
    togglePanel.add(normalToggleButton);

    // Add a disabled ToggleButton
    ToggleButton disabledToggleButton = new ToggleButton(
        new Image(Showcase.images.gwtLogo()));
    disabledToggleButton.ensureDebugId("cwCustomButton-toggle-disabled");
    disabledToggleButton.setEnabled(false);
    togglePanel.add(disabledToggleButton);

    // Return the panel
    return vpanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCustomButton.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
