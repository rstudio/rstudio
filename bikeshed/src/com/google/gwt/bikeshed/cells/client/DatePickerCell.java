/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.bikeshed.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * A {@link Cell} used to render and edit {@link Date}s. When a cell is selected
 * by clicking on it, a {@link DatePicker} is popped up. When a date is selected
 * using the DatePicker, the new date is passed to the
 * {@link ValueUpdater#update update} method of the {@link ValueUpdater} that
 * was passed to {@link #onBrowserEvent} for the click event. Note that this
 * means that the call to ValueUpdater.update will occur after onBrowserEvent
 * has returned. Pressing the 'escape' key dismisses the DatePicker popup
 * without calling ValueUpdater.update.
 *
 * <p>
 * Each DatePickerCell has a unique DatePicker popup associated with it; thus,
 * if a single DatePickerCell is used as the cell for a column in a table, only
 * one entry in that column will be editable at a given time.
 *
 * @param <V> the view data type
 */
public class DatePickerCell<V> extends Cell<Date, V> {

  private static final int ESCAPE = 27;

  private final DatePicker datePicker;
  private final DateTimeFormat format;
  private int offsetX = 10;
  private int offsetY = 10;
  private PopupPanel panel;
  private ValueUpdater<Date, V> valueUpdater;
  private V viewData;

  /**
   * Constructs a new DatePickerCell that uses the date/time format
   * given by {@link DateTimeFormat#getFullDateFormat}.
   */
  public DatePickerCell() {
    this(DateTimeFormat.getFullDateFormat());
  }

  /**
   * Constructs a new DatePickerCell that uses the given date/time format.
   */
  public DatePickerCell(DateTimeFormat format) {
    this.format = format;

    this.datePicker = new DatePicker();
    this.panel = new PopupPanel(true, true) {
      // Dismiss when escape is pressed
      @Override
      public boolean onKeyUpPreview(char key, int modifiers) {
        if (key == ESCAPE) {
          panel.hide();
        }
        return true;
      }
    };
    panel.add(datePicker);

    // Hide the panel and call valueUpdater.update when a date is selected
    datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        panel.hide();
        valueUpdater.update(event.getValue(), viewData);
      }
    });
  }

  @Override
  public V onBrowserEvent(final Element parent, Date value, V viewData,
      NativeEvent event, ValueUpdater<Date, V> valueUpdater) {
    if (event.getType().equals("click")) {
      this.viewData = viewData;
      this.valueUpdater = valueUpdater;

      datePicker.setValue(value);
      panel.setPopupPositionAndShow(new PositionCallback() {
        public void setPosition(int offsetWidth, int offsetHeight) {
          panel.setPopupPosition(parent.getAbsoluteLeft() + offsetX,
              parent.getAbsoluteTop() + offsetY);
        }
      });
    }
    return viewData;
  }

  @Override
  public void render(Date value, V viewData, StringBuilder sb) {
    if (value != null) {
      sb.append(format.format(value));
    }
  }
}
