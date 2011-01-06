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

import java.sql.Timestamp;
import java.util.Date;

/**
 * Tests {@link java.sql.Timestamp}. We assume that the underlying
 * {@link java.util.Date} implementation is correct and concentrate only on the
 * differences between the two.
 */
@SuppressWarnings("deprecation")
public class SqlTimestampTest extends GWTTestCase {

  /**
   * Sets module name so that javascript compiler can operate.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCompareTo() {
    Timestamp now = Timestamp.valueOf("2011-01-05 12:45:18.000000000");
    // add 1.5 << 31 so coercing the ms difference to int results in a
    // sign change
    Timestamp later = new Timestamp(now.getTime() + (3L << 30));
    assertTrue("later <= now", later.compareTo(now) > 0);
  }

  /**
   * Timestamps have some non-obvious comparison semantics when compared to
   * dates.
   */
  public void testDateComparison() {
    /* Consider two cases, whether or not the time is a multiple of 1000 */
    testDateComparisonOneValue(1283895274000L);
    testDateComparisonOneValue(1283895273475L);
  }

  public void testNanosAffectTime() {
    long now = 1283895273475L;
    int millis = (int) (now % 1000);
    Timestamp t = new Timestamp(now);

    assertEquals(now, t.getTime());
    assertEquals(millis * 1000000, t.getNanos());

    t.setNanos(0);
    assertEquals(now - millis, t.getTime());

    t.setNanos(999999999);
    assertEquals(now - millis + 999, t.getTime());
  }

  public void testNanosComparison() {
    long now = 1283895273475L;
    Timestamp t = new Timestamp(now);
    t.setNanos(0);

    Timestamp t2 = new Timestamp(t.getTime());
    t2.setNanos(0);

    assertEquals(t, t2);
    assertEquals(0, t.compareTo(t2));
    assertFalse(t.before(t2));
    assertFalse(t.after(t2));

    t2.setNanos(1);

    assertFalse(t.equals(t2));
    assertTrue(t.compareTo(t2) < 0);
    assertTrue(t2.compareTo(t) > 0);
    assertTrue(t.before(t2));
    assertTrue(t2.after(t));
  }

  public void testNanosRange() {
    long now = 1283895273475L;
    Timestamp t = new Timestamp(now);

    assertEquals(now, t.getTime());
    assertEquals((now % 1000) * 1000000, t.getNanos());

    try {
      t.setNanos(-1);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Correct
    }

    t.setNanos(0);

    try {
      t.setNanos(1000000000);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Correct
    }

    t.setNanos(999999999);
  }

  public void testTimeAffectsNanos() {
    // A value 5 millis past the current second
    long now = 1283895273005L; // (1283895273475 / 1000) * 1000 + 5;

    Timestamp t = new Timestamp(now);
    assertEquals(5000000, t.getNanos());

    t.setTime(now + 1);
    assertEquals(6000000, t.getNanos());
  }

  public void testToString() {
    Timestamp ts = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123456789);
    assertEquals("2000-01-01 12:34:56.123456789", ts.toString());
  }

  public void testValueOf() {
    try {
      Timestamp.valueOf("");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Correct
    }

    try {
      Timestamp.valueOf("asdfg");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Correct
    }

    Timestamp expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123456789);
    Timestamp actual = Timestamp.valueOf("2000-01-01 12:34:56.123456789");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 0);
    actual = Timestamp.valueOf("2000-01-01 12:34:56");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 100000000);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.1");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 120000000);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.12");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123000000);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.123");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123400000);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.1234");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123450000);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.12345");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123456000);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.123456");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123456700);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.1234567");
    assertEquals(expected, actual);
    
    expected = new Timestamp(2000 - 1900, 1 - 1, 1, 12, 34, 56, 123456780);
    actual = Timestamp.valueOf("2000-01-01 12:34:56.12345678");
  }

  private void testDateComparisonOneValue(long value) {
    Date d = new Date(value);

    Timestamp t = new Timestamp(d.getTime());
    if (value % 1000 == 0) {
      t.setNanos(1000001);
    } else {
      t.setNanos(1);
    }

    // Timestamps are stored at second-level precision
    Date d2 = new Date(t.getTime());

    assertFalse("d.equals(t)", d.equals(t));
    assertEquals("d2, t", d2, t);
    assertEquals("hashcode", d2.hashCode(), t.hashCode());
    assertFalse("t.equals(d2)", t.equals(d2));

    // t is later then d2 by some number of nanoseconds
    assertEquals(1, t.compareTo(d2));

    Timestamp t2 = new Timestamp(d.getTime());
    t2.setNanos(t.getNanos() + 1);

    assertFalse("t.equals(t2)", t.equals(t2));
    assertEquals("hashcode2", t.hashCode(), t2.hashCode());
  }
}
