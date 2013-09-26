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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.Format;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * Tests {@link DateBox}.
 */
@DoNotRunWith({Platform.HtmlUnitBug})
public class DateBoxTest extends WidgetTestBase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAccessors() {
    DateBox db = new DateBox();
    assertFalse(db.isDatePickerShowing());
    db.showDatePicker();
    assertTrue(db.isDatePickerShowing());
    db.hideDatePicker();
    assertFalse(db.isDatePickerShowing());
  }

  private static class MillisStrippedValueChangeTester extends DateValueChangeTester {

    public MillisStrippedValueChangeTester(DateBox subject) {
      super(subject);
      subject.setValue(null); // reset to default
    }

    @Override
    protected void normalizeTime(Date highResolutionDate) {
      highResolutionDate.setTime((highResolutionDate.getTime() / 1000) * 1000);
    }
  }

  public void testValueChangeEvent() {
    final DateBox db = new DateBox();
    RootPanel.get().add(db);

    // Checks setValue(date, true). Should preserve precision so getValue returns the exact value
    // passed by setValue.
    new DateValueChangeTester(db).run();

    // Check setting the text directly in the text box.
    new MillisStrippedValueChangeTester(db) {
      @Override
      protected void fire(Date d) {
        enterViaTextBox(db, d);
      }
    }.run();

    // Checks that selecting a date from date picker works correctly.
    new MillisStrippedValueChangeTester(db) {
      @Override
      protected void fire(Date d) {
        selectViaDatePicker(db, d);
      }
    }.run();

    // Checks that setting the date picker's date works correctly.
    new MillisStrippedValueChangeTester(db) {
      @Override
      protected void fire(Date d) {
        setViaDatePicker(db, d);
      }
    }.run();
  }
  
  private static class TimeStrippedValueChangeTester extends DateValueChangeTester {

    public TimeStrippedValueChangeTester(DateBox subject) {
      super(subject);
      subject.setValue(null); // reset to default
    }

    @Override
    protected void normalizeTime(Date highResolutionDate) {
      CalendarUtil.resetTime(highResolutionDate);
    }
  }

  public void testValueChangeEventWithCustomFormat() {
    Format format = new DateBox.DefaultFormat(DateTimeFormat.getFormat("dd/MM/yyyy"));
    final DateBox db = new DateBox(new DatePicker(), null, format);
    RootPanel.get().add(db);

    // Checks setValue(date, true). Should preserve precision so getValue returns the exact value
    // passed by setValue.
    new DateValueChangeTester(db).run();

    // Check setting the text directly in the text box.
    new TimeStrippedValueChangeTester(db) {
      @Override
      protected void fire(Date d) {
        enterViaTextBox(db, d);
      }
    }.run();

    // Checks that setting the date picker's date works correctly.
    new TimeStrippedValueChangeTester(db) {
      @Override
      protected void fire(Date d) {
        setViaDatePicker(db, d);
      }
    }.run();

    // Checks that selecting a date from date picker works correctly.
    new TimeStrippedValueChangeTester(db) {
      @Override
      protected void fire(Date d) {
        selectViaDatePicker(db, d);
      }
    }.run();
  }


  private static void enterViaTextBox(DateBox db, Date d) {
    // Intended use of higher resolution formatter to test normalization
    db.getTextBox().setText(DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_LONG).format(d));
    db.getTextBox().getElement().dispatchEvent(Document.get().createBlurEvent());
  }

  private static void selectViaDatePicker(final DateBox db, Date d) {
    // To mimic selection first show the picker
    db.showDatePicker();
    db.getDatePicker().setValue(d, true);
  }

  private static void setViaDatePicker(DateBox db, Date d) {
    db.getDatePicker().setValue(d, true);
  }

  public void testFireNullValues() {
    DateBox db = new DateBox();
    db.setFireNullValues(true);
    assertTrue(db.getFireNullValues());
    RootPanel.get().add(db);
    @SuppressWarnings("unchecked")
    final ValueChangeEvent<Date>[] eventHolder = new ValueChangeEvent[1];
    final boolean[] wasCalled = new boolean[1];
    db.addValueChangeHandler(new ValueChangeHandler<Date>() {
      @Override
      public void onValueChange(ValueChangeEvent<Date> event) {
        wasCalled[0] = true;
        eventHolder[0] = event;
      }
    });

    // test that an empty string fires an event
    db.setValue(new Date(1976,1,20));
    db.getTextBox().setText("");
    NativeEvent e = Document.get().createBlurEvent();
    db.getTextBox().getElement().dispatchEvent(e);
    assertTrue(wasCalled[0]);
    assertNull(eventHolder[0].getValue());
    
    // test that an invalid date string fires an event, and leaves the text in
    // the textbox
    db.setValue(new Date(1976,1,20));
    db.getTextBox().setText("abcd");
    e = Document.get().createBlurEvent();
    db.getTextBox().getElement().dispatchEvent(e);
    assertTrue(wasCalled[0]);
    assertNull(eventHolder[0].getValue());
    assertEquals("abcd", db.getTextBox().getText());
  }
}
