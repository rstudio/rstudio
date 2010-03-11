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
package com.google.gwt.core.client;

/**
 * A simple wrapper around a native JS Date object.
 */
public class JsDate extends JavaScriptObject {

  /**
   * Creates a new date with the current time.
   */
  public static native JsDate create() /*-{
    return new Date();
  }-*/;

  /**
   * Creates a new date with the specified internal representation, which is the
   * number of milliseconds since midnight on January 1st, 1970. This is the
   * same representation returned by {@link #getTime()}.
   */
  public static native JsDate create(double milliseconds) /*-{
    return new Date(milliseconds);
  }-*/;

  /**
   * Creates a new date using the specified values.
   */
  public static native JsDate create(int year, int month) /*-{
    return new Date(year, month);
  }-*/;

  /**
   * Creates a new date using the specified values.
   */
  public static native JsDate create(int year, int month, int dayOfMonth) /*-{
    return new Date(year, month, dayOfMonth);
  }-*/;

  /**
   * Creates a new date using the specified values.
   */
  public static native JsDate create(int year, int month, int dayOfMonth, int hours) /*-{
    return new Date(year, month, dayOfMonth, hours);
  }-*/;

  /**
   * Creates a new date using the specified values.
   */
  public static native JsDate create(int year, int month, int dayOfMonth, int hours,
      int minutes) /*-{
    return new Date(year, month, dayOfMonth, hours, minutes);
  }-*/;

  /**
   * Creates a new date using the specified values.
   */
  public static native JsDate create(int year, int month, int dayOfMonth, int hours,
      int minutes, int seconds) /*-{
    return new Date(year, month, dayOfMonth, hours, minutes, seconds);
  }-*/;

  /**
   * Creates a new date using the specified values.
   */
  public static native JsDate create(int year, int month, int dayOfMonth, int hours,
      int minutes, int seconds, int millis) /*-{
    return new Date(year, month, dayOfMonth, hours, minutes, seconds, millis);
  }-*/;

  /**
   * Creates a new date from a string to be parsed.
   */
  public static native JsDate create(String dateString) /*-{
    return new Date(dateString);
  }-*/;

  /**
   * Parses a string representation of a date and time and returns the internal
   * millisecond representation. If the string cannot be parsed, the returned
   * value will be <code>NaN</code>. Use {@link Double#isNaN(double)} to check
   * the result.
   */
  public static native double parse(String dateString) /*-{
    return Date.parse(dateString);
  }-*/;

  // CHECKSTYLE_OFF: Matching the spec.
  /**
   * Returns the internal millisecond representation of the specified UTC date
   * and time.
   */
  public static native double UTC(int year, int month, int dayOfMonth, int hours,
      int minutes, int seconds, int millis) /*-{
    return Date.UTC(year, month, dayOfMonth, hours, minutes, seconds, millis);
  }-*/;

  // CHECKSTYLE_ON

  /**
   * Non directly instantiable, use one of the {@link #create()} methods.
   */
  protected JsDate() {
  }

  /**
   * Returns the day of the month.
   */
  public final native int getDate() /*-{
    return this.getDate();
  }-*/;

  /**
   * Returns the day of the week, from <code>0</code> (Sunday) to <code>6</code>
   * Saturday.
   */
  public final native int getDay() /*-{
    return this.getDay();
  }-*/;

  /**
   * Returns the four-digit year.
   */
  public final native int getFullYear() /*-{
    return this.getFullYear();
  }-*/;

  /**
   * Returns the hour, between <code>0</code> (midnight) and <code>23</code>.
   */
  public final native int getHours() /*-{
    return this.getHours();
  }-*/;

  /**
   * Returns the milliseconds, between <code>0</code> and <code>999</code>.
   */
  public final native int getMilliseconds() /*-{
    return this.getMilliseconds();
  }-*/;

  /**
   * Returns the minutes, between <code>0</code> and <code>59</code>.
   */
  public final native int getMinutes() /*-{
    return this.getMinutes();
  }-*/;

  /**
   * Returns the month, from <code>0</code> (January) to <code>6</code>
   * December.
   */
  public final native int getMonth() /*-{
    return this.getMonth();
  }-*/;

  /**
   * Returns the seconds, between <code>0</code> and <code>59</code>.
   */
  public final native int getSeconds() /*-{
    return this.getSeconds();
  }-*/;

  /**
   * Returns the internal millisecond representation of the date, the number of
   * milliseconds since midnight on January 1st, 1970. This is the same
   * representation returned by {@link #getTime()}.
   */
  public final native double getTime() /*-{
    return this.getTime();
  }-*/;

  /**
   * Returns the difference, in minutes, between the local and UTC
   * representations of this date. The value returned is affected by whether or
   * not daylight savings time would be in effect on specified date.
   */
  public final native int getTimezoneOffset() /*-{
    return this.getTimezoneOffset();
  }-*/;

