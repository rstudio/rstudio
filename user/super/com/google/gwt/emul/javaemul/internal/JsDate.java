/*
 * Copyright 2015 Google Inc.
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
package javaemul.internal;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A simple wrapper around a native JS Date object.
 */
// TODO(dankurka): Turn this into a @JsType and move to dev/.../javaemul/internal
// Currently it can not be moved because of its dep on JavaScriptObject
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
  public static native JsDate create(int year, int month, int dayOfMonth, int hours,
      int minutes, int seconds, int millis) /*-{
    return new Date(year, month, dayOfMonth, hours, minutes, seconds, millis);
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
   * Non directly instantiable.
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
   * Returns the month, from <code>0</code> (January) to <code>11</code>
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
   * Returns the minutes, between <code>0</code> and <code>59</code>, in UTC.
   */
  public final native int getUTCMinutes() /*-{
    return this.getUTCMinutes();
  }-*/;

  /**
   * Returns the month, from <code>0</code> (January) to <code>11</code>
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
   * Sets the day of the month. Returns the millisecond representation of the
   * adjusted date.
   */
  public final native void setDate(int dayOfMonth) /*-{
    this.setDate(dayOfMonth);
  }-*/;

  /**
   * Sets the year. Returns the millisecond representation of the adjusted date.
   */
  public final native void setFullYear(int year) /*-{
    this.setFullYear(year);
  }-*/;

  /**
   * Sets the year, month, and day. Returns the millisecond representation of
   * the adjusted date.
   */
  public final native void setFullYear(int year, int month, int day) /*-{
    this.setFullYear(year, month, day);
  }-*/;

  /**
   * Sets the hour. Returns the millisecond representation of the adjusted date.
   */
  public final native void setHours(int hours) /*-{
    this.setHours(hours);
  }-*/;

  /**
   * Sets the hour, minutes, seconds, and milliseconds. Returns the millisecond
   * representation of the adjusted date.
   */
  public final native void setHours(int hours, int mins, int secs, int ms) /*-{
    this.setHours(hours, mins, secs, ms);
  }-*/;

  /**
   * Sets the minutes. Returns the millisecond representation of the adjusted
   * date.
   */
  public final native void setMinutes(int minutes) /*-{
    this.setMinutes(minutes);
  }-*/;

  /**
   * Sets the month. Returns the millisecond representation of the adjusted
   * date.
   */
  public final native void setMonth(int month) /*-{
    this.setMonth(month);
  }-*/;

  /**
   * Sets the seconds. Returns the millisecond representation of the adjusted
   * date.
   */
  public final native void setSeconds(int seconds) /*-{
    this.setSeconds(seconds);
  }-*/;

  /**
   * Sets the internal date representation. Returns the
   * <code>milliseconds</code> argument.
   */
  public final native void setTime(double milliseconds) /*-{
    this.setTime(milliseconds);
  }-*/;

  /**
   * Returns a date and time string in the local time zone according to local
   * formatting conventions.
   */
  public final native String toLocaleString() /*-{
    return this.toLocaleString();
  }-*/;
}

