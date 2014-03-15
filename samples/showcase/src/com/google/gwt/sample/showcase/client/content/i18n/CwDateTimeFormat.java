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
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;

/**
 * Example file.
 */
@ShowcaseStyle(".cw-RedText")
public class CwDateTimeFormat extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwDateTimeFormatDescription();

    String cwDateTimeFormatFailedToParseInput();

    String cwDateTimeFormatFormattedLabel();

    String cwDateTimeFormatInvalidPattern();

    String cwDateTimeFormatName();

    String cwDateTimeFormatPatternLabel();

    String[] cwDateTimeFormatPatterns();

    String cwDateTimeFormatValueLabel();
  }

  /**
   * The {@link DateTimeFormat} that is currently being applied.
   */
  private DateTimeFormat activeFormat = null;

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
  public CwDateTimeFormat(CwConstants constants) {
    super(constants.cwDateTimeFormatName(),
        constants.cwDateTimeFormatDescription(), true);
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
    CellFormatter formatter = layout.getCellFormatter();
    layout.setCellSpacing(5);

    // Add a field to select the pattern
    patternList = new ListBox();
    patternList.setWidth("17em");
    String[] patterns = constants.cwDateTimeFormatPatterns();
    for (String pattern : patterns) {
      patternList.addItem(pattern);
    }
    patternList.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        updatePattern();
      }
    });
    layout.setHTML(0, 0, constants.cwDateTimeFormatPatternLabel());
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
    valueBox.setText("13 September 1999 12:34:56");
    valueBox.addKeyUpHandler(new KeyUpHandler() {

      public void onKeyUp(KeyUpEvent event) {
        updateFormattedValue();
      }

    });

    layout.setHTML(2, 0, constants.cwDateTimeFormatValueLabel());
    layout.setWidget(2, 1, valueBox);

    // Add a field to display the formatted value
    formattedBox = new Label();
    formattedBox.setWidth("17em");
    layout.setHTML(3, 0, constants.cwDateTimeFormatFormattedLabel());
    layout.setWidget(3, 1, formattedBox);
    formatter.setVerticalAlignment(3, 0, HasVerticalAlignment.ALIGN_TOP);

    // Return the layout Widget
    updatePattern();
    return layout;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwDateTimeFormat.class, new RunAsyncCallback() {

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
  @SuppressWarnings("deprecation")
  @ShowcaseSource
  private void updateFormattedValue() {
    String sValue = valueBox.getText();
    if (!sValue.equals("")) {
      try {
        Date date = new Date(sValue);
        String formattedValue = activeFormat.format(date);
        formattedBox.setText(formattedValue);
        showErrorMessage(null);
      } catch (IllegalArgumentException e) {
        showErrorMessage(constants.cwDateTimeFormatFailedToParseInput());
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
      // Date + Time
      case 0:
        activeFormat = DateTimeFormat.getFormat(
            PredefinedFormat.DATE_TIME_FULL);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;

      case 1:
        activeFormat = DateTimeFormat.getFormat(
            PredefinedFormat.DATE_TIME_LONG);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 2:
        activeFormat = DateTimeFormat.getFormat(
            PredefinedFormat.DATE_TIME_MEDIUM);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 3:
        activeFormat = DateTimeFormat.getFormat(
            PredefinedFormat.DATE_TIME_SHORT);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;

      // Date only
      case 4:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;

      case 5:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 6:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 7:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;

      // Time only
      case 8:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.TIME_FULL);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;

      case 9:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 10:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.TIME_MEDIUM);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;
      case 11:
        activeFormat = DateTimeFormat.getFormat(PredefinedFormat.TIME_SHORT);
        patternBox.setText(activeFormat.getPattern());
        patternBox.setEnabled(false);
        break;

      // Custom
      case 12:
        patternBox.setEnabled(true);
        String pattern = patternBox.getText();
        try {
          activeFormat = DateTimeFormat.getFormat(pattern);
        } catch (IllegalArgumentException e) {
          showErrorMessage(constants.cwDateTimeFormatInvalidPattern());
          return;
        }
        break;
    }

    // Update the formatted value
    updateFormattedValue();
  }
}