  /**
   * Returns the day of the month, in UTC.
   */
  public final native int getUTCDate() /*-{
    return this.getUTCDate();
  }-*/;

  /**
   * Returns the day of the week, from <code>0</code> (Sunday) to <code>6</code>
   * Saturday, in UTC.
   */
  public final native int getUTCDay() /*-{
    return this.getUTCDay();
  }-*/;

  /**
   * Returns the four-digit year, in UTC.
   */
  public final native int getUTCFullYear() /*-{
    return this.getUTCFullYear();
  }-*/;

  /**
   * Returns the hour, between <code>0</code> (midnight) and <code>23</code>, in
   * UTC.
   */
  public final native int getUTCHours() /*-{
    return this.getUTCHours();
  }-*/;

  /**
   * Returns the milliseconds, between <code>0</code> and <code>999</code>, in
   * UTC.
   */
  public final native int getUTCMilliseconds() /*-{
    return this.getUTCMilliseconds();
  }-*/;

  /**
   * Returns the minutes, between <code>0</code> and <code>59</code>, in UTC.
   */
  public final native int getUTCMinutes() /*-{
    return this.getUTCMinutes();
  }-*/;

  /**
   * Returns the month, from <code>0</code> (January) to <code>6</code>
   * December, in UTC.
   */
  public final native int getUTCMonth() /*-{
    return this.getUTCMonth();
  }-*/;

  /**
   * Returns the seconds, between <code>0</code> and <code>59</code>, in UTC.
   */
  public final native int getUTCSeconds() /*-{
    return this.getUTCSeconds();
  }-*/;

  /**
   * Returns the year minus 1900.
   * 
   * @deprecated Use {@link #getFullYear()}.
   */
  @Deprecated
  public final native int getYear() /*-{
    return this.getYear();
  }-*/;

  /**
   * Sets the day of the month. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setDate(int dayOfMonth) /*-{
    return this.setDate(dayOfMonth);
  }-*/;

  /**
   * Sets the year. Returns the millisecond representation of the adjusted date.
   */
  public final native double setFullYear(int year) /*-{
    return this.setFullYear(year);
  }-*/;

  /**
   * Sets the year and month. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setFullYear(int year, int month) /*-{
    return this.setFullYear(year, month);
  }-*/;

  /**
   * Sets the year, month, and day. Returns the millisecond representation of
   * the adjusted date.
   */
  public final native double setFullYear(int year, int month, int day) /*-{
    return this.setFullYear(year, month, day);
  }-*/;

  /**
   * Sets the hour. Returns the millisecond representation of the adjusted date.
   */
  public final native double setHours(int hours) /*-{
    return this.setHours(hours);
  }-*/;

  /**
   * Sets the hour and minutes. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setHours(int hours, int mins) /*-{
    return this.setHours(hours, mins);
  }-*/;

  /**
   * Sets the hour, minutes, and seconds. Returns the millisecond representation
   * of the adjusted date.
   */
  public final native double setHours(int hours, int mins, int secs) /*-{
    return this.setHours(hours, mins, secs);
  }-*/;

  /**
   * Sets the hour, minutes, seconds, and milliseconds. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native double setHours(int hours, int mins, int secs, int ms) /*-{
    return this.setHours(hours, mins, secs, ms);
  }-*/;

  /**
   * Sets the minutes. Returns the millisecond representation of the adjusted
   * date.
   */
  public final native double setMinutes(int minutes) /*-{
    return this.setMinutes(minutes);
  }-*/;

  /**
   * Sets the minutes and seconds. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setMinutes(int minutes, int seconds) /*-{
    return this.setMinutes(minutes, seconds);
  }-*/;

  /**
   * Sets the minutes, seconds, and milliseconds. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native double setMinutes(int minutes, int seconds, int millis) /*-{
    return this.setMinutes(minutes, seconds, millis);
  }-*/;

  /**
   * Sets the month. Returns the millisecond representation of the adjusted
   * date.
   */
  public final native double setMonth(int month) /*-{
    return this.setMonth(month);
  }-*/;

  /**
   * Sets the month and day. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setMonth(int month, int dayOfMonth) /*-{
    return this.setMonth(month, dayOfMonth);
  }-*/;

  /**
   * Sets the seconds. Returns the millisecond representation of the adjusted
   * date.
   */
  public final native double setSeconds(int seconds) /*-{
    return this.setSeconds(seconds);
  }-*/;

  /**
   * Sets the seconds and milliseconds. Returns the millisecond representation
   * of the adjusted date.
   */
  public final native double setSeconds(int seconds, int millis) /*-{
    return this.setSeconds(seconds, millis);
  }-*/;

  /**
   * Sets the internal date representation. Returns the
   * <code>milliseconds</code> argument.
   */
  public final native double setTime(double milliseconds) /*-{
    return this.setTime(milliseconds);
  }-*/;

