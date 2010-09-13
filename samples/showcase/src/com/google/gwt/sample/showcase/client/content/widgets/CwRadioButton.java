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
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".gwt-RadioButton")
public class CwRadioButton extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String[] cwRadioButtonColors();

    String cwRadioButtonDescription();

    String cwRadioButtonName();

    String cwRadioButtonSelectColor();

    String cwRadioButtonSelectSport();

    String[] cwRadioButtonSports();
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
  public CwRadioButton(CwConstants constants) {
    super(constants.cwRadioButtonName(), constants.cwRadioButtonDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a vertical panel to align the radio buttons
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(new HTML(constants.cwRadioButtonSelectColor()));

    // Add some radio buttons to a group called 'color'
    String[] colors = constants.cwRadioButtonColors();
    for (int i = 0; i < colors.length; i++) {
      String color = colors[i];
      RadioButton radioButton = new RadioButton("color", color);
      radioButton.ensureDebugId("cwRadioButton-color-" + color);
      if (i == 2) {
        radioButton.setEnabled(false);
      }
      vPanel.add(radioButton);
    }

    // Add a new header to select your favorite sport
    vPanel.add(new HTML("<br>" + constants.cwRadioButtonSelectSport()));

    // Add some radio buttons to a group called 'sport'
    String[] sports = constants.cwRadioButtonSports();
    for (int i = 0; i < sports.length; i++) {
      String sport = sports[i];
      RadioButton radioButton = new RadioButton("sport", sport);
      radioButton.ensureDebugId(
          "cwRadioButton-sport-" + sport.replaceAll(" ", ""));
      if (i == 2) {
        radioButton.setValue(true);
      }
      vPanel.add(radioButton);
    }

    return vPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwRadioButton.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
