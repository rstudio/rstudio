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

/**
 * Information required for formatting and parsing localized date/time values.
 * 
 * <p>Implementors should subclass {@link DefaultDateTimeFormatInfo} so when
 * methods are added they will get reasonable defaults and not break.  See the
 * same class for example values returned by these methods.
 */
public interface DateTimeFormatInfo {

  /**
   * @return array of strings containing abbreviations for Ante Meridiem and
   * Post Meridiem.
   */
  String[] ampms();

  /**
   * @return a safe default date format.
   */
  String dateFormat();

  /**
   * @return a "full" date format.
   */
  String dateFormatFull();

  /**
   * @return a "long" date format.
   */
  String dateFormatLong();

  /**
   * @return a "medium" date format.
   */
  String dateFormatMedium();

  /**
   * @return a "short" date format.
   */
  String dateFormatShort();

  /**
   * Construct a date/time format from a date format pattern and a time format
   * pattern, using the locale default joining.
   * @param timePattern
   * @param datePattern
   * 
   * @return a date/time format
   */
  String dateTime(String timePattern, String datePattern);

  /**
   * Construct a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   * @param timePattern
   * @param datePattern
   * 
   * @return a date/time format
   */
  String dateTimeFull(String timePattern, String datePattern);

  /**
   * Construct a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   * @param timePattern
   * @param datePattern
   * 
   * @return a date/time format
   */
  String dateTimeLong(String timePattern, String datePattern);

  /**
   * Construct a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   * @param timePattern
   * @param datePattern
   * 
   * @return a date/time format
   */
  String dateTimeMedium(String timePattern, String datePattern);

  /**
   * Construct a date/time format from a date format pattern and a time format
   * pattern, using "full" joining.
   * 
   * @param datePattern
   * @param timePattern
   * @return a date/time format
   */
  String dateTimeShort(String datePattern, String timePattern);

  /**
   * @return an array of the full era names.
   */
  String[] erasFull();

  /**
   * @return abbreviations of the era names.
   */
  String[] erasShort();

  /**
   * @return the day which generally comes first in a weekly calendar view, as
   *     an index into the return value of {@link #weekdaysFull()}.
   */
  int firstDayOfTheWeek();

  /**
   * @return localized format equivalent to the "d" skeleton pattern.
   */
  String formatDay();

  /**
   * @return localized format equivalent to the "hm" skeleton pattern.
   */
  String formatHour12Minute();

  /**
   * @return localized format equivalent to the "hms" skeleton pattern.
   */
  String formatHour12MinuteSecond();

  /**
   * @return localized format equivalent to the "Hm" skeleton pattern.
   */
  String formatHour24Minute();

  /**
   * @return localized format equivalent to the "Hms" skeleton pattern.
   */
  String formatHour24MinuteSecond();

  /**
   * @return localized format equivalent to the "ms" skeleton pattern.
   */
  String formatMinuteSecond();

  /**
   * @return localized format equivalent to the "MMM" skeleton pattern.
   */
  String formatMonthAbbrev();

  /**
   * @return localized format equivalent to the "MMMd" skeleton pattern.
   */
  String formatMonthAbbrevDay();

  /**
   * @return localized format equivalent to the "MMMM" skeleton pattern.
   */
  String formatMonthFull();

  /**
   * @return localized format equivalent to the "MMMMd" skeleton pattern.
   */
  String formatMonthFullDay();

  /**
   * @return localized format equivalent to the "MMMMEEEEd" skeleton pattern.
   */
  String formatMonthFullWeekdayDay();

  /**
   * @return localized format equivalent to the "Md" skeleton pattern.
   */
  String formatMonthNumDay();

  /**
   * @return localized format equivalent to the "y" skeleton pattern.
   */
  String formatYear();

  /**
   * @return localized format equivalent to the "yMMM" skeleton pattern.
   */
  String formatYearMonthAbbrev();

  /**
   * @return localized format equivalent to the "yMMMd" skeleton pattern.
   */
  String formatYearMonthAbbrevDay();

  /**
   * @return localized format equivalent to the "yMMMM" skeleton pattern.
   */
  String formatYearMonthFull();

  /**
   * @return localized format equivalent to the "yMMMMd" skeleton pattern.
   */
  String formatYearMonthFullDay();

  /**
   * @return localized format equivalent to the "yM" skeleton pattern.
   */
  String formatYearMonthNum();

  /**
   * @return localized format equivalent to the "yMd" skeleton pattern.
   */
  String formatYearMonthNumDay();

  /**
   * @return localized format equivalent to the "yMMMEEEd" skeleton pattern.
   */
  String formatYearMonthWeekdayDay();

  /**
   * @return localized format equivalent to the "yQQQQ" skeleton pattern.
   */
  String formatYearQuarterFull();

  /**
   * @return localized format equivalent to the "yQ" skeleton pattern.
   */
  String formatYearQuarterShort();

  /**
   * @return an array of full month names.
   */
  String[] monthsFull();

  /**
   * @return an array of month names for use in a stand-alone context.
   */
  String[] monthsFullStandalone();

  /**
   * @return an array of the shortest abbreviations for months, typically a
   *     single character and not guaranteed to be unique.
   */
  String[] monthsNarrow();

  /**
   * @return an array of the shortest abbreviations for months suitable for use
   *     in a stand-alone context, typically a single character and not
   *     guaranteed to be unique.
   */
  String[] monthsNarrowStandalone();

  /**
   * @return an array of month abbreviations.
   */
  String[] monthsShort();

  /**
   * @return an array of month abbreviations, suitable for use in a stand-alone
   *     context.
   */
  String[] monthsShortStandalone();

  /**
   * @return an array of full quarter names.
   */
  String[] quartersFull();

  /**
   * @return an array of abbreviations for quarters.
   */
  String[] quartersShort();

  /**
   * @return a safe default time format.
   */
  String timeFormat();

  /**
   * @return a "full" time format.
   */
  String timeFormatFull();

  /**
   * @return a "long" time format.
   */
  String timeFormatLong();

  /**
   * @return a "medium" time format.
   */
  String timeFormatMedium();

  /**
   * @return a "short" time format.
   */
  String timeFormatShort();

  /**
   * @return an array of the full names of weekdays.
   */
  String[] weekdaysFull();

  /**
   * @return an array of the full names of weekdays, suitable for use in a
   *     stand-alone context.
   */
  String[] weekdaysFullStandalone();

  /**
   * @return an array of the shortest abbreviations for weekdays, typically a
   *     single character and not guaranteed to be unique.
   */
  String[] weekdaysNarrow();

  /**
   * @return an array of the shortest abbreviations for weekdays suitable for
   *     use in a stand-alone context, typically a single character and not
   *     guaranteed to be unique.
   */
  String[] weekdaysNarrowStandalone();

  /**
   * @return an array of abbreviations for weekdays.
   */
  String[] weekdaysShort();

  /**
   * @return an array of abbreviations for weekdays, suitable for use in a
   *     stand-alone context.
   */
  String[] weekdaysShortStandalone();

  /**
   * @return the day which starts the weekend, as an index into the return value
   *     of {@link #weekdaysFull()}.
   */
  int weekendEnd();

  /**
   * @return the day which ends the weekend, as an index into the return value
   *     of {@link #weekdaysFull()}.  Note that this value may be numerically less
   *     than {@link #weekendEnd()} - for example, {@link #weekendEnd()} of 6
   *     and {@link #weekendStart()} of 0 means Saturday and Sunday are the
   *     weekend.
   */
  int weekendStart();
}