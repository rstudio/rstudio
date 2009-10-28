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

package com.google.gwt.user.datepicker.client;

import com.google.gwt.user.client.ui.Composite;

/**
 * Package protected class used to combine functionality for the
 * {@link MonthSelector} and {@link CalendarView} components.
 * 
 */
abstract class DatePickerComponent extends Composite {
  private DatePicker datePicker;

  public CalendarModel getModel() {
    return datePicker.getModel();
  }

  protected void addMonths(int numMonths) {
    getModel().shiftCurrentMonth(numMonths);
    getDatePicker().refreshAll();
  }

  protected DatePicker getDatePicker() {
    return datePicker;
  }

  /**
   * Refresh the component. Usually called because the model's current date has
   * changed. In general, only should be called by {@link DatePicker}. Use
   * refreshAll() if you need to refresh all components.
   */
  protected abstract void refresh();

  /**
   * Refreshes the {@link DatePicker}, {@link CalendarView}, and
   * {@link CalendarModel}.
   */
  protected void refreshAll() {
    getDatePicker().refreshAll();
  }

  /**
   * Set up the component.
   */
  protected abstract void setup();

  DatePicker.StandardCss css() {
    return datePicker.css();
  }

  void setDatePicker(DatePicker me) {
    this.datePicker = me;
  }
}
