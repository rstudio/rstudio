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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;

import java.util.Date;

/**
 * Model used to get calendar information for {@link DatePicker} and its
 * subclasses.
 */
@SuppressWarnings(/* Required to use Date API in gwt */{"deprecation"})
public class CalendarModel {

  /**
   * The number of weeks normally displayed in a month.
   */
  public static final int WEEKS_IN_MONTH = 6;

  /**
   * Number of days normally displayed in a week.
   */
  public static final int DAYS_IN_WEEK = 7;

  private static final String[] dayOfWeekNames = new String[7];

  private static String[] dayOfMonthNames = new String[32];

  private final Date currentMonth;

  /**
   * Constructor.
   */
  public CalendarModel() {
    currentMonth = new Date();

    CalendarUtil.setToFirstDayOfMonth(currentMonth);

    // Finding day of week names
    Date date = new Date();
    for (int i = 1; i <= 7; i++) {
      date.setDate(i);
      int dayOfWeek = date.getDay();
      dayOfWeekNames[dayOfWeek] = getDayOfWeekFormatter().format(date);
    }

    // Finding day of month names
    date.setMonth(0);

    for (int i = 1; i < 32; ++i) {
      date.setDate(i);
      dayOfMonthNames[i] = getDayOfMonthFormatter().format(date);
    }
  }

  /**
   * Formats the current specified month. For example "Sep".
   * 
   * @return the formatted month
   */
  public String formatCurrentMonth() {
    return getMonthAndYearFormatter().format(currentMonth);
  }

  /**
   * Formats a date's day of month. For example "1".
   * 
   * @param date the date
   * @return the formated day of month
   */
  public String formatDayOfMonth(Date date) {
    return dayOfMonthNames[date.getDate()];
  }

  /**
   * Format a day in the week. So, for example "Monday".
   * 
   * @param dayInWeek the day in week to format
   * @return the formatted day in week
   */
  public String formatDayOfWeek(int dayInWeek) {
    return dayOfWeekNames[dayInWeek];
  }

  /**
   * Gets the first day of the first week in the currently specified month.
   * 
   * @return the first day
   */
  public Date getCurrentFirstDayOfFirstWeek() {
    int wkDayOfMonth1st = currentMonth.getDay();
    int start = CalendarUtil.getStartingDayOfWeek();
    if (wkDayOfMonth1st == start) {
      // always return a copy to allow SimpleCalendarView to adjust first
      // display date
      return new Date(currentMonth.getTime());
    } else {
      Date d = new Date(currentMonth.getTime());
      int offset = wkDayOfMonth1st - start > 0 ? wkDayOfMonth1st - start
          : DAYS_IN_WEEK - (start - wkDayOfMonth1st);
      CalendarUtil.addDaysToDate(d, -offset);
      return d;
    }
  }

  /**
   * Gets the date representation of the currently specified month. Used to
   * access both the month and year information.
   * 
   * @return the month and year
   */
  public Date getCurrentMonth() {
    return currentMonth;
  }

  /**
   * Is a date in the currently specified month?
   * 
   * @param date the date
   * @return date
   */
  public boolean isInCurrentMonth(Date date) {
    return currentMonth.getMonth() == date.getMonth();
  }

  /**
   * Sets the currently specified date.
   * 
   * @param currentDate the currently specified date
   */
  public void setCurrentMonth(Date currentDate) {
    this.currentMonth.setYear(currentDate.getYear());
    this.currentMonth.setMonth(currentDate.getMonth());
  }

  /**
   * Shifts the currently specified date by the given number of months. The day
   * of the month will be pinned to the original value as far as possible.
   * 
   * @param deltaMonths - number of months to be added to the current date
   */
  public void shiftCurrentMonth(int deltaMonths) {
    CalendarUtil.addMonthsToDate(currentMonth, deltaMonths);
    refresh();
  }

  /**
   * Gets the date of month formatter.
   * 
   * @return the day of month formatter
   */
  protected DateTimeFormat getDayOfMonthFormatter() {
    return DateTimeFormat.getFormat("d");
  }

  /**
   * Gets the day of week formatter.
   * 
   * @return the day of week formatter
   */
  protected DateTimeFormat getDayOfWeekFormatter() {
    return DateTimeFormat.getFormat("ccccc");
  }

  /**
   * Gets the month and year formatter.
   * 
   * @return the month and year formatter
   */
  protected DateTimeFormat getMonthAndYearFormatter() {
    return DateTimeFormat.getFormat(PredefinedFormat.YEAR_MONTH_ABBR);
  }

  /**
   * Refresh the current model as needed.
   */
  protected void refresh() {
  }

}
