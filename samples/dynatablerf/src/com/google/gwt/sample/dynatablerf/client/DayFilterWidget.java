/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A UI Widget that allows a user to filter the days being displayed in the
 * dynamic table.
 */
public class DayFilterWidget extends Composite {

  interface Binder extends UiBinder<Widget, DayFilterWidget> {
  };

  @UiField(provided = true)
  final SchoolCalendarWidget calendar;

  @UiField
  DayCheckBox sunday;
  @UiField
  DayCheckBox monday;
  @UiField
  DayCheckBox tuesday;
  @UiField
  DayCheckBox wednesday;
  @UiField
  DayCheckBox thursday;
  @UiField
  DayCheckBox friday;
  @UiField
  DayCheckBox saturday;
  @UiField
  Button all;
  @UiField
  Button none;

  private final DayCheckBox[] allDays;

  public DayFilterWidget(SchoolCalendarWidget calendar) {
    this.calendar = calendar;
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));
    allDays = new DayCheckBox[] {
        sunday, monday, tuesday, wednesday, thursday, friday, saturday};
  }

  @UiHandler(value = {"all", "none"})
  public void handleAllNoneClick(ClickEvent e) {
    setAllCheckBoxes(all == e.getSource());
  }

  @UiHandler(value = {
      "sunday", "monday", "tuesday", "wednesday", "thursday", "friday",
      "saturday"})
  public void handleClick(ValueChangeEvent<Boolean> e) {
    DayCheckBox box = (DayCheckBox) e.getSource();
    onToggle(box);
  }

  private void onToggle(DayCheckBox box) {
    calendar.setDayIncluded(box.getDay(), box.getValue());
  }

  private void setAllCheckBoxes(boolean checked) {
    for (DayCheckBox box : allDays) {
      box.setValue(checked);
      onToggle(box);
    }
  }
}
