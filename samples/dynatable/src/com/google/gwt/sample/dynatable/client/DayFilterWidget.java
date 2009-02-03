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
package com.google.gwt.sample.dynatable.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A UI Widget that allows a user to filter the days being displayed in the
 * dynamic table.
 */
public class DayFilterWidget extends Composite {

  private class DayCheckBox extends CheckBox {
    public final int day;

    public DayCheckBox(String caption, int day) {
      super(caption);

      // Remember custom data for this widget.
      this.day = day;

      // Use a shared handler to save memory.
      addClickHandler(dayCheckBoxHandler);

      // Initialize based on the calendar's current value.
      setValue(calendar.getDayIncluded(day));
    }
  }

  private class DayCheckBoxHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      onClick((DayCheckBox) event.getSource());
    }

    public void onClick(DayCheckBox dayCheckBox) {
      calendar.setDayIncluded(dayCheckBox.day, dayCheckBox.getValue());
    }
  }

  private final SchoolCalendarWidget calendar;

  private final VerticalPanel outer = new VerticalPanel();

  private final DayCheckBoxHandler dayCheckBoxHandler = new DayCheckBoxHandler();

  public DayFilterWidget(SchoolCalendarWidget calendar) {
    this.calendar = calendar;
    initWidget(outer);
    setStyleName("DynaTable-DayFilterWidget");
    outer.add(new DayCheckBox("Sunday", 0));
    outer.add(new DayCheckBox("Monday", 1));
    outer.add(new DayCheckBox("Tuesday", 2));
    outer.add(new DayCheckBox("Wednesday", 3));
    outer.add(new DayCheckBox("Thursday", 4));
    outer.add(new DayCheckBox("Friday", 5));
    outer.add(new DayCheckBox("Saturday", 6));

    Button buttonAll = new Button("All", new ClickHandler() {
      public void onClick(ClickEvent event) {
        setAllCheckBoxes(true);
      }
    });

    Button buttonNone = new Button("None", new ClickHandler() {
      public void onClick(ClickEvent event) {
        setAllCheckBoxes(false);
      }
    });

    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
    hp.add(buttonAll);
    hp.add(buttonNone);

    outer.add(hp);
    outer.setCellVerticalAlignment(hp, HasAlignment.ALIGN_BOTTOM);
    outer.setCellHorizontalAlignment(hp, HasAlignment.ALIGN_CENTER);
  }

  private void setAllCheckBoxes(boolean checked) {
    for (int i = 0, n = outer.getWidgetCount(); i < n; ++i) {
      Widget w = outer.getWidget(i);
      if (w instanceof DayCheckBox) {
        ((DayCheckBox) w).setValue(checked);
        dayCheckBoxHandler.onClick((DayCheckBox) w);
      }
    }
  }
}
