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

import com.google.gwt.event.logical.shared.HighlightEvent;
import com.google.gwt.event.logical.shared.HighlightHandler;
import com.google.gwt.event.logical.shared.ShowRangeEvent;
import com.google.gwt.event.logical.shared.ShowRangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * Date picker demo.
 */
public class VisualsForDatePicker extends AbstractIssue {

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();
    final DatePicker picker = new DatePicker();
    p.add(picker);
    final Label value = new Label("value: ");
    p.add(value);
    final Label highlight = new Label("highlight: ");
    p.add(highlight);
    final Label range = new Label("range: ");
    p.add(range);
    picker.addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        value.setText("value: "
            + DateTimeFormat.getShortDateFormat().format(event.getValue()));
      }
    });
    picker.addHighlightHandler(new HighlightHandler<Date>() {

      @SuppressWarnings("deprecation")
      // Should never be seen, as highlight should be cloned.
      public void onHighlight(HighlightEvent<Date> event) {
        event.getHighlighted().setYear(1);
        picker.getHighlightedDate().setYear(1);
      }

    });
    picker.addHighlightHandler(new HighlightHandler<Date>() {
      public void onHighlight(HighlightEvent<Date> event) {
        highlight.setText("highlight: "
            + DateTimeFormat.getShortDateFormat().format(event.getHighlighted()));
      }
    });
    picker.addShowRangeHandler(new ShowRangeHandler<Date>() {
      public void onShowRange(ShowRangeEvent<Date> event) {
        Date start = event.getStart();
        Date end = event.getEnd();
        DateTimeFormat format = DateTimeFormat.getShortDateFormat();
        range.setText("range: " + format.format(start) + " - "
            + format.format(end));
      }
    });
    return p;
  };

  @Override
  public String getInstructions() {
    return "Go back one month, go forward one month, check that highlighting is working, and try selecting a date.";
  }

  @Override
  public String getSummary() {
    return "Visual test for date picker";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
