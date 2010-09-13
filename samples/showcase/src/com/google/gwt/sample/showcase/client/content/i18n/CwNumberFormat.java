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
package com.google.gwt.sample.showcase.client.content.i18n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".cw-RedText")
public class CwNumberFormat extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwNumberFormatDescription();

    String cwNumberFormatFailedToParseInput();

    String cwNumberFormatFormattedLabel();

    String cwNumberFormatInvalidPattern();

    String cwNumberFormatName();

    String cwNumberFormatPatternLabel();

    String[] cwNumberFormatPatterns();

    String cwNumberFormatValueLabel();
  }

  /**
   * The {@link NumberFormat} that is currently being applied.
   */
  private NumberFormat activeFormat = null;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * The {@link Label} where the formatted value is displayed.
   */
  @ShowcaseData
  private Label formattedBox = null;

  /**
   * The {@link TextBox} that displays the current pattern.
   */
  @ShowcaseData
  private TextBox patternBox = null;

  /**
   * The {@link ListBox} that holds the patterns.
   */
  @ShowcaseData
  private ListBox patternList = null;

  /**
   * The {@link TextBox} where the user enters a value.
   */
  @ShowcaseData
  private TextBox valueBox = null;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwNumberFormat(CwConstants constants) {
    super(constants.cwNumberFormatName(), constants.cwNumberFormatDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Use a Grid to layout the content
    Grid layout = new Grid(4, 2);
    layout.setCellSpacing(5);

    // Add a field to select the pattern
    patternList = new ListBox();
    patternList.setWidth("17em");
    String[] patterns = constants.cwNumberFormatPatterns();
    for (String pattern : patterns) {
      patternList.addItem(pattern);
    }
    patternList.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        updatePattern();
      }
    });
    layout.setHTML(0, 0, constants.cwNumberFormatPatternLabel());
    layout.setWidget(0, 1, patternList);

    // Add a field to display the pattern
    patternBox = new TextBox();
    patternBox.setWidth("17em");
    patternBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updatePattern();
      }
    });
    layout.setWidget(1, 1, patternBox);

    // Add a field to set the value
    valueBox = new TextBox();
    valueBox.setWidth("17em");
    valueBox.setText("31415926535.897932");
    valueBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updateFormattedValue();
      }
    });
    layout.setHTML(2, 0, constants.cwNumberFormatValueLabel());
    layout.setWidget(2, 1, valueBox);

    // Add a field to display the formatted value
    formattedBox = new Label();
    formattedBox.setWidth("17em");
    layout.setHTML(3, 0, constants.cwNumberFormatFormattedLabel());
    layout.setWidget(3, 1, formattedBox);

    // Return the layout Widget
    updatePattern();
    return layout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwNumberFormat.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  /**
   * Show an error message. Pass in null to clear the error message.
   *
   * @param errorMsg the error message
   */
  @ShowcaseSource
  private void showErrorMessage(String errorMsg) {
    if (errorMsg == null) {
      formattedBox.removeStyleName("cw-RedText");
    } else {
      formattedBox.setText(errorMsg);
      formattedBox.addStyleName("cw-RedText");
    }
  }

  /**
   * Update the formatted value based on the user entered value and pattern.
   */
  @ShowcaseSource
  private void updateFormattedValue() {
    String sValue = valueBox.getText();
    if (!sValue.equals("")) {
      try {
        double value = Double.parseDouble(sValue);
        String formattedValue = activeFormat.format(value);
        formattedBox.setText(formattedValue);
        showErrorMessage(null);
      } catch (NumberFormatException e) {
        showErrorMessage(constants.cwNumberFormatFailedToParseInput());
      }
    } else {
      formattedBox.setText("<None>");
    }
  }

  /**
   * Update the selected pattern based on the pattern in the list.
   */
  @ShowcaseSource
  private void updatePattern() {
    switch (patternList.getSelectedIndex()) {
      case 0:
        activeFormat = NumberFormat.getDecimalFormat();
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 1:
        activeFormat = NumberFormat.getCurrencyFormat();
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 2:
        activeFormat = NumberFormat.getScientificFormat();
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 3:
        activeFormat = NumberFormat.getPercentFormat();
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 4:
        patternBox.setEnabled(true);
        String pattern = patternBox.getText();
        try {
          activeFormat = NumberFormat.getFormat(pattern);
        } catch (IllegalArgumentException e) {
          showErrorMessage(constants.cwNumberFormatInvalidPattern());
          return;
        }
        break;
    }

    // Update the formatted value
    updateFormattedValue();
  }
}
