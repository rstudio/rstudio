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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;

import java.util.Date;

/**
 * Visuals for date box.
 */
public class VisualsForDateBox extends AbstractIssue {
  class FormatWithNewYearsEve extends DefaultFormat {
    public FormatWithNewYearsEve() {
    }

    public FormatWithNewYearsEve(DateTimeFormat format) {
      super(format);
    }

    @Override
    public String format(DateBox box, Date d) {
      if (d == null) {
        return "Please Change me";
      } else {
        return super.format(box, d);
      }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Date parse(DateBox box, String input, boolean reportError) {
      if (input.equalsIgnoreCase("new year's eve")) {
        Date d = new Date();
        d.setDate(31);
        d.setMonth(12);
        return d;
      } else {
        return super.parse(box, input, reportError);
      }
    }
  }

  @Override
  public Widget createIssue() {
    VerticalPanel v = new VerticalPanel();
    v.add(new HTML("<div style='height:25px'></div>"));
    v.add(dateRange());
    v.add(new HTML("<div style='height:25px'></div>"));
    return v;
  }

  @Override
  public String getInstructions() {
    return "Instructions <ul><li>Click on first date box, see that date picker is displayed</li> "
        + "<li>use arrow keys to navigate to second date box, select a date.</li> "
        + "<li>type in a bad date then click back to the first date box. Your bad date should now be in red</li>"
        + "<li>get back to the second box, now type in a valid date and tab away, its text should now be black again. </li>"
        + "<li>Try typing 'New Year's Eve' in on the start datebox)</li>"
        + "<li> Hit 'Show values' and confirm that you see the correct values</li></ul>";
  }

  @Override
  public String getSummary() {
    return "date box visual test";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private Widget dateRange() {
    VerticalPanel v = new VerticalPanel();
    HorizontalPanel p = new HorizontalPanel();
    v.add(p);
    final DateBox start = new DateBox(new DatePicker(), null,
        new FormatWithNewYearsEve());

    start.setWidth("13em");
    final DateBox end = new DateBox();
    end.setWidth("13em");

    end.getDatePicker().addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        start.removeStyleName("user-modified");
      }
    });

    final TextBox startText = start.getTextBox();
    startText.addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent e) {
        if (e.isRightArrow()
            && start.getCursorPos() == startText.getText().length()) {
          start.hideDatePicker();
          end.setFocus(true);
        }
      }
    });

    end.getTextBox().addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent e) {
        if ((e.isLeftArrow()) && end.getCursorPos() == 0) {
          end.hideDatePicker();
          start.setFocus(true);
        }
      }
    });

    end.setValue(new Date());
    p.add(start);
    Label l = new Label(" - ");
    l.setStyleName("filler");
    p.add(l);
    p.add(end);
    final Label value = new Label();
    p.add(value);
    HorizontalPanel h2 = new HorizontalPanel();
    v.add(h2);
    h2.add(new Button("Short format", new ClickHandler() {
      public void onClick(ClickEvent event) {
        updateFormat(start, end, DateTimeFormat.getShortDateFormat());
      }
    }));
    h2.add(new Button("Long format", new ClickHandler() {

      public void onClick(ClickEvent event) {
        updateFormat(start, end, DateTimeFormat.getMediumDateFormat());
      }
    }));

    h2.add(new Button("Clear", new ClickHandler() {
      public void onClick(ClickEvent sender) {
        start.setValue(null);
        end.setValue(null);
      }
    }));

    h2.add(new Button("Show Values", new ClickHandler() {
      public void onClick(ClickEvent event) {
        DateTimeFormat f = DateTimeFormat.getShortDateFormat();
        Date d1 = start.getValue();
        Date d2 = end.getValue();
        value.setText("Start: \"" + (d1 == null ? "null" : f.format(d1))
            + "\" End: \"" + (d2 == null ? "null" : f.format(d2)) + "\"");
      }
    }));

    EventReporter<Date, DateBox> reporter = new EventReporter<Date, DateBox>();
    start.addValueChangeHandler(reporter);
    end.addValueChangeHandler(reporter);
    reporter.report("Events are logged here");
    v.add(reporter);
    return v;
  }

  private void updateFormat(DateBox start, DateBox end, DateTimeFormat format) {
    // You can replace the format itself.
    start.setFormat(new FormatWithNewYearsEve(format));
    end.setFormat(new DefaultFormat(format));
  }
}