// CHECKSTYLE_OFF: Copyrighted to members of JCP JSR-166 Expert Group.
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */
// CHECKSTYLE_ON
package com.google.gwt.emultest.java.util.concurrent;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.concurrent.TimeUnit;

/**
 * Tests for TimeUnit.
 */
public class TimeUnitTest extends EmulTestBase {

  private static final long SECS_IN_MIN = 60L;
  private static final long SECS_IN_HOUR = SECS_IN_MIN * 60L;
  private static final long SECS_IN_DAY = SECS_IN_HOUR * 24L;

  private static final long MILLIS_IN_SEC = 1000L;
  private static final long MILLIS_IN_MIN = SECS_IN_MIN * MILLIS_IN_SEC;
  private static final long MILLIS_IN_HOUR = SECS_IN_HOUR * MILLIS_IN_SEC;
  private static final long MILLIS_IN_DAY = SECS_IN_DAY * MILLIS_IN_SEC;

  private static final long MICROS_IN_SEC = 1000000L;
  private static final long MICROS_IN_MILLI = 1000L;
  private static final long MICROS_IN_MIN = SECS_IN_MIN * MICROS_IN_SEC;
  private static final long MICROS_IN_HOUR = SECS_IN_HOUR * MICROS_IN_SEC;
  private static final long MICROS_IN_DAY = SECS_IN_DAY * MICROS_IN_SEC;

  private static final long NANOS_IN_SEC = 1000000000L;
  private static final long NANOS_IN_MILLI = 1000000L;
  private static final long NANOS_IN_MICRO = 1000L;
  private static final long NANOS_IN_MIN = SECS_IN_MIN * NANOS_IN_SEC;
  private static final long NANOS_IN_HOUR = SECS_IN_HOUR * NANOS_IN_SEC;
  private static final long NANOS_IN_DAY = SECS_IN_DAY * NANOS_IN_SEC;

  // (loops to 88888 check increments at all time divisions.)

  /** convert correctly converts sample values across the units */
  public void testConvert() {
    for (long t = 0; t < 88888; t += 500) {
      assertEquals(t * SECS_IN_DAY, TimeUnit.SECONDS.convert(t, TimeUnit.DAYS));
      assertEquals(t * SECS_IN_HOUR, TimeUnit.SECONDS.convert(t, TimeUnit.HOURS));
      assertEquals(t * SECS_IN_MIN, TimeUnit.SECONDS.convert(t, TimeUnit.MINUTES));
      assertEquals(t, TimeUnit.SECONDS.convert(t, TimeUnit.SECONDS));
      assertEquals(t, TimeUnit.SECONDS.convert(t * MILLIS_IN_SEC, TimeUnit.MILLISECONDS));
      assertEquals(t, TimeUnit.SECONDS.convert(t * MICROS_IN_SEC, TimeUnit.MICROSECONDS));
      assertEquals(t, TimeUnit.SECONDS.convert(t * NANOS_IN_SEC, TimeUnit.NANOSECONDS));

      assertEquals(t * MILLIS_IN_DAY, TimeUnit.MILLISECONDS.convert(t, TimeUnit.DAYS));
      assertEquals(t * MILLIS_IN_HOUR, TimeUnit.MILLISECONDS.convert(t, TimeUnit.HOURS));
      assertEquals(t * MILLIS_IN_MIN, TimeUnit.MILLISECONDS.convert(t, TimeUnit.MINUTES));
      assertEquals(t * MILLIS_IN_SEC, TimeUnit.MILLISECONDS.convert(t, TimeUnit.SECONDS));
      assertEquals(t, TimeUnit.MILLISECONDS.convert(t, TimeUnit.MILLISECONDS));
      assertEquals(t, TimeUnit.MILLISECONDS.convert(t * MICROS_IN_MILLI, TimeUnit.MICROSECONDS));
      assertEquals(t, TimeUnit.MILLISECONDS.convert(t * NANOS_IN_MILLI, TimeUnit.NANOSECONDS));

      assertEquals(t * MICROS_IN_DAY, TimeUnit.MICROSECONDS.convert(t, TimeUnit.DAYS));
      assertEquals(t * MICROS_IN_HOUR, TimeUnit.MICROSECONDS.convert(t, TimeUnit.HOURS));
      assertEquals(t * MICROS_IN_MIN, TimeUnit.MICROSECONDS.convert(t, TimeUnit.MINUTES));
      assertEquals(t * MICROS_IN_SEC, TimeUnit.MICROSECONDS.convert(t, TimeUnit.SECONDS));
      assertEquals(t * MICROS_IN_MILLI, TimeUnit.MICROSECONDS.convert(t, TimeUnit.MILLISECONDS));
      assertEquals(t, TimeUnit.MICROSECONDS.convert(t, TimeUnit.MICROSECONDS));
      assertEquals(t, TimeUnit.MICROSECONDS.convert(t * NANOS_IN_MICRO, TimeUnit.NANOSECONDS));

      assertEquals(t * NANOS_IN_DAY, TimeUnit.NANOSECONDS.convert(t, TimeUnit.DAYS));
      assertEquals(t * NANOS_IN_HOUR, TimeUnit.NANOSECONDS.convert(t, TimeUnit.HOURS));
      assertEquals(t * NANOS_IN_MIN, TimeUnit.NANOSECONDS.convert(t, TimeUnit.MINUTES));
      assertEquals(t * NANOS_IN_SEC, TimeUnit.NANOSECONDS.convert(t, TimeUnit.SECONDS));
      assertEquals(t * NANOS_IN_MILLI, TimeUnit.NANOSECONDS.convert(t, TimeUnit.MILLISECONDS));
      assertEquals(t * NANOS_IN_MICRO, TimeUnit.NANOSECONDS.convert(t, TimeUnit.MICROSECONDS));
      assertEquals(t, TimeUnit.NANOSECONDS.convert(t, TimeUnit.NANOSECONDS));
    }
  }

