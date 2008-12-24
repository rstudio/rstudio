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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.datepicker.client.CalendarModel;
import com.google.gwt.user.datepicker.client.CalendarView;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwt.user.datepicker.client.DefaultMonthSelector;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests DatePicker's public api.
 */
@SuppressWarnings("deprecation")
// Due to Date
public class DatePickerTest extends GWTTestCase {

  private static class DatePickerWithView extends DatePicker {
    DatePickerWithView(MockCalendarView view) {
      super(new DefaultMonthSelector(), view, new CalendarModel());
    }
  }
  /**
   * Mock calendar view pretends to show datesVisibleList from the first of the month to
   * the 30th day after that.
   */
  private static class MockCalendarView extends CalendarView {
    Map<Date, Set<String>> dateStyles = new HashMap<Date, Set<String>>();
    Set<Date> disabledDates = new HashSet<Date>();

    MockCalendarView() {
      initWidget(new Label());
    }

    @Override
    public void addStyleToDate(String styleName, Date date) {
      Set<String> fred = dateStyles.get(date);
      if (fred == null) {
        fred = new HashSet<String>();
        dateStyles.put(date, fred);
      }
      fred.add(styleName);
    }

    @Override
    public Date getFirstDate() {
      Date thisMonth = getModel().getCurrentMonth();
      return new Date(thisMonth.getYear(), thisMonth.getMonth(), 1);
    }

    @Override
    public Date getLastDate() {
      Date thisMonth = getModel().getCurrentMonth();
      return new Date(thisMonth.getYear(), thisMonth.getMonth(), 30);
    }

    @Override
    public boolean isDateEnabled(Date date) {
      return !disabledDates.contains(date);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void removeStyleFromDate(String styleName, Date date) {
      Set<String> fred;
      assertNotNull(fred = dateStyles.get(date));
      assertTrue(fred.remove(styleName));
    }

    @Override
    public void setEnabledOnDate(boolean enabled, Date date) {
      if (enabled) {
        disabledDates.remove(date);
      } else {
        disabledDates.add(date);
      }
    }

    @Override
    protected void setup() {
    }
  }

  private static final String STYLE_LATER = "styleLater";

  private static final String STYLE = "style1";

  private DatePickerWithView mockedDatePicker;
  private MockCalendarView view;

  private final Date dateVisible1 = new Date(65, 6, 12);
  private final Date dateVisible2 = new Date(65, 6, 13);
  private final Date dateVisible3 = new Date(65, 6, 14);
  private final Date dateVisible4 = new Date(65, 6, 15);
  private final Date dateVisible5 = new Date(65, 6, 16);
  private final List<Date> datesVisibleList = new ArrayList<Date>();
  private final Date dateLater1 =
      new Date(dateVisible1.getYear(), dateVisible1.getMonth() + 1,
          dateVisible1.getDay());

  private final Date dateLater2 =
      new Date(dateVisible2.getYear(), dateVisible2.getMonth() + 1,
          dateVisible2.getDay());
  private final Date dateLater3 =
      new Date(dateVisible1.getYear(), dateVisible3.getMonth() + 1,
          dateVisible3.getDay());
  private final Date dateLater4 =
      new Date(dateVisible2.getYear(), dateVisible4.getMonth() + 1,
          dateVisible4.getDay());
  private final Date dateLater5 =
      new Date(dateVisible1.getYear(), dateVisible5.getMonth() + 1,
          dateVisible5.getDay());
  private final List<Date> datesLaterList = new ArrayList<Date>();
  {
    datesVisibleList.add(dateVisible4);
    datesVisibleList.add(dateVisible5);
  }
  {
    datesLaterList.add(dateLater4);
    datesLaterList.add(dateLater5);
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();

    view = new MockCalendarView();
    mockedDatePicker = new DatePickerWithView(view);
    mockedDatePicker.setCurrentMonth(dateVisible1);
  }

  public void testDisabling() {
    mockedDatePicker.setTransientEnabledOnDates(false, dateVisible1);
    mockedDatePicker.setTransientEnabledOnDates(false, dateVisible2,
        dateVisible3);
    mockedDatePicker.setTransientEnabledOnDates(false, datesVisibleList);
    
    assertTrue(view.disabledDates.contains(dateVisible1));
    assertTrue(view.disabledDates.contains(dateVisible2));
    assertTrue(view.disabledDates.contains(dateVisible3));
    assertTrue(view.disabledDates.contains(dateVisible4));
    assertTrue(view.disabledDates.contains(dateVisible5));

    mockedDatePicker.setTransientEnabledOnDates(true, dateVisible1);
    mockedDatePicker.setTransientEnabledOnDates(true, dateVisible2,
        dateVisible3);
    mockedDatePicker.setTransientEnabledOnDates(true, datesVisibleList);

    assertFalse(view.disabledDates.contains(dateVisible1));
    assertFalse(view.disabledDates.contains(dateVisible2));
    assertFalse(view.disabledDates.contains(dateVisible3));
    assertFalse(view.disabledDates.contains(dateVisible4));
    assertFalse(view.disabledDates.contains(dateVisible5));
  }

