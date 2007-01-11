// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

public class DateTest extends GWTTestCase {
  public static final String CURRENT = "CURRENT";
  public static final long DAY_MILLISECONDS_SHIFT = 27;
  public static final String FUTURE = "FUTURE";
  public static final String PAST = "PAST";
  public static final long SECOND_MILLISECONDS_SHIFT = 10;
  /** Sets module name so that javascript compiler can operate */
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /** Testing for public boolean java.util.Date.after(java.util.Date)* */
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

  /** Testing for public boolean java.util.Date.before(java.util.Date)* */
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

  /** Testing for public java.lang.Object java.util.Date.clone()* */
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

  /** Testing for public int java.util.Date.compareTo(java.util.Date)* */
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

  /** Testing for public int java.util.Date.getDate()* */
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

  /** Testing for public int java.util.Date.getDay()* */
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

  /**
   * Testing for public int java.util.Date.getHours()
   */
  public void testGetHours() {
    // Cannot be done because each time zone will give a different
    // answer
  }

  /** Testing for public int java.util.Date.getMinutes()* */
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

  /** Testing for public int java.util.Date.getMonth()* */
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

  /** Testing for public int java.util.Date.getSeconds()* */
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

  /** Testing for public long java.util.Date.getTime()* */
  public void testGetTime() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    long a1 = accum1.getTime();
    assertEquals(-2839795200000l, a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    long a2 = accum2.getTime();
    assertEquals(1293678245000l, a2);

  }

  /** Testing for public int java.util.Date.getTimezoneOffset()* */
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

  /** Testing for public int java.util.Date.getYear()* */
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
    assertEquals(110, a2);
  }

  /** Testing for public static long java.util.Date.parse(java.lang.String)* */
  public void testParse() {

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
    assertEquals(-2840140800000l, a1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    String arg30 = createString(FUTURE);
    long a2 = Date.parse(arg30);
    assertEquals(1293678245000l, a2);

  }

  /** Testing for public void java.util.Date.setDate(int)* */
  public void testSetDate() {
    // We only go through dates from 0-28 here. There are some months that do not
    // have 29, 30, or 31 days - so our assertion would be wrong in the cases where
    // the current month did not have 29,30,or 31 days
    for (int i = 1; i < 29; i++) {
      Date accum0 = create();
      accum0.setDate(i);
      assertEquals(accum0.getDate(), i);
    }
  }

  /** Testing to that if we set the day number to 31 for a month that only has 30 days in it,
      that the date rolls over to the first day of the next month in sequence.
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

  /** Testing for public void java.util.Date.setHours(int)* */
  public void testSetHours() {
    for (int i = 0; i < 24; i++) {
      Date accum0 = create();
      accum0.setHours(i);
      assertEquals(accum0.getHours(), i);
    }

  }

  /** Testing for public void java.util.Date.setMinutes(int)* */
  public void testSetMinutes() {
    for (int i = 0; i < 24; i++) {
      Date accum0 = create();
      accum0.setMinutes(i);
      assertEquals(accum0.getMinutes(), i);
    }
  }

  /** Testing for public void java.util.Date.setMonth(int)* */
  public void testSetMonth() {
    for (int i = 0; i < 12; i++) {
      // We want to use a fixed date here. If we use the current date, the assertion may fail
      // when the date is the 29th, 30th, or 31st, and we set the month to one which does
      // not have 29, 30, or 31 days in it, respectively.
      Date accum0 = new Date(2006, 12, 1);
      accum0.setMonth(i);
      assertEquals(accum0.getMonth(), i);
    }

  }

  /** We want to test to see that if we are currently in a month with 31 days and we
      set the month to one which has less than 31 days, that the month returned by the
      date class will be one higher than the month that we originally set (according to
      the spec of java.util.date)
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

  /** Testing for public void java.util.Date.setSeconds(int)* */
  public void testSetSeconds() {
    for (int i = 0; i < 24; i++) {
      Date accum0 = create();
      accum0.setSeconds(i);
      assertEquals(accum0.getSeconds(), i);
    }
  }

  /** Testing for public void java.util.Date.setTime(long)* */
  public void testSetTime() {
    long[] values = new long[]{-100000000000l, -100l, 0, 100l, 1000000000l};
    for (int i = 0; i < values.length; i++) {
      Date accum0 = create();
      accum0.setTime(values[i]);
      assertEquals(accum0.getTime(), values[i]);
    }
  }

  /** Testing for public void java.util.Date.setYear(int)* */
  public void testSetYear() {
    for (int i = 1880; i < 2050; i++) {
      // We want to use a fixed date here. If we use the current date, the assertion may fail
      // when the date is February 29th, and we set the year to a non-leap year
      Date accum0 = new Date(2006, 12, 01);
      accum0.setYear(i);
      assertEquals(accum0.getYear(), i);
    }
  }

  /** We want to test to see that if the date is Feb 29th (in a leap year) and we set the
      year to a non-leap year, that the month and day will roll over to March 1st.
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

  /** We want to test to see that if the date is Feb 29th (in a leap year) and we set the
      year to another leap year, that the month and day will be retained
  */
  public void testSetValidLeapYearForDate() {
    int dayNum = 29;
    int monthNum = 1; //February
    int yearNum = 2004;
    int newYearNum = yearNum + 4;
    Date leapYearDate = new Date(yearNum, monthNum, dayNum);
    leapYearDate.setYear(newYearNum);
    assertEquals(leapYearDate.getYear(), newYearNum);
    assertEquals(leapYearDate.getMonth(), monthNum);
    assertEquals(leapYearDate.getDate(), dayNum);
 }

  /** Testing for public java.lang.String java.util.Date.toGMTString()* */
  public void testToGMTString() {

    // /////////////////////////////
    // Past
    // /////////////////////////////
    Date accum1 = create(PAST);
    String a1 = accum1.toGMTString();
    a1 = a1.replaceAll("UTC", "GMT");
    assertTrue(a1.indexOf("5 Jan 1880 00:00:00") != -1);

    // /////////////////////////////
    // Future
    // /////////////////////////////
    Date accum2 = create(FUTURE);
    String a2 = accum2.toGMTString();
    a2 = a2.replaceAll("UTC", "GMT");
    assertTrue(a2.indexOf("30 Dec 2010 03:04:05 GMT") != -1);
  }

  /** Testing for public java.lang.String java.util.Date.toLocaleString()* */
  public void testToLocaleString() {

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
    assertTrue(a2.indexOf("2010") != -1);
  }

  /** Testing for public static long java.util.Date.UTC(int,int,int,int,int,int)* */
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

  Date create() {
    return (Date) theDate.clone();
  }

  Date create(String s) {
    if (s.equals(FUTURE)) {
      return new Date("12/30/2010 3:4:5 GMT");
    } else if (s.equals(PAST)) {
      return new Date("1/5/1880 GMT");
    } else {
      return (Date) theDate.clone();
    }
  }

  private String createString(String s) {
    if (s.equals(FUTURE)) {
      return "12/30/2010 3:4:5 GMT";
    } else if (s.equals(PAST)) {
      return "1/1/1880 GMT";
    } else {
      return theDate.toLocaleString();
    }
  }

  private long roundToDay(long accum0) {
    return accum0 >> DAY_MILLISECONDS_SHIFT << DAY_MILLISECONDS_SHIFT;
  }

  Date theDate = new Date();
}
