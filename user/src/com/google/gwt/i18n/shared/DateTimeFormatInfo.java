/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.shared;

/**
 * Information required for formatting and parsing localized date/time values.
 *
 * <p>Implementors should subclass {@link DefaultDateTimeFormatInfo} so when
 * methods are added they will get reasonable defaults and not break.  See the
 * same class for example values returned by these methods.
 */
public interface DateTimeFormatInfo {

  /**
   * Returns array of strings containing abbreviations for Ante Meridiem and
   * Post Meridiem.
   */
  String[] ampms();

  /**
   * Returns a safe default date format.
   */
  String dateFormat();

  /**
   * Returns a "full" date format.
   */
  String dateFormatFull();

  /**
   * Returns a "long" date format.
   */
  String dateFormatLong();

  /**
   * Returns a "medium" date format.
   */
  String dateFormatMedium();

  /**
   * Returns a "short" date format.
   */
  String dateFormatShort();

  /**
   * Returns a date/time format from a date format pattern and a time format
   * pattern, using the locale default joining.
   *
   * @param timePattern the time pattern String
   * @param datePattern the data pattern String
   */
  String dateTime(String timePattern, String datePattern);

  /**
   * Returns a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   *
   * @param timePattern the time pattern String
   * @param datePattern the data pattern String
   */
  String dateTimeFull(String timePattern, String datePattern);

  /**
   * Returns a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   *
   * @param timePattern the time pattern String
   * @param datePattern the data pattern String
   */
  String dateTimeLong(String timePattern, String datePattern);

  /**
   * Returns a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   *
   * @param timePattern the time pattern String
   * @param datePattern the data pattern String
   */
  String dateTimeMedium(String timePattern, String datePattern);

  /**
   * Returns a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   *
   * @param timePattern the time pattern String
   * @param datePattern the data pattern String
   */
  String dateTimeShort(String timePattern, String datePattern);

  /**
   * Returns an array of the full era names.
   */
  String[] erasFull();

  /**
   * Returns abbreviations of the era names.
   */
  String[] erasShort();

  /**
   * Returns the day which generally comes first in a weekly calendar view, as
   * an index into the return value of {@link #weekdaysFull()}.
   */
  int firstDayOfTheWeek();

  /**
   * Returns localized format equivalent to the "d" skeleton pattern.
   */
  String formatDay();

  /**
   * Returns localized format equivalent to the "hm" skeleton pattern.
   */
  String formatHour12Minute();

  /**
   * Returns localized format equivalent to the "hms" skeleton pattern.
   */
  String formatHour12MinuteSecond();

  /**
   * Returns localized format equivalent to the "Hm" skeleton pattern.
   */
  String formatHour24Minute();

  /**
   * Returns localized format equivalent to the "Hms" skeleton pattern.
   */
  String formatHour24MinuteSecond();

  /**
   * Returns localized format equivalent to the "ms" skeleton pattern.
   */
  String formatMinuteSecond();

  /**
   * Returns localized format equivalent to the "MMM" skeleton pattern.
   */
  String formatMonthAbbrev();

  /**
   * Returns localized format equivalent to the "MMMd" skeleton pattern.
   */
  String formatMonthAbbrevDay();

  /**
   * Returns localized format equivalent to the "MMMM" skeleton pattern.
   */
  String formatMonthFull();

  /**
   * Returns localized format equivalent to the "MMMMd" skeleton pattern.
   */
  String formatMonthFullDay();

  /**
   * Returns localized format equivalent to the "MMMMEEEEd" skeleton pattern.
   */
  String formatMonthFullWeekdayDay();

  /**
   * Returns localized format equivalent to the "Md" skeleton pattern.
   */
  String formatMonthNumDay();

  /**
   * Returns localized format equivalent to the "y" skeleton pattern.
   */
  String formatYear();

  /**
   * Returns localized format equivalent to the "yMMM" skeleton pattern.
   */
  String formatYearMonthAbbrev();

  /**
   * Returns localized format equivalent to the "yMMMd" skeleton pattern.
   */
  String formatYearMonthAbbrevDay();

  /**
   * Returns localized format equivalent to the "yMMMM" skeleton pattern.
   */
  String formatYearMonthFull();

  /**
   * Returns localized format equivalent to the "yMMMMd" skeleton pattern.
   */
  String formatYearMonthFullDay();

  /**
   * Returns localized format equivalent to the "yM" skeleton pattern.
   */
  String formatYearMonthNum();

  /**
   * Returns localized format equivalent to the "yMd" skeleton pattern.
   */
  String formatYearMonthNumDay();

  /**
   * Returns localized format equivalent to the "yMMMEEEd" skeleton pattern.
   */
  String formatYearMonthWeekdayDay();

  /**
   * Returns localized format equivalent to the "yQQQQ" skeleton pattern.
   */
  String formatYearQuarterFull();

  /**
   * Returns localized format equivalent to the "yQ" skeleton pattern.
   */
  String formatYearQuarterShort();

  /**
   * Returns an array of full month names.
   */
  String[] monthsFull();

  /**
   * Returns an array of month names for use in a stand-alone context.
   */
  String[] monthsFullStandalone();

  /**
   * Returns an array of the shortest abbreviations for months, typically a
   * single character and not guaranteed to be unique.
   */
  String[] monthsNarrow();

  /**
   * Returns an array of the shortest abbreviations for months suitable for use
   * in a stand-alone context, typically a single character and not guaranteed
   * to be unique.
   */
  String[] monthsNarrowStandalone();

  /**
   * Returns an array of month abbreviations.
   */
  String[] monthsShort();

  /**
   * Returns an array of month abbreviations, suitable for use in a stand-alone
   * context.
   */
  String[] monthsShortStandalone();

  /**
   * Returns an array of full quarter names.
   */
  String[] quartersFull();

  /**
   * Returns an array of abbreviations for quarters.
   */
  String[] quartersShort();

  /**
   * Returns a safe default time format.
   */
  String timeFormat();

  /**
   * Returns a "full" time format.
   */
  String timeFormatFull();

  /**
   * Returns a "long" time format.
   */
  String timeFormatLong();

  /**
   * Returns a "medium" time format.
   */
  String timeFormatMedium();

  /**
   * Returns a "short" time format.
   */
  String timeFormatShort();

  /**
   * Returns an array of the full names of weekdays.
   */
  String[] weekdaysFull();

  /**
   * Returns an array of the full names of weekdays, suitable for use in a
   * stand-alone context.
   */
  String[] weekdaysFullStandalone();

  /**
   * Returns an array of the shortest abbreviations for weekdays, typically a
   * single character and not guaranteed to be unique.
   */
  String[] weekdaysNarrow();

  /**
   * Returns an array of the shortest abbreviations for weekdays suitable for
   * use in a stand-alone context, typically a single character and not
   * guaranteed to be unique.
   */
  String[] weekdaysNarrowStandalone();

  /**
   * Returns an array of abbreviations for weekdays.
   */
  String[] weekdaysShort();

  /**
   * Returns an array of abbreviations for weekdays, suitable for use in a
   * stand-alone context.
   */
  String[] weekdaysShortStandalone();

  /**
   * Returns the day which ends the weekend, as an index into the return value
   * of {@link #weekdaysFull()}.
   *
   * <p>Note that this value may be numerically less than
   * {@link #weekendStart()} - for example, {@link #weekendStart()} of 6 and
   * {@link #weekendEnd()} of 0 means Saturday and Sunday are the weekend.
   */
  int weekendEnd();

  /**
   * Returns the day which starts the weekend, as an index into the return value
   * of {@link #weekdaysFull()}.
   */
  int weekendStart();
}
