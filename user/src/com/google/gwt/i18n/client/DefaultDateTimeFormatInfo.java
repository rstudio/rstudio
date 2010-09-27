/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.i18n.client;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA

/**
 * Default implementation of DateTimeFormatInfo interface, using values from
 * the CLDR root locale.
 * <p>
 * Users who need to create their own DateTimeFormatInfo implementation are
 * encouraged to extend this class so their implementation won't break when   
 * new methods are added.
 */
public class DefaultDateTimeFormatInfo implements DateTimeFormatInfo {

  public String[] ampms() {
    return new String[] {
        "AM",
        "PM"
    };
  }

  public String dateFormat() {
    return dateFormatMedium();
  }

  public String dateFormatFull() {
    return "EEEE, y MMMM dd";
  }

  public String dateFormatLong() {
    return "y MMMM d";
  }

  public String dateFormatMedium() {
    return "y MMM d";
  }

  public String dateFormatShort() {
    return "yyyy-MM-dd";
  }

  public String dateTime(String timePattern, String datePattern) {
    return dateTimeMedium(timePattern, datePattern);
  }

  public String dateTimeFull(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(" ").append(timePattern).toString();
  }

  public String dateTimeLong(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(" ").append(timePattern).toString();
  }

  public String dateTimeMedium(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(" ").append(timePattern).toString();
  }

  public String dateTimeShort(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(" ").append(timePattern).toString();
  }

  public String[] erasFull() {
    return new String[] {
        "Before Christ",
        "Anno Domini"
    };
  }

  public String[] erasShort() {
    return new String[] {
        "BC",
        "AD"
    };
  }

  public int firstDayOfTheWeek() {
    return 1;
  }

  public String formatDay() {
    return "d";
  }

  public String formatHour12Minute() {
    return "h:mm a";
  }

  public String formatHour12MinuteSecond() {
    return "h:mm:ss a";
  }

  public String formatHour24Minute() {
    return "HH:mm";
  }

  public String formatHour24MinuteSecond() {
    return "HH:mm:ss";
  }

  public String formatMinuteSecond() {
    return "mm:ss";
  }

  public String formatMonthAbbrev() {
    return "LLL";
  }

  public String formatMonthAbbrevDay() {
    return "MMM d";
  }

  public String formatMonthFull() {
    return "LLLL";
  }

  public String formatMonthFullDay() {
    return "MMMM d";
  }

  public String formatMonthFullWeekdayDay() {
    return "EEEE MMMM d";
  }

  public String formatMonthNumDay() {
    return "M-d";
  }

  public String formatYear() {
    return "y";
  }

  public String formatYearMonthAbbrev() {
    return "y MMM";
  }

  public String formatYearMonthAbbrevDay() {
    return "y MMM d";
  }

  public String formatYearMonthFull() {
    return "y MMMM";
  }

  public String formatYearMonthFullDay() {
    return "y MMMM d";
  }

  public String formatYearMonthNum() {
    return "y-M";
  }

  public String formatYearMonthNumDay() {
    return "y-M-d";
  }

  public String formatYearMonthWeekdayDay() {
    return "EEE, y MMM d";
  }

  public String formatYearQuarterFull() {
    return "y QQQQ";
  }

  public String formatYearQuarterShort() {
    return "y Q";
  }

  public String[] monthsFull() {
    return new String[] {
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    };
  }

  public String[] monthsFullStandalone() {
    return monthsFull();
  }

  public String[] monthsNarrow() {
    return new String[] {
        "J",
        "F",
        "M",
        "A",
        "M",
        "J",
        "J",
        "A",
        "S",
        "O",
        "N",
        "D"
    };
  }

  public String[] monthsNarrowStandalone() {
    return monthsNarrow();
  }

  public String[] monthsShort() {
    return new String[] {
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec"
    };
  }

  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  public String[] quartersFull() {
    return new String[] {
        "1st quarter",
        "2nd quarter",
        "3rd quarter",
        "4th quarter"
    };
  }

  public String[] quartersShort() {
    return new String[] {
        "Q1",
        "Q2",
        "Q3",
        "Q4"
    };
  }

  public String timeFormat() {
    return timeFormatMedium();
  }

  public String timeFormatFull() {
    return "HH:mm:ss zzzz";
  }

  public String timeFormatLong() {
    return "HH:mm:ss z";
  }

  public String timeFormatMedium() {
    return "HH:mm:ss";
  }

  public String timeFormatShort() {
    return "HH:mm";
  }

  public String[] weekdaysFull() {
    return new String[] {
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    };
  }

  public String[] weekdaysFullStandalone() {
    return weekdaysFull();
  }

  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "M",
        "T",
        "W",
        "T",
        "F",
        "S"
    };
  }

  public String[] weekdaysNarrowStandalone() {
    return weekdaysNarrow();
  }

  public String[] weekdaysShort() {
    return new String[] {
        "Sun",
        "Mon",
        "Tue",
        "Wed",
        "Thu",
        "Fri",
        "Sat"
    };
  }

  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }

  public int weekendEnd() {
    return 0;
  }

  public int weekendStart() {
    return 6;
  }
}
