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
package com.google.gwt.emultest.java.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Date;

/**
 * Tests for GWT's emulation of the JRE Date class.
 */
@SuppressWarnings("deprecation")
public class DateTest extends GWTTestCase {
  public static final String CURRENT = "CURRENT";
  public static final String TO_STRING_PATTERN = "\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2}( .+)? \\d{4}";
  public static final long DAY_MILLISECONDS_SHIFT = 27;
  public static final String FUTURE = "FUTURE";
  public static final String PAST = "PAST";
  public static final long SECOND_MILLISECONDS_SHIFT = 10;

  Date theDate = new Date();

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /** Testing for public boolean java.util.Date.after(java.util.Date). */
  public void testAfter() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    Date arg10 = create();
    boolean a0 = accum0.after(arg10);
    assertFalse(a0);
    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    Date arg20 = create();
    boolean a1 = accum1.after(arg20);
    assertFalse(a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    Date arg30 = create();
    boolean a2 = accum2.after(arg30);
    assertTrue(a2);
  }

  /** Testing for public boolean java.util.Date.before(java.util.Date). */
  public void testBefore() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    Date arg10 = create();
    boolean a0 = accum0.before(arg10);
    assertFalse(a0);
    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    Date arg20 = create();
    boolean a1 = accum1.before(arg20);
    assertTrue(a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    Date arg30 = create();
    boolean a2 = accum2.before(arg30);
    assertFalse(a2);
  }

  /**
   * Tests that if daylight savings time occurs tomorrow, the current date isn't
   * affected.
   */
  public void testClockForwardNextDay() {
    int[] monthDayHour = new int[3];
    if (!findClockForwardTime(2009, monthDayHour)) {
      return;
    }

    int month = monthDayHour[0];
    int day = monthDayHour[1] - 1; // Day before.
    int hour = monthDayHour[2];
    Date d = new Date(2009 - 1900, month, day, hour, 0, 0);
    assertEquals(day, d.getDate());
    assertEquals(hour, d.getHours());

    // Change the minutes, which triggers fixDaylightSavings.
    d.setMinutes(10);
    assertEquals(day, d.getDate());
    assertEquals(hour, d.getHours());

    // Change the seconds, which triggers fixDaylightSavings.
    d.setSeconds(10);
    assertEquals(day, d.getDate());
    assertEquals(hour, d.getHours());

    // Change the minutes by more than an hour.
    d.setMinutes(80);
    assertEquals(day, d.getDate());
    assertEquals(hour + 1, d.getHours());
  }

  /** Testing for public java.lang.Object java.util.Date.clone(). */
  public void testClone() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    Object a0 = accum0.clone();
    assertFalse(a0 == accum0);
    assertEquals(a0, accum0);
    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    Object a1 = accum1.clone();
    assertFalse(a1 == accum1);
    assertEquals(a1, accum1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    Object a2 = accum2.clone();
    assertFalse(a2 == accum2);
    assertEquals(a2, accum2);
  }

