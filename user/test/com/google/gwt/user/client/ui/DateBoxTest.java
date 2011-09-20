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
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.datepicker.client.DateBox;

import java.util.Date;

/**
 * Tests {@link DateBox}.
 */
public class DateBoxTest extends WidgetTestBase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testAccessors() {
    DateBox db = new DateBox();
    assertFalse(db.isDatePickerShowing());
    db.showDatePicker();
    assertTrue(db.isDatePickerShowing());
    db.hideDatePicker();
    assertFalse(db.isDatePickerShowing());
  }

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testValueChangeEvent() {

    // Checks setValue(date, true);
    DateBox db = new DateBox();
    RootPanel.get().add(db);
    new DateValueChangeTester(db).run();

    // Check setting the text directly in the text box.
    final DateBox db2 = new DateBox();
    RootPanel.get().add(db2);
    new DateValueChangeTester(db2) {
      @Override
      protected void fire(java.util.Date d) {
        db2.getTextBox().setText(db2.getFormat().format(db2, d));
        NativeEvent e = Document.get().createBlurEvent();
        db2.getTextBox().getElement().dispatchEvent(e);
      }
    }.run();

    // Checks that setting the date picker's date works correctly.
    final DateBox db3 = new DateBox();
    RootPanel.get().add(db3);
    new DateValueChangeTester(db3) {
      @Override
      protected void fire(java.util.Date d) {
        db3.getDatePicker().setValue(d, true);
      }
    }.run();
  }
  
  @DoNotRunWith({Platform.HtmlUnitBug})
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
