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
package com.google.gwt.sample.mobilewebapp.client.ui;

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * A button that shows a {@link DatePicker} when clicked.
 */
public class DateButton extends Composite implements HasValue<Date>,
    IsEditor<LeafValueEditor<Date>> {

  private final Button button;
  private Date date;
  private final DateTimeFormat dateFormat = DateTimeFormat.getFormat("EE, MMM d, yyyy");
  private final DatePicker datePicker;
  private final PopupPanel datePickerPopup;
  private LeafValueEditor<Date> editor;

  public DateButton() {
    button = new Button();
    initWidget(button);

    // Create the DatePicker popup.
    datePicker = new DatePicker();
    datePickerPopup = new PopupPanel(true, false);
    datePickerPopup.setWidget(datePicker);
    datePickerPopup.setGlassEnabled(true);

    // Show the DatePicker popup on click.
    button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        datePicker.setValue(date, false);
        datePickerPopup.center();
      }
    });

    // Push the new value into the button.
    datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        setValue(event.getValue(), true);
        datePickerPopup.hide();
      }
    });

    // Set the default date.
    setValue(null);
  }

  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Date> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public LeafValueEditor<Date> asEditor() {
    if (editor == null) {
      editor = TakesValueEditor.of(this);
    }
    return editor;
  }

  public Date getValue() {
    return date;
  }

  public void setValue(Date value) {
    setValue(value, false);
  }

  public void setValue(Date value, boolean fireEvents) {
    Date oldValue = getValue();
    this.date = value;
    if (value == null) {
      button.setText("Set due date");
    } else {
      button.setText(dateFormat.format(date));
    }

    if (fireEvents) {
      ValueChangeEvent.fireIfNotEqual(this, oldValue, value);
    }
  }
}
