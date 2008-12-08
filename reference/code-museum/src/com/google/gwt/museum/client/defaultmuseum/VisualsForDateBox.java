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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwt.user.datepicker.client.DateBox.InvalidDateReporter;

import java.util.Date;

/**
 * Visuals for date box.
 */
public class VisualsForDateBox extends AbstractIssue {

  @Override
  public Widget createIssue() {
    VerticalPanel v = new VerticalPanel();
    v.add(new HTML("<div style='height:25px'></div>"));
    v.add(dateRange());
    v.add(new HTML("<div style='height:25px'></div>"));
    final Label startErrors = makeErrorLabel();

    Widget errorReportingDateBox = dateRange(new DateBox.InvalidDateReporter() {
      public void clearError() {
        startErrors.setText("");
      }
      public void reportError(String input) {
        startErrors.setText("\"" + input + "\" is not a date");
      }
    });

    v.add(errorReportingDateBox);
    v.add(startErrors);

    return v;
  }

  @Override
  public String getInstructions() {
    return "Click on first date box, see that date picker is displayed, "
        + "use arrow keys to navigate to second date box, select a date. "
        + "The second set includes an error display (one, shared by both "
        + "fields). See that it notices  when you type garbage, and that "
        + "its error message is cleared when you empty the field or provide "
        + "a valid date.";
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
    return dateRange(null);
  }

  private Widget dateRange(InvalidDateReporter invalidDateReporter) {
    VerticalPanel v = new VerticalPanel();
    HorizontalPanel p = new HorizontalPanel();
    v.add(p);
    final DateBox start = newDateBox(invalidDateReporter);
    start.setWidth("13em");
    final DateBox end = newDateBox(invalidDateReporter);
    end.setWidth("13em");

    end.getDatePicker().addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        start.removeStyleName("user-modified");
      }
    });

    start.setValue(new Date());

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
        start.setDateFormat(DateTimeFormat.getShortDateFormat());
        end.setDateFormat(DateTimeFormat.getShortDateFormat());
      }
    }));
    h2.add(new Button("Long format", new ClickHandler() {

      public void onClick(ClickEvent event) {
        start.setDateFormat(DateTimeFormat.getLongDateFormat());
        end.setDateFormat(DateTimeFormat.getLongDateFormat());
      }
    }));

    h2.add(new Button("Clear", new ClickHandler() {
      public void onClick(ClickEvent sender) {
        start.setValue(null);
        end.setValue(null);
      }
    }));
    
    h2.add(new Button("Get Value", new ClickHandler() {
      public void onClick(ClickEvent event) {
        DateTimeFormat f = DateTimeFormat.getShortDateFormat();
        Date d1 = start.getValue();
        Date d2 = end.getValue();
        value.setText("Start: \"" 
            + (d1 == null ? "null" : f.format(d1))
            + "\" End: \"" 
            + (d2 == null ? "null" : f.format(d2)) 
            + "\"");
      }
    }));
    return v;
  }

  private Label makeErrorLabel() {
    final Label startErrors = new Label();
    startErrors.getElement().getStyle().setProperty("color", "red");
    return startErrors;
  }

  private DateBox newDateBox(InvalidDateReporter invalidDateReporter) {
    DateBox dateBox =
        invalidDateReporter == null ? new DateBox() : new DateBox(
            new DatePicker(), invalidDateReporter);
    return dateBox;
  }
}