  /** Testing for public int java.util.Date.compareTo(java.util.Date). */
  public void testCompareTo() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    Date arg10 = create();
    int a0 = accum0.compareTo(arg10);
    assertEquals(a0, 0);
    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create();
    Date arg20 = create(PAST);
    int a1 = accum1.compareTo(arg20);
    assertEquals(a1, 1);
    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create();
    Date arg30 = create(FUTURE);
    int a2 = accum2.compareTo(arg30);
    assertEquals(a2, -1);
  }

  /** Testing for public int java.util.Date.getDate(). */
  public void testGetDate() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getDate();
    assertEquals(4, a1);
    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getDate();
    assertEquals(29, a2);
  }

  /** Testing for public int java.util.Date.getDay(). */
  public void testGetDay() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    int a0 = accum0.getDay();

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getDay();

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getDay();
  }

  /** Testing for public int java.util.Date.getHours(). */
  public void testGetHours() {
    // Cannot be done because each time zone will give a different
    // answer
  }

  /** Testing for public int java.util.Date.getMinutes(). */
  public void testGetMinutes() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getMinutes();
    assertEquals(a1, 0);
    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getMinutes();
    assertEquals(a2, 4);
  }

  /** Testing for public int java.util.Date.getMonth(). */
  public void testGetMonth() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getMonth();
    assertEquals(0, a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getMonth();
    assertEquals(11, a2);
  }

  /** Testing for public int java.util.Date.getSeconds(). */
  public void testGetSeconds() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getSeconds();
    assertEquals(0, a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getSeconds();
    assertEquals(5, a2);
  }

  /** Testing for public long java.util.Date.getTime(). */
  public void testGetTime() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    long a1 = accum1.getTime();
    assertEquals(-2839795200000L, a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    long a2 = accum2.getTime();
    assertEquals(1924830245000L, a2);
  }

  /** Testing for public int java.util.Date.getTimezoneOffset(). */
  public void testGetTimezoneOffset() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    int a0 = accum0.getTimezoneOffset();

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getTimezoneOffset();

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getTimezoneOffset();
  }

  /** Testing for public int java.util.Date.getYear(). */
  public void testGetYear() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int a1 = accum1.getYear();
    assertEquals(a1, -20);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int a2 = accum2.getYear();
    assertEquals(130, a2);
  }

  /**
   * Testing to that if we set the day number to 31 for a month that only has 30
   * days in it, that the date rolls over to the first day of the next month in
   * sequence.
   */
  public void testInvalidDateForMonth() {
    int monthNum = 3; // April
    int numDaysInOldMonth = 30;
    int newDayNum = 31;
    Date dateWithThirtyDays = new Date(2006, monthNum, 30);
    dateWithThirtyDays.setDate(newDayNum);
    assertEquals(dateWithThirtyDays.getMonth(), monthNum + 1);
    assertEquals(dateWithThirtyDays.getDate(), newDayNum - numDaysInOldMonth);
  }

  /** Testing for public static long java.util.Date.parse(java.lang.String). */
  public void testParse() {
    try {
      Date.parse(null);
      fail("Should have thrown exception");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      Date.parse("");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    String arg10 = createString(CURRENT);
    long a0 = Date.parse(arg10);
    assertEquals(roundToDay(accum0.getTime()), roundToDay(a0));
    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    String arg20 = createString(PAST);
    long a1 = Date.parse(arg20);
    assertEquals(-2840140800000L, a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    String arg30 = createString(FUTURE);
    long a2 = Date.parse(arg30);
    assertEquals(1924830245000L, a2);
  }

  /** Testing for public void java.util.Date.setDate(int). */
  public void testSetDate() {
    // We only go through dates from 0-28 here. There are some months that do
    // not
    // have 29, 30, or 31 days - so our assertion would be wrong in the cases
    // where
    // the current month did not have 29,30,or 31 days
    for (int i = 1; i < 29; i++) {
      Date accum0 = create();
      accum0.setDate(i);
      assertEquals(accum0.getDate(), i);
    }
  }

  /** Testing for public void java.util.Date.setHours(int). */
  public void testSetHours() {
    for (int i = 0; i < 24; i++) {
      Date accum0 = create();
      if (isDst(accum0)) {
        // This test fails on the day of DST, so skip it.
        return;
      }
      accum0.setHours(i);
      assertEquals(accum0.getHours(), i);
    }
  }

  /**
   * We want to test to see that if we are currently in a month with 31 days and
   * we set the month to one which has less than 31 days, that the month
   * returned by the date class will be one higher than the month that we
   * originally set (according to the spec of java.util.date).
   */
  public void testSetInvalidMonthForDate() {
    int dayNum = 31;
    int newMonthNum = 1;
    int numDaysInNewMonth = 28;
    Date dateWithThirtyOneDays = new Date(2006, 12, dayNum);
    dateWithThirtyOneDays.setMonth(newMonthNum);
    assertEquals(dateWithThirtyOneDays.getMonth(), newMonthNum + 1);
    assertEquals(dateWithThirtyOneDays.getDate(), dayNum - numDaysInNewMonth);
  }

  /**
   * We want to test to see that if the date is Feb 29th (in a leap year) and we
   * set the year to a non-leap year, that the month and day will roll over to
   * March 1st.
   */
  public void testSetInvalidYearForDate() {
    int dayNum = 29;
    int monthNum = 1; // February
    int newYearNum = 2005;
    int numDaysInFebInNewYear = 28;
    Date leapYearDate = new Date(2004, monthNum, dayNum);
    leapYearDate.setYear(newYearNum);
    assertEquals(leapYearDate.getYear(), newYearNum);
    assertEquals(leapYearDate.getMonth(), monthNum + 1);
    assertEquals(leapYearDate.getDate(), dayNum - numDaysInFebInNewYear);
  }

  /** Testing for public void java.util.Date.setMinutes(int). */
  public void testSetMinutes() {
    for (int i = 0; i < 24; i++) {
      Date accum0 = create();
      accum0.setMinutes(i);
      assertEquals(accum0.getMinutes(), i);
    }
  }

  /** Testing for public void java.util.Date.setMonth(int). */
  public void testSetMonth() {
    for (int i = 0; i < 12; i++) {
      // We want to use a fixed date here. If we use the current date, the
      // assertion may fail
      // when the date is the 29th, 30th, or 31st, and we set the month to one
      // which does
      // not have 29, 30, or 31 days in it, respectively.
      Date accum0 = new Date(2006, 12, 1);
      accum0.setMonth(i);
      assertEquals(accum0.getMonth(), i);
    }
  }

  /** Testing for public void java.util.Date.setSeconds(int). */
  public void testSetSeconds() {
    for (int i = 0; i < 24; i++) {
      Date accum0 = create();
      accum0.setSeconds(i);
      assertEquals(accum0.getSeconds(), i);
    }
  }

  /** Testing for public void java.util.Date.setTime(long). */
  public void testSetTime() {
    long[] values = new long[] {-100000000000L, -100L, 0, 100L, 1000000000L};
    for (int i = 0; i < values.length; i++) {
      Date accum0 = create();
      accum0.setTime(values[i]);
      assertEquals(accum0.getTime(), values[i]);
    }
  }

  /**
   * We want to test to see that if the date is Feb 29th (in a leap year) and we
   * set the year to another leap year, that the month and day will be retained.
   */
  public void testSetValidLeapYearForDate() {
    int dayNum = 29;
    int monthNum = 1; // February
    int yearNum = 2004;
    int newYearNum = yearNum + 4;
    Date leapYearDate = new Date(yearNum, monthNum, dayNum);
    leapYearDate.setYear(newYearNum);
    assertEquals(leapYearDate.getYear(), newYearNum);
    assertEquals(leapYearDate.getMonth(), monthNum);
    assertEquals(leapYearDate.getDate(), dayNum);
  }

  /** Testing for public void java.util.Date.setYear(int). */
  public void testSetYear() {
    for (int i = 1880; i < 2030; i++) {
      // We want to use a fixed date here. If we use the current date, the
      // assertion may fail
      // when the date is February 29th, and we set the year to a non-leap year
      Date accum0 = new Date(2006, 12, 01);
      accum0.setYear(i);
      assertEquals(accum0.getYear(), i);
    }
  }

  /** Testing for public java.lang.String java.util.Date.toGMTString(). */
  public void testToGMTString() {

    // We can't rely on the JRE's toString, as it is an implementation detail.
    if (GWT.isScript()) {
      // /////////////////////////////
      // Past
      // /////////////////////////////
      Date accum1 = create(PAST);
      String a1 = accum1.toGMTString();
      assertEquals("5 Jan 1880 00:00:00 GMT", a1);

      // /////////////////////////////
      // Future
      // /////////////////////////////
      Date accum2 = create(FUTURE);
      String a2 = accum2.toGMTString();
      assertEquals("30 Dec 2030 03:04:05 GMT", a2);
    }
  }

  /** Testing for public java.lang.String java.util.Date.toLocaleString(). */
  public void testToLocaleString() {

    // We can't rely on the JRE's toString, as it is an implementation detail.
    if (GWT.isScript()) {
      // /////////////////////////////
      // Past
      // /////////////////////////////
      Date accum1 = create(PAST);
      String a1 = accum1.toLocaleString();
      assertTrue(a1.indexOf("1880") != -1);
      // /////////////////////////////
      // Future
      // /////////////////////////////
      Date accum2 = create(FUTURE);
      String a2 = accum2.toLocaleString();
      assertTrue(a2.indexOf("2030") != -1);
    }
  }

  /** Date docs specify an exact format for toString(). */
  public void testToString() {

    // We can't rely on the JRE's toString, as it is an implementation detail.
    if (GWT.isScript()) {
      // /////////////////////////////
      // Past
      // /////////////////////////////
      Date d = create(PAST);
      String s = d.toString();

      assertTrue("Bad format " + s, s.matches(TO_STRING_PATTERN));
      assertEquals("Parsing returned unequal dates from " + s, d, new Date(
          Date.parse(s)));

      // /////////////////////////////
      // Future
      // /////////////////////////////
      d = create(FUTURE);
      s = d.toString();

      assertTrue("Bad format " + s, s.matches(TO_STRING_PATTERN));
      assertEquals("Parsing returned unequal dates from " + s, d, new Date(
          Date.parse(s)));
    }
  }

  /** Testing for public static long java.util.Date.UTC(int,int,int,int,int,int). */
  public void testUTC() {

    // /////////////////////////////
    // Current
    // /////////////////////////////
    Date accum0 = create();
    int arg10 = 0;
    int arg11 = 0;
    int arg12 = 0;
    int arg13 = 0;
    int arg14 = 0;
    int arg15 = 0;
    long a0 = accum0.UTC(arg10, arg11, arg12, arg13, arg14, arg15);

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    int arg20 = 0;
    int arg21 = 0;
    int arg22 = 0;
    int arg23 = 0;
    int arg24 = 0;
    int arg25 = 0;
    long a1 = accum1.UTC(arg20, arg21, arg22, arg23, arg24, arg25);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    int arg30 = 0;
    int arg31 = 0;
    int arg32 = 0;
    int arg33 = 0;
    int arg34 = 0;
    int arg35 = 0;
    long a2 = accum2.UTC(arg30, arg31, arg32, arg33, arg34, arg35);
  }

  // Month and date of days with time shifts
  private ArrayList<Integer> timeShiftMonth = new ArrayList<Integer>();
  private ArrayList<Integer> timeShiftDate = new ArrayList<Integer>();
  
  private boolean containsTimeShift(Date start, int days) {
    long startTime = start.getTime();
    Date end = new Date();
    end.setTime(startTime);
    end.setDate(start.getDate() + days);
    long endTime = end.getTime();
    return (endTime - startTime) != ((long) days * 24 * 60 * 60 * 1000);
  }

  private void findTimeShift(Date start, int days) {
    assertTrue(days != 0);

    // Found a shift day
    if (days == 1) {
      timeShiftMonth.add(start.getMonth());
      timeShiftDate.add(start.getDate());
      return;
    }
    
    // Recurse over the first half of the period
    if (containsTimeShift(start, days / 2)) {
      findTimeShift(start, days / 2);
    }
    
    // Recurse over the second half of the period
    Date mid = new Date();
    mid.setTime(start.getTime());
    mid.setDate(start.getDate() + days / 2);
    if (containsTimeShift(mid, days - days / 2)) {
      findTimeShift(mid, days - days / 2);
    }
  }

  private void findTimeShifts(int year) {
    timeShiftMonth.clear();
    timeShiftDate.clear();
    Date start = new Date(year - 1900, 0, 1, 12, 0, 0);
    Date end = new Date(year + 1 - 1900, 0, 1, 12, 0, 0);
    int days = (int) ((end.getTime() - start.getTime()) /
        (24 * 60 * 60 * 1000));
    findTimeShift(start, days);
  }

  private boolean findClockBackwardTime(int year, int[] monthDayHour) {
    findTimeShifts(year);
    int numShifts = timeShiftMonth.size();
    for (int i = 0; i < numShifts; i++) {
      int month = timeShiftMonth.get(i);
      int day = timeShiftDate.get(i);

      long start = new Date(year - 1900, month, day, 0, 30, 0).getTime();
      long end = new Date(year - 1900, month, day + 1, 23, 30, 0).getTime();
      int lastHour = -1;
      for (long time = start; time < end; time += 60 * 60 * 1000) {
        Date d = new Date();
        d.setTime(time);
        int hour = d.getHours();
        if (hour == lastHour) {
          monthDayHour[0] = d.getMonth();
          monthDayHour[1] = d.getDate();
          monthDayHour[2] = d.getHours();
          return true;
        }
        lastHour = hour;
      }
    }

    return false;
  }

  private boolean findClockForwardTime(int year, int[] monthDayHour) {
    findTimeShifts(year);
    int numShifts = timeShiftMonth.size();
    for (int i = 0; i < numShifts; i++) {
      int month = timeShiftMonth.get(i);
      int startDay = timeShiftDate.get(i);
      
      for (int day = startDay; day <= startDay + 1; day++) {
        for (int hour = 0; hour < 24; hour++) {
          Date d = new Date(year - 1900, month, day, hour, 0, 0);
          int h = d.getHours();
          if ((h % 24) == ((hour + 1) % 24)) {
            monthDayHour[0] = month;
            monthDayHour[1] = day;
            monthDayHour[2] = hour;
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Check if daylight saving time occurs on the date.
   * 
   * @param date the date to check
   * @return true if DST occurs on the date, false if not
   */
  private boolean isDst(Date date) {
    int[] monthDayHour = new int[3];
    if (!findClockForwardTime(date.getYear() + 1900, monthDayHour)) {
      return false;
    }
    return monthDayHour[0] == date.getMonth()
        && monthDayHour[1] == date.getDate();
  }

  public void testClockBackwardTime() {
    int[] monthDayHour = new int[3];
    if (!findClockBackwardTime(2009, monthDayHour)) {
      return;
    }
    
    Date d;
    int month = monthDayHour[0];
    int day = monthDayHour[1];
    int hour = monthDayHour[2];
    
    // Check that this is the later of the two times having the
    // same hour:minute:second
    d = new Date(2009 - 1900, month, day, hour, 30, 0);
    assertEquals(hour, d.getHours());
    d.setTime(d.getTime() - 60 * 60 * 1000);
    assertEquals(hour, d.getHours());
  }
  
  public void testClockForwardTime() {
    int[] monthDayHour = new int[3];
    if (!findClockForwardTime(2009, monthDayHour)) {
      return;
    }
    
    Date d;
    int month = monthDayHour[0];
    int day = monthDayHour[1];
    int hour = monthDayHour[2];
    
    d = new Date(2009 - 1900, month, day, hour, 0, 0);
    assertEquals(hour + 1, d.getHours());
    
    // Test year change -- assume the previous year changes on a different day
    d = new Date(2008 - 1900, month, day, hour, 0, 0);
    assertEquals(hour, d.getHours());
    d.setYear(2009 - 1900);
    assertEquals(hour + 1, d.getHours());
    
    // Test month change
    d = new Date(2009 - 1900, month + 1, day, hour, 0, 0);
    assertEquals(hour, d.getHours());
    d.setMonth(month);
    assertEquals(3, d.getHours());
    
    // Test day change
    d = new Date(2009 - 1900, month, day + 1, hour, 0, 0);
    assertEquals(hour, d.getHours());
    d.setDate(day);
    assertEquals(hour + 1, d.getHours());
    
    // Test hour setting
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    assertEquals(hour + 2, d.getHours());
    d.setHours(hour);
    assertEquals(hour + 1, d.getHours());
    
    // Test changing hour by minutes = +- 60
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    assertEquals(hour + 2, d.getHours());
    d.setMinutes(-60);
    assertEquals(hour + 1, d.getHours());

    d = new Date(2009 - 1900, month, day, hour - 1, 0, 0);
    assertEquals(hour - 1, d.getHours());
    d.setMinutes(60);
    assertEquals(hour + 1, d.getHours());
    
    // Test changing hour by minutes = +- 120
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    assertEquals(hour + 2, d.getHours());
    d.setMinutes(-120);
    assertEquals(hour + 1, d.getHours());
    
    d = new Date(2009 - 1900, month, day, hour - 2, 0, 0);
    assertEquals(hour - 2, d.getHours());
    d.setMinutes(120);
    assertEquals(hour + 1, d.getHours());
    
    // Test changing hour by seconds = +- 3600
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    assertEquals(hour + 2, d.getHours());
    d.setSeconds(-3600);
    assertEquals(hour + 1, d.getHours());

    d = new Date(2009 - 1900, month, day, hour - 1, 0, 0);
    assertEquals(hour - 1, d.getHours());
    d.setSeconds(3600);
    assertEquals(hour + 1, d.getHours());
    
    // Test changing hour by seconds = +- 7200
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    assertEquals(hour + 2, d.getHours());
    d.setSeconds(-7200);
    assertEquals(hour + 1, d.getHours());

    d = new Date(2009 - 1900, month, day, hour - 2, 0, 0);
    assertEquals(hour - 2, d.getHours());
    d.setSeconds(7200);
    assertEquals(hour + 1, d.getHours());
    
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    d.setHours(hour);
    d.setMinutes(30);
    assertEquals(hour + 1, d.getHours());
    assertEquals(30, d.getMinutes());
    
    d = new Date(2009 - 1900, month, day, hour + 2, 0, 0);
    d.setMinutes(30);
    d.setHours(hour);
    assertEquals(hour + 1, d.getHours());
    assertEquals(30, d.getMinutes());
  }

  Date create() {
    return (Date) theDate.clone();
  }

  Date create(String s) {
    if (s.equals(FUTURE)) {
      return new Date("12/30/2030 3:4:5 GMT");
    } else if (s.equals(PAST)) {
      return new Date("1/5/1880 GMT");
    } else {
      return (Date) theDate.clone();
    }
  }

  private String createString(String s) {
    if (s.equals(FUTURE)) {
      return "12/30/2030 3:4:5 GMT";
    } else if (s.equals(PAST)) {
      return "1/1/1880 GMT";
    } else {
      return theDate.toLocaleString();
    }
  }

  private long roundToDay(long accum0) {
    return accum0 >> DAY_MILLISECONDS_SHIFT << DAY_MILLISECONDS_SHIFT;
  }
}
