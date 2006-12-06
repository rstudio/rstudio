/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class DayFilterWidget extends Composite {

  private class DayCheckBox extends CheckBox {
    public DayCheckBox(String caption, int day) {
      super(caption);

      // Remember custom data for this widget.
      this.day = day;

      // Use a shared listener to save memory.
      addClickListener(dayCheckBoxListener);

      // Initialize based on the calendar's current value.
      setChecked(calendar.getDayIncluded(day));
    }

    public final int day;
  }

  private class DayCheckBoxListener implements ClickListener {
    public void onClick(Widget sender) {
      DayCheckBox dayCheckBox = ((DayCheckBox) sender);
      calendar.setDayIncluded(dayCheckBox.day, dayCheckBox.isChecked());
    }
  }

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

    Button buttonAll = new Button("All", new ClickListener() {
      public void onClick(Widget sender) {
        setAllCheckBoxes(true);
      }
    });

    Button buttonNone = new Button("None", new ClickListener() {
      public void onClick(Widget sender) {
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
        ((DayCheckBox) w).setChecked(checked);
        dayCheckBoxListener.onClick(w);
      }
    }
  }

  private final SchoolCalendarWidget calendar;
  private final VerticalPanel outer = new VerticalPanel();
  private final DayCheckBoxListener dayCheckBoxListener = new DayCheckBoxListener();
}
