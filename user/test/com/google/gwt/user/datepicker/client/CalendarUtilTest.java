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

import junit.framework.TestCase;

import java.util.Date;

/**
 * Tests {@link CalendarUtil}.
 */
@SuppressWarnings("deprecation")
public class CalendarUtilTest extends TestCase {
  public void testAddDaysToDate() {
    Date start = new Date(99, 5, 15, 3, 30, 5);

    // Add 0 days
    CalendarUtil.addDaysToDate(start, 0);
    assertEquals(new Date(99, 5, 15, 3, 30, 5), start);

    // Add 5 days
    CalendarUtil.addDaysToDate(start, 5);
    assertEquals(new Date(99, 5, 20, 3, 30, 5), start);

    // Subtract 10 days
    CalendarUtil.addDaysToDate(start, -10);
    assertEquals(new Date(99, 5, 10, 3, 30, 5), start);
  }

  public void testAddMonthsToDate() {
    Date start = new Date(99, 5, 15, 3, 30, 5);

    // Add 0 months
    CalendarUtil.addMonthsToDate(start, 0);
    assertEquals(new Date(99, 5, 15, 3, 30, 5), start);

    // Add 5 months
    CalendarUtil.addMonthsToDate(start, 5);
    assertEquals(new Date(99, 10, 15, 3, 30, 5), start);

    // Subtract 6 months
    CalendarUtil.addMonthsToDate(start, -6);
    assertEquals(new Date(99, 4, 15, 3, 30, 5), start);
  }

  public void testCopyDate() {
    Date original = new Date(99, 5, 15, 3, 30, 5);

    // Copy the date
    Date copy = CalendarUtil.copyDate(original);
    assertEquals(original, copy);
    assertEquals(99, original.getYear());

    // Mutate the copy
    copy.setYear(70);
    assertEquals(99, original.getYear());
    assertEquals(70, copy.getYear());
  }

  public void testGetDaysBetween() {
    // Same date, same time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 15, 3, 30, 5);
      assertEquals(0, CalendarUtil.getDaysBetween(d0, d1));
    }

    // Same date, different time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 15, 4, 20, 5);
      assertEquals(0, CalendarUtil.getDaysBetween(d0, d1));
    }

    // Three days ahead, same time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 18, 3, 30, 5);
      assertEquals(3, CalendarUtil.getDaysBetween(d0, d1));
    }

    // Three days ahead, different time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 18, 4, 20, 5);
      assertEquals(3, CalendarUtil.getDaysBetween(d0, d1));
    }

    // Three days behind, sametime
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 12, 3, 30, 5);
      assertEquals(-3, CalendarUtil.getDaysBetween(d0, d1));
    }
  }

  public void testIsSameDate() {
    // Same date, same time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 15, 3, 30, 5);
      assertTrue(CalendarUtil.isSameDate(d0, d1));
    }

    // Same date, different time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 15, 4, 20, 5);
      assertTrue(CalendarUtil.isSameDate(d0, d1));
    }

    // Different date, same time
    {
      Date d0 = new Date(99, 5, 15, 3, 30, 5);
      Date d1 = new Date(99, 5, 18, 3, 30, 5);
      assertFalse(CalendarUtil.isSameDate(d0, d1));
    }
  }

  public void testSetToFirstDayOfMonth() {
    // Start in middle of month
    {
      Date date = new Date(99, 5, 15);
      CalendarUtil.setToFirstDayOfMonth(date);
      assertTrue(CalendarUtil.isSameDate(new Date(99, 5, 1), date));
    }

    // Start on first day of month
    {
      Date date = new Date(99, 5, 1);
      CalendarUtil.setToFirstDayOfMonth(date);
      assertTrue(CalendarUtil.isSameDate(new Date(99, 5, 1), date));
    }
  }
}
