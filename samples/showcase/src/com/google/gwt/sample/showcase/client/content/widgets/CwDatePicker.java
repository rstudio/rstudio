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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * Example file.
 */
@ShowcaseStyle({
    ".gwt-DatePicker", ".datePicker", "td.datePickerMonth", ".gwt-DateBox",
    ".dateBox"})
public class CwDatePicker extends ContentWidget {

  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwDatePickerBoxLabel();

    String cwDatePickerDescription();

    String cwDatePickerLabel();

    String cwDatePickerName();
  }

  /**
   * {@link ValueChangeHandler} used to set the value in the text box when the user selects a date
   */
  @ShowcaseSource
  public static class MyDateValueChangeHandler implements ValueChangeHandler<Date> {
    private final Label text;

    public MyDateValueChangeHandler(Label text) {
      this.text = text;
    }

    public void onValueChange(ValueChangeEvent<Date> event) {
      Date date = event.getValue();
      String dateString = DateTimeFormat.getMediumDateFormat().format(date);
      text.setText(dateString);
    }
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
  public CwDatePicker(CwConstants constants) {
    super(constants.cwDatePickerName(), constants.cwDatePickerDescription(),
        true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @SuppressWarnings("deprecation")
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a basic date picker
    DatePicker datePicker = new DatePicker();
    final Label text = new Label();

    // Set the value in the text box when the user selects a date
    datePicker.addValueChangeHandler(new MyDateValueChangeHandler(text));

    // create a date picker where years and months are selectable with drop down lists and where we
    // can navigate trough the years
    DatePicker advancedDatePicker = new DatePicker();
    advancedDatePicker.setYearArrowsVisible(true);
    advancedDatePicker.setYearAndMonthDropdownVisible(true);
    // show 51 years in the years dropdown. The range of years is centered on the selected date
    advancedDatePicker.setVisibleYearCount(51);

    final Label text2 = new Label();
    text2.getElement().getStyle().setMarginTop(15, Unit.PX);

    // Set the value in the text box when the user selects a date
    advancedDatePicker.addValueChangeHandler(new MyDateValueChangeHandler(text2));

    // Set the default value
    datePicker.setValue(new Date(), true);
    advancedDatePicker.setValue(new Date(), true);

    // Create a DateBox
    DateTimeFormat dateFormat = DateTimeFormat.getLongDateFormat();
    DateBox dateBox = new DateBox();
    dateBox.setFormat(new DateBox.DefaultFormat(dateFormat));
    dateBox.getDatePicker().setYearArrowsVisible(true);

    // Combine the widgets into a panel and return them
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(new HTML(constants.cwDatePickerLabel()));
    vPanel.add(text);
    vPanel.add(datePicker);
    vPanel.add(text2);
    vPanel.add(advancedDatePicker);
    vPanel.add(new HTML(constants.cwDatePickerBoxLabel()));
    vPanel.add(dateBox);
    return vPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwDatePicker.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