  public void testStyleSetting() {
    mockedDatePicker.addStyleToDates(STYLE, dateVisible1);
    mockedDatePicker.addStyleToDates(STYLE, dateVisible2, dateVisible3);
    mockedDatePicker.addStyleToDates(STYLE, datesVisibleList);

    assertViewHasStyleOnVisibleDates(STYLE);
    assertPickerHasStyleOnVisibleDates(STYLE);

    // See that styles on an invisible datesVisibleList don't

    mockedDatePicker.addStyleToDates(STYLE_LATER, dateLater1);
    mockedDatePicker.addStyleToDates(STYLE_LATER, dateLater2, dateLater3);
    mockedDatePicker.addStyleToDates(STYLE_LATER, datesLaterList);

    assertViewHasNoStyleOnHiddenDates();
    assertPickerLacksStyleOnHiddenDates(STYLE_LATER);

    // Remove a style from a visible date, and it should leave the view too
    mockedDatePicker.removeStyleFromDates(STYLE, dateVisible1);
    mockedDatePicker.removeStyleFromDates(STYLE, dateVisible2, dateVisible3);
    mockedDatePicker.removeStyleFromDates(STYLE, datesVisibleList);

    assertViewLacksStyleOnVisibleDates(STYLE);
    assertPickerLacksStyleOnVisibleDates();

    // Remove a style from an invisible date, and the view should not hear
    // about it (the mock will explode if asked to remove a style it doesn't
    // have)
    mockedDatePicker.removeStyleFromDates(STYLE_LATER, dateLater1);
    mockedDatePicker.removeStyleFromDates(STYLE_LATER, dateLater2, dateLater3);
    mockedDatePicker.removeStyleFromDates(STYLE_LATER, datesLaterList);
    assertPickerHasNoStyleOnInvisibleDates();
  }

  public void testTransientStyles() {
    mockedDatePicker.addTransientStyleToDates(STYLE, dateVisible1);
    mockedDatePicker.addTransientStyleToDates(STYLE, dateVisible2,
        dateVisible3);
    mockedDatePicker.addTransientStyleToDates(STYLE, datesVisibleList);
    assertViewHasStyleOnVisibleDates(STYLE);
    assertPickerLacksStyleOnVisibleDates();
    
    mockedDatePicker.removeStyleFromDates(STYLE, dateVisible1);
    mockedDatePicker.removeStyleFromDates(STYLE, dateVisible2, dateVisible3);
    mockedDatePicker.removeStyleFromDates(STYLE, datesVisibleList);
    assertViewLacksStyleOnVisibleDates(STYLE);
    assertPickerLacksStyleOnVisibleDates();
  }
  
  public void testValueChangeEvent() {
    DatePicker dp = new DatePicker();
    RootPanel.get().add(dp);
    new DateValueChangeTester(dp).run();
  }
  
  public void testValueStyle() {
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible4));
    
    mockedDatePicker.setValue(dateVisible4);
    assertTrue(mockedDatePicker.getStyleOfDate(dateVisible4).contains("datePickerDayIsValue"));
    assertTrue(view.dateStyles.get(dateVisible4).contains("datePickerDayIsValue"));

    mockedDatePicker.setValue(dateVisible5);
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible4));
    assertFalse(view.dateStyles.get(dateVisible4).contains("datePickerDayIsValue"));
  }

  private void assertPickerHasNoStyleOnInvisibleDates() {
    assertNull(mockedDatePicker.getStyleOfDate(dateLater1));
    assertNull(mockedDatePicker.getStyleOfDate(dateLater2));
    assertNull(mockedDatePicker.getStyleOfDate(dateLater3));
    assertNull(mockedDatePicker.getStyleOfDate(dateLater4));
    assertNull(mockedDatePicker.getStyleOfDate(dateLater5));
  }

  private void assertPickerHasStyleOnVisibleDates(String style) {
    assertTrue(mockedDatePicker.getStyleOfDate(dateVisible1).contains(
        style));
    assertTrue(mockedDatePicker.getStyleOfDate(dateVisible2).contains(
        style));
    assertTrue(mockedDatePicker.getStyleOfDate(dateVisible3).contains(
        style));
    assertTrue(mockedDatePicker.getStyleOfDate(dateVisible4).contains(
        style));
    assertTrue(mockedDatePicker.getStyleOfDate(dateVisible5).contains(
        style));
  }

  private void assertPickerLacksStyleOnHiddenDates(String styleLater) {
    assertTrue(mockedDatePicker.getStyleOfDate(dateLater1).contains(
        styleLater));
    assertTrue(mockedDatePicker.getStyleOfDate(dateLater2).contains(
        styleLater));
    assertTrue(mockedDatePicker.getStyleOfDate(dateLater3).contains(
        styleLater));
    assertTrue(mockedDatePicker.getStyleOfDate(dateLater4).contains(
        styleLater));
    assertTrue(mockedDatePicker.getStyleOfDate(dateLater5).contains(
        styleLater));
  }

  private void assertPickerLacksStyleOnVisibleDates() {
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible1));
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible2));
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible3));
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible4));
    assertNull(mockedDatePicker.getStyleOfDate(dateVisible5));
  }

  private void assertViewHasNoStyleOnHiddenDates() {
    assertNull(view.dateStyles.get(dateLater1));
    assertNull(view.dateStyles.get(dateLater2));
    assertNull(view.dateStyles.get(dateLater3));
    assertNull(view.dateStyles.get(dateLater4));
    assertNull(view.dateStyles.get(dateLater5));
  }

  private void assertViewHasStyleOnVisibleDates(String style) {
    assertTrue(view.dateStyles.get(dateVisible1).contains(style));
    assertTrue(view.dateStyles.get(dateVisible2).contains(style));
    assertTrue(view.dateStyles.get(dateVisible3).contains(style));
    assertTrue(view.dateStyles.get(dateVisible4).contains(style));
    assertTrue(view.dateStyles.get(dateVisible5).contains(style));
  }

  private void assertViewLacksStyleOnVisibleDates(String style) {
    assertFalse(view.dateStyles.get(dateVisible1).contains(style));
    assertFalse(view.dateStyles.get(dateVisible2).contains(style));
    assertFalse(view.dateStyles.get(dateVisible3).contains(style));
    assertFalse(view.dateStyles.get(dateVisible4).contains(style));
    assertFalse(view.dateStyles.get(dateVisible5).contains(style));
  }
}
