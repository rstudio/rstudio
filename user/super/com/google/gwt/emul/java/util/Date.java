/*
 * Copyright 2007 Google Inc.
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
package java.util;

import com.google.gwt.core.client.JavaScriptObject;

import java.io.Serializable;

/**
 * Represents a date and time.
 */
public class Date implements Cloneable, Comparable<Date>, Serializable {

  /**
   * Used only by toString().
   */
  private static final String[] DAYS = {
      "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
  };

  /**
   * Used only by toString().
   */
  private static final String[] MONTHS = {
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  };

  public static long parse(String s) {
    long d = (long) parse0(s);
    if (d != -1) {
      return d;
    } else {
      throw new IllegalArgumentException();
    }
  }

  // CHECKSTYLE_OFF: Matching the spec.
  public static long UTC(int year, int month, int date, int hrs,
      int min, int sec) {
    return (long) utc0(year, month, date, hrs, min, sec);
  }
  // CHECKSTYLE_ON

  /**
   * Ensure a number is displayed with two digits.
   * 
   * @return a two-character base 10 representation of the number
   */
  protected static String padTwo(int number) {
    if (number < 10) {
      return "0" + number;
    } else {
      return String.valueOf(number);
    }
  }

  /**
   *  Return the names for the days of the week as specified by the Date
   *  specification.
   */
  @SuppressWarnings("unused") // called by JSNI
  private static String dayToString(int day) {
    return DAYS[day];
  }

  /**
   *  Return the names for the months of the year as specified by the Date
   *  specification.
   */
  @SuppressWarnings("unused") // called by JSNI
  private static String monthToString(int month) {
    return MONTHS[month];
  }

  private static native double parse0(String s) /*-{
    var d = Date.parse(s);
    return isNaN(d) ? -1 : d;
  }-*/;

  /**
   * Throw an exception if jsdate is not an object.
   * 
   * @param val
   */
  @SuppressWarnings("unused") // called by JSNI
  private static void throwJsDateException(String val) {
    throw new IllegalStateException("jsdate is " + val);
  }

  private static native double utc0(int year, int month, int date, int hrs,
      int min, int sec) /*-{
    return Date.UTC(year + 1900, month, date, hrs, min, sec);
  }-*/;

  /**
   * JavaScript Date instance.
   */
  @SuppressWarnings("unused") // used from JSNI
  private JavaScriptObject jsdate;
  
  public Date() {
    init();
  }

  public Date(int year, int month, int date) {
    init(year, month, date, 0, 0, 0);
  }

  public Date(int year, int month, int date, int hrs, int min) {
    init(year, month, date, hrs, min, 0);
  }

  public Date(int year, int month, int date, int hrs, int min, int sec) {
    init(year, month, date, hrs, min, sec);
  }

  public Date(long date) {
    init(date);
  }

  public Date(String date) {
    init(Date.parse(date));
  }

  public boolean after(Date when) {
    return getTime() > when.getTime();
  }

  public boolean before(Date when) {
    return getTime() < when.getTime();
  }

  public Object clone() {
    return new Date(getTime());
  }

  public int compareTo(Date other) {
    long thisTime = getTime();
    long otherTime = other.getTime();
    if (thisTime < otherTime) {
      return -1;
    } else if (thisTime > otherTime) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return ((obj instanceof Date) && (getTime() == ((Date) obj).getTime()));
  }

  public native int getDate() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getDate();
  }-*/;

  public native int getDay() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getDay();
  }-*/;

  public native int getHours() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getHours();
  }-*/;

  public native int getMinutes() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getMinutes();
  }-*/;

  public native int getMonth() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getMonth();
  }-*/;

  public native int getSeconds() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getSeconds();
  }-*/;

  public long getTime() {
    return (long) getTime0();
  }

  public native int getTimezoneOffset() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getTimezoneOffset();
  }-*/;

  public native int getYear() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getFullYear()-1900;
  }-*/;

  @Override
  public int hashCode() {
    return (int) (this.getTime() ^ (this.getTime() >>> 32));
  }

  public native void setDate(int date) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setDate(date);
  }-*/;

  public native void setHours(int hours) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setHours(hours);
  }-*/;

  public native void setMinutes(int minutes) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setMinutes(minutes);
  }-*/;

  public native void setMonth(int month) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setMonth(month);
  }-*/;

  public native void setSeconds(int seconds) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setSeconds(seconds);
  }-*/;

  public void setTime(long time) {
    setTime0(time);
  }

  public native void setYear(int year) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setFullYear(year + 1900);
  }-*/;

  public native String toGMTString() /*-{
    this.@java.util.Date::checkJsDate()();
    var d = this.@java.util.Date::jsdate;
    var padTwo = @java.util.Date::padTwo(I);
    var month =
        @java.util.Date::monthToString(I)(this.@java.util.Date::jsdate.getUTCMonth());
  
    return d.getUTCDate() + " " +
        month + " " +
        d.getUTCFullYear() + " " +
        padTwo(d.getUTCHours()) + ":" +
        padTwo(d.getUTCMinutes()) + ":" +
        padTwo(d.getUTCSeconds()) +
        " GMT";
  }-*/;

  public native String toLocaleString() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.toLocaleString();
  }-*/;

  @Override
  public native String toString() /*-{
    this.@java.util.Date::checkJsDate()();
    var d = this.@java.util.Date::jsdate;
    var padTwo = @java.util.Date::padTwo(I);
    var day =
        @java.util.Date::dayToString(I)(d.getDay());
    var month =
        @java.util.Date::monthToString(I)(d.getMonth());

    // Compute timezone offset. The value that getTimezoneOffset returns is
    // backwards for the transformation that we want.
    var offset = -d.getTimezoneOffset();
    var hourOffset = String((offset >= 0) ?
        "+" + Math.floor(offset / 60) : Math.ceil(offset / 60));
    var minuteOffset = padTwo(Math.abs(offset) % 60);

    return day + " " + month + " " +
        padTwo(d.getDate()) + " " +
        padTwo(d.getHours()) + ":" +
        padTwo(d.getMinutes()) + ":" +
        padTwo(d.getSeconds()) +
        " GMT" + hourOffset + minuteOffset +
        + " " + d.getFullYear();
  }-*/;

  /**
   *  Check that jsdate is valid and throw an exception if not.
   */
  @SuppressWarnings("unused") // called by JSNI
  private native void checkJsDate() /*-{
    if (!this.@java.util.Date::jsdate
        || typeof this.@java.util.Date::jsdate != "object") {
      @java.util.Date::throwJsDateException(Ljava/lang/String;)(""
          + this.@java.util.Date::jsdate);
    }
  }-*/;

  private native double getTime0() /*-{
    this.@java.util.Date::checkJsDate()();
    return this.@java.util.Date::jsdate.getTime();
  }-*/;

  private native void init() /*-{
    this.@java.util.Date::jsdate = new Date();
  }-*/;

  private native void init(double date) /*-{
    this.@java.util.Date::jsdate = new Date(date);
  }-*/;

  private native void init(int year, int month, int date, int hrs, int min,
      int sec) /*-{
    this.@java.util.Date::jsdate = new Date();
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setFullYear(year + 1900, month, date);
    this.@java.util.Date::jsdate.setHours(hrs, min, sec, 0);
  }-*/;

  private native void setTime0(double time) /*-{
    this.@java.util.Date::checkJsDate()();
    this.@java.util.Date::jsdate.setTime(time);
  }-*/;
}