  /** toNanos correctly converts sample values in different units to nanoseconds */
  public void testToNanos() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t * NANOS_IN_DAY, TimeUnit.DAYS.toNanos(t));
      assertEquals(t * NANOS_IN_HOUR, TimeUnit.HOURS.toNanos(t));
      assertEquals(t * NANOS_IN_MIN, TimeUnit.MINUTES.toNanos(t));
      assertEquals(t * NANOS_IN_SEC, TimeUnit.SECONDS.toNanos(t));
      assertEquals(t * NANOS_IN_MILLI, TimeUnit.MILLISECONDS.toNanos(t));
      assertEquals(t * NANOS_IN_MICRO, TimeUnit.MICROSECONDS.toNanos(t));
      assertEquals(t, TimeUnit.NANOSECONDS.toNanos(t));
    }
  }

  /** toMicros correctly converts sample values in different units to microseconds */
  public void testToMicros() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t * MICROS_IN_DAY, TimeUnit.DAYS.toMicros(t));
      assertEquals(t * MICROS_IN_HOUR, TimeUnit.HOURS.toMicros(t));
      assertEquals(t * MICROS_IN_MIN, TimeUnit.MINUTES.toMicros(t));
      assertEquals(t * MICROS_IN_SEC, TimeUnit.SECONDS.toMicros(t));
      assertEquals(t * MICROS_IN_MILLI, TimeUnit.MILLISECONDS.toMicros(t));
      assertEquals(t, TimeUnit.MICROSECONDS.toMicros(t));
      assertEquals(t, TimeUnit.NANOSECONDS.toMicros(t * NANOS_IN_MICRO));
    }
  }

  /** toMillis correctly converts sample values in different units to milliseconds */
  public void testToMillis() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t * MILLIS_IN_DAY, TimeUnit.DAYS.toMillis(t));
      assertEquals(t * MILLIS_IN_HOUR, TimeUnit.HOURS.toMillis(t));
      assertEquals(t * MILLIS_IN_MIN, TimeUnit.MINUTES.toMillis(t));
      assertEquals(t * MILLIS_IN_SEC, TimeUnit.SECONDS.toMillis(t));
      assertEquals(t, TimeUnit.MILLISECONDS.toMillis(t));
      assertEquals(t, TimeUnit.MICROSECONDS.toMillis(t * MICROS_IN_MILLI));
      assertEquals(t, TimeUnit.NANOSECONDS.toMillis(t * NANOS_IN_MILLI));
    }
  }

  /** toSeconds correctly converts sample values in different units to seconds */
  public void testToSeconds() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t * SECS_IN_DAY, TimeUnit.DAYS.toSeconds(t));
      assertEquals(t * SECS_IN_HOUR, TimeUnit.HOURS.toSeconds(t));
      assertEquals(t * SECS_IN_MIN, TimeUnit.MINUTES.toSeconds(t));
      assertEquals(t, TimeUnit.SECONDS.toSeconds(t));
      assertEquals(t, TimeUnit.MILLISECONDS.toSeconds(t * MILLIS_IN_SEC));
      assertEquals(t, TimeUnit.MICROSECONDS.toSeconds(t * MICROS_IN_SEC));
      assertEquals(t, TimeUnit.NANOSECONDS.toSeconds(t * NANOS_IN_SEC));
    }
  }

  /** toMinutes correctly converts sample values in different units to minutes */
  public void testToMinutes() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t * SECS_IN_MIN * 24, TimeUnit.DAYS.toMinutes(t));
      assertEquals(t * SECS_IN_MIN, TimeUnit.HOURS.toMinutes(t));
      assertEquals(t, TimeUnit.MINUTES.toMinutes(t));
      assertEquals(t, TimeUnit.SECONDS.toMinutes(t * SECS_IN_MIN));
      assertEquals(t, TimeUnit.MILLISECONDS.toMinutes(t * MILLIS_IN_MIN));
      assertEquals(t, TimeUnit.MICROSECONDS.toMinutes(t * MICROS_IN_MIN));
      assertEquals(t, TimeUnit.NANOSECONDS.toMinutes(t * NANOS_IN_MIN));
    }
  }

  /** toHours correctly converts sample values in different units to hours */
  public void testToHours() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t * 24, TimeUnit.DAYS.toHours(t));
      assertEquals(t, TimeUnit.HOURS.toHours(t));
      assertEquals(t, TimeUnit.MINUTES.toHours(t * SECS_IN_MIN));
      assertEquals(t, TimeUnit.SECONDS.toHours(t * SECS_IN_HOUR));
      assertEquals(t, TimeUnit.MILLISECONDS.toHours(t * MILLIS_IN_HOUR));
      assertEquals(t, TimeUnit.MICROSECONDS.toHours(t * MICROS_IN_HOUR));
      assertEquals(t, TimeUnit.NANOSECONDS.toHours(t * NANOS_IN_HOUR));
    }
  }

  /** toDays correctly converts sample values in different units to days */
  public void testToDays() {
    for (long t = 0; t < 88888; t += 100) {
      assertEquals(t, TimeUnit.DAYS.toDays(t));
      assertEquals(t, TimeUnit.HOURS.toDays(t * 24));
      assertEquals(t, TimeUnit.MINUTES.toDays(t * SECS_IN_MIN * 24));
      assertEquals(t, TimeUnit.SECONDS.toDays(t * SECS_IN_DAY));
      assertEquals(t, TimeUnit.MILLISECONDS.toDays(t * MILLIS_IN_DAY));
      assertEquals(t, TimeUnit.MICROSECONDS.toDays(t * MICROS_IN_DAY));
      assertEquals(t, TimeUnit.NANOSECONDS.toDays(t * NANOS_IN_DAY));
    }
  }

  /**
   * convert saturates positive too-large values to Long.MAX_VALUE and negative to LONG.MIN_VALUE
   */
  public void testConvertSaturate() {
    assertEquals(
        Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Long.MAX_VALUE / 2, TimeUnit.SECONDS));
    assertEquals(
        Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(-Long.MAX_VALUE / 4, TimeUnit.SECONDS));
    assertEquals(
        Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Long.MAX_VALUE / 2, TimeUnit.MINUTES));
    assertEquals(
        Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(-Long.MAX_VALUE / 4, TimeUnit.MINUTES));
    assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Long.MAX_VALUE / 2, TimeUnit.HOURS));
    assertEquals(Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(-Long.MAX_VALUE / 4, TimeUnit.HOURS));
    assertEquals(Long.MAX_VALUE, TimeUnit.NANOSECONDS.convert(Long.MAX_VALUE / 2, TimeUnit.DAYS));
    assertEquals(Long.MIN_VALUE, TimeUnit.NANOSECONDS.convert(-Long.MAX_VALUE / 4, TimeUnit.DAYS));
  }

  /**
   * toNanos saturates positive too-large values to Long.MAX_VALUE and negative to LONG.MIN_VALUE
   */
  public void testToNanosSaturate() {
    assertEquals(Long.MAX_VALUE, TimeUnit.MILLISECONDS.toNanos(Long.MAX_VALUE / 2));
    assertEquals(Long.MIN_VALUE, TimeUnit.MILLISECONDS.toNanos(-Long.MAX_VALUE / 3));
  }

  /** toString returns string containing common name of unit */
  public void testToString() {
    String s = TimeUnit.SECONDS.toString();
    assertTrue(s.indexOf("ECOND") >= 0);
  }
}