  /**
   * Sets the day of the month, in UTC. Returns the millisecond representation
   * of the adjusted date.
   */
  public final native double setUTCDate(int dayOfMonth) /*-{
    return this.setUTCDate(dayOfMonth);
  }-*/;

  /**
   * Sets the year, in UTC. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setUTCFullYear(int year) /*-{
    return this.setUTCFullYear(year);
  }-*/;

  /**
   * Sets the year and month, in UTC. Returns the millisecond representation of
   * the adjusted date.
   */
  public final native double setUTCFullYear(int year, int month) /*-{
    return this.setUTCFullYear(year, month);
  }-*/;

  /**
   * Sets the year, month, and day, in UTC. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native double setUTCFullYear(int year, int month, int day) /*-{
    return this.setUTCFullYear(year, month, day);
  }-*/;

  /**
   * Sets the hour, in UTC. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setUTCHours(int hours) /*-{
    return this.setUTCHours(hours);
  }-*/;

  /**
   * Sets the hour and minutes, in UTC. Returns the millisecond representation
   * of the adjusted date.
   */
  public final native double setUTCHours(int hours, int mins) /*-{
    return this.setUTCHours(hours, mins);
  }-*/;

  /**
   * Sets the hour, minutes, and seconds, in UTC. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native double setUTCHours(int hours, int mins, int secs) /*-{
    return this.setUTCHours(hours, mins, secs);
  }-*/;

  /**
   * Sets the hour, minutes, seconds, and milliseconds, in UTC. Returns the
   * millisecond representation of the adjusted date.
   */
  public final native double setUTCHours(int hours, int mins, int secs, int ms) /*-{
    return this.setUTCHours(hours, mins, secs, ms);
  }-*/;

  /**
   * Sets the minutes, in UTC. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setUTCMinutes(int minutes) /*-{
    return this.setUTCMinutes(minutes);
  }-*/;

  /**
   * Sets the minutes and seconds, in UTC. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native double setUTCMinutes(int minutes, int seconds) /*-{
    return this.setUTCMinutes(minutes, seconds);
  }-*/;

  /**
   * Sets the minutes, seconds, and milliseconds, in UTC. Returns the
   * millisecond representation of the adjusted date.
   */
  public final native double setUTCMinutes(int minutes, int seconds, int millis) /*-{
    return this.setUTCMinutes(minutes, seconds, millis);
  }-*/;

  /**
   * Sets the month, in UTC. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setUTCMonth(int month) /*-{
    return this.setUTCMonth(month);
  }-*/;

  /**
   * Sets the month and day, in UTC. Returns the millisecond representation of
   * the adjusted date.
   */
  public final native double setUTCMonth(int month, int dayOfMonth) /*-{
    return this.setUTCMonth(month, dayOfMonth);
  }-*/;

  /**
   * Sets the seconds, in UTC. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native double setUTCSeconds(int seconds) /*-{
    return this.setUTCSeconds(seconds);
  }-*/;

  /**
   * Sets the seconds and milliseconds, in UTC. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native double setUTCSeconds(int seconds, int millis) /*-{
    return this.setUTCSeconds(seconds, millis);
  }-*/;

  /**
   * Sets a two-digit year.
   * 
   * @deprecated Use {@link #setFullYear(int)}.
   */
  @Deprecated
  public final native double setYear(int year) /*-{
    return this.setYear(year);
  }-*/;

  /**
   * Returns a date string in the local time zone.
   */
  public final native String toDateString() /*-{
    return this.toDateString();
  }-*/;

  /**
   * Returns a date and time string in GMT.
   * 
   * @deprecated Use {@link #toUTCString()}.
   */
  @Deprecated
  public final native String toGMTString() /*-{
    return this.toGMTString();
  }-*/;

  /**
   * Returns a date string in the local time zone according to local formatting
   * conventions.
   */
  public final native String toLocaleDateString() /*-{
    return this.toLocaleDateString();
  }-*/;

  /**
   * Returns a date and time string in the local time zone according to local
   * formatting conventions.
   */
  public final native String toLocaleString() /*-{
    return this.toLocaleString();
  }-*/;

  /**
   * Returns a time string in the local time zone according to local formatting
   * conventions.
   */
  public final native String toLocaleTimeString() /*-{
    return this.toLocaleTimeString();
  }-*/;

  /**
   * Returns a time string in the local time zone.
   */
  public final native String toTimeString() /*-{
    return this.toTimeString();
  }-*/;

  /**
   * Returns a date and time string in UTC.
   */
  public final native String toUTCString() /*-{
    return this.toUTCString();
  }-*/;

  /**
   * Returns the millisecond representation, as {@link #getTime()}.
   */
  public final native double valueOf() /*-{
    return this.valueOf();
  }-*/;

}