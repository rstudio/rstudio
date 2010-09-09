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
package com.google.gwt.emultest.java.sql;

import com.google.gwt.junit.client.GWTTestCase;

import java.sql.Date;

/**
 * Tests {@link java.sql.Date}. We assume that the underlying
 * {@link java.util.Date} implementation is correct and concentrate only on the
 * differences between the two.
 */
@SuppressWarnings("deprecation")
public class SqlDateTest extends GWTTestCase {

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testInternalPrecision() {
    long millis = 1283895273475L;
    Date now = new Date(millis);
    Date after = new Date(now.getTime() + 1);
    Date before = new Date(now.getTime() - 1);

    // Note that Dates internally retain millisecond precision
    assertTrue(after.after(now));
    assertTrue(before.before(now));
  }

  public void testRoundedToDay() {
    java.util.Date utilDate = new java.util.Date();
    Date sqlDate = new Date(utilDate.getTime());

    java.util.Date utilDate2 = new java.util.Date(sqlDate.getTime());

    assertEquals(utilDate.getYear(), utilDate2.getYear());
    assertEquals(utilDate.getMonth(), utilDate2.getMonth());
    assertEquals(utilDate.getDate(), utilDate2.getDate());
  }

  public void testToString() {
    Date sqlDate = new Date(2000 - 1900, 1 - 1, 1);
    assertEquals("2000-01-01", sqlDate.toString());
  }
  
  public void testUnimplementedFunctions() {
    Date d = new Date(0);

    try {
      d.getHours();
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected behavior
    }

    try {
      d.getMinutes();
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected behavior
    }

    try {
      d.getSeconds();
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected behavior
    }

    try {
      d.setHours(0);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected behavior
    }

    try {
      d.setMinutes(0);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected behavior
    }

    try {
      d.setSeconds(0);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected behavior
    }
  }

  public void testValueOf() {
    Date d = Date.valueOf("2008-03-26");
    // Months are 0-based, days are 1-based
    assertEquals(108, d.getYear());
    assertEquals(2, d.getMonth());
    assertEquals(26, d.getDate());

    Date d2 = Date.valueOf(d.toString());
    assertEquals(d, d2);
    
    // validate that leading zero's don't trigger octal eval
    d = Date.valueOf("2009-08-08");
    assertEquals(109, d.getYear());
    assertEquals(7, d.getMonth());
    assertEquals(8, d.getDate());
    
    // validate 0x isn't a valid prefix
    try {
      d = Date.valueOf("2009-0xA-0xB");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }
}
