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

package com.google.gwt.i18n.client;

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;

import java.util.Date;

/**
 * The TimeZone class implements a time zone information source for client
 * applications. The time zone object is instantiated from a TimeZoneData object,
 * which is made from a JSON string that contains all the data needed for
 * the specified time zone. Applications can instantiate a time zone statically,
 * in which case the data could be retrieved from
 * the {@link com.google.gwt.i18n.client.constants.TimeZoneConstants TimeZoneConstants}
 * class. Applications can also choose to instantiate from a string obtained
 * from a server. The time zone string contains locale specific data. If the 
 * application only uses a short representation, the English data will usually
 * satisfy the user's need. In the case that only the time zone offset is known, 
 * there is a decent fallback that only uses the time zone offset to create a
 * TimeZone object.
 */
public class TimeZone {  
  // constants to reference time zone names in the time zone names array
  private static final int STD_SHORT_NAME = 0;
  private static final int STD_LONG_NAME = 1;
  private static final int DLT_SHORT_NAME = 2;
  private static final int DLT_LONG_NAME = 3;

  /**
   * This factory method provides a decent fallback to create a time zone object
   * just based on a given time zone offset.
   * 
   * @param timeZoneOffsetInMinutes time zone offset in minutes
   * @return a new time zone object
   */
  public static TimeZone createTimeZone(int timeZoneOffsetInMinutes) {
    TimeZone tz = new TimeZone();
    tz.standardOffset = timeZoneOffsetInMinutes;
    tz.timezoneID = composePOSIXTimeZoneID(timeZoneOffsetInMinutes);
    tz.tzNames = new String[2]; 
    tz.tzNames[0] = composeUTCString(timeZoneOffsetInMinutes);
    tz.tzNames[1] = composeUTCString(timeZoneOffsetInMinutes);
    tz.transitionPoints = null;
    tz.adjustments = null;
    return tz;
  }

  /**
   * This factory method creates a time zone instance from a JSON string that
   * contains the time zone information for desired time zone. Applications can
   * get such a string from the TimeZoneConstants class, or it can request the
   * string from the server. Either way, the application obtains the original
   * string from the data provided in the TimeZoneConstant.properties file,
   * which was carefully prepared from CLDR and Olson time zone database.
   * 
   * @param tzJSON JSON string that contains time zone data
   * @return a new time zone object
   */
  public static TimeZone createTimeZone(String tzJSON) {
    TimeZoneInfo tzData = TimeZoneInfo.buildTimeZoneData(tzJSON);
    
    return createTimeZone(tzData);
  }

  public static TimeZone createTimeZone(TimeZoneInfo timezoneData) {
    TimeZone tz = new TimeZone();

    tz.timezoneID = timezoneData.getID();
    tz.standardOffset = -timezoneData.getStandardOffset();
    
    JsArrayString jsTimezoneNames = timezoneData.getNames();
    
    tz.tzNames = new String[jsTimezoneNames.length()];
    
    for (int i = 0; i < jsTimezoneNames.length(); i++) {
      tz.tzNames[i] = jsTimezoneNames.get(i);
    }

    JsArrayInteger transitions = timezoneData.getTransitions();    

    if (transitions == null || transitions.length() == 0) {
      tz.transitionPoints = null;
      tz.adjustments = null;
    } else {
      int transitionNum = transitions.length() / 2;
      tz.transitionPoints = new int[transitionNum];
      tz.adjustments = new int[transitionNum];

      for (int i = 0; i < transitionNum; ++i) {
        tz.transitionPoints[i] = transitions.get(i * 2);
        tz.adjustments[i] = transitions.get(i * 2 + 1);
      }
    }
    return tz;
  }

  /**
   * In GMT representation, +/- has reverse sign of time zone offset.
   * when offset == 480, it should output GMT-08:00.
   */
  private static String composeGMTString(int offset) {
    char data[] = {'G', 'M', 'T', '-', '0', '0', ':', '0', '0'};
    if (offset <= 0) {
      data[3] = '+';
      offset = -offset; // suppress the '-' sign for text display.
    }
    data[4] += (offset / 60) / 10;
    data[5] += (offset / 60) % 10;
    data[7] += (offset % 60) / 10;
    data[8] += offset % 10;
    return new String(data);
  }

  /** 
   * POSIX time zone ID as fallback. 
   */
  private static String composePOSIXTimeZoneID(int offset) {
    if (offset == 0) {
      return "Etc/GMT";
    }
    String str;
    if (offset < 0) {
      offset = -offset;
      str = "Etc/GMT-";
    } else {
      str = "Etc/GMT+";
    }
    return str + offsetDisplay(offset);
  }

  private static String composeUTCString(int offset) {
    if (offset == 0) {
      return "UTC";
    }
    String str;
    if (offset < 0) {
      offset = -offset;
      str = "UTC+";
    } else {
      str = "UTC-";
    }
    return str + offsetDisplay(offset);
  }
  
  private static String offsetDisplay(int offset) {
    int hour = offset / 60;
    int mins = offset % 60;
    if (mins == 0) {
      return Integer.toString(hour);
    }
    return Integer.toString(hour) + ":" + Integer.toString(mins);
  }

  private String timezoneID;
  private int standardOffset;
  private String[] tzNames;
  private int[] transitionPoints;
  private int[]  adjustments;

  private TimeZone() {
  }

  /**
   * Return the daylight savings time adjustment, in minutes, for the given
   * date. If daylight savings time is in effect on the given date, the number
   * will be positive, otherwise 0.
   * 
   * @param date the date to check
   * @return offset amount
   */
  public int getDaylightAdjustment(Date date) {
    if (transitionPoints == null) {
      return 0;
    }
    long timeInHours = date.getTime() / 1000 / 3600;
    int index = 0;
    while (index < transitionPoints.length && 
        timeInHours >= transitionPoints[index]) {
      ++index;
    }
    return (index == 0) ? 0 : adjustments[index - 1];
  }

  /**
   * Return the GMT representation of this time zone object.
   * 
   * @param date The date from which the time information should be extracted
   * @return A GMT representation of the time given by the date
   */
  public String getGMTString(Date date) {
    return composeGMTString(getOffset(date));
  }

  /**
   * Return time zone id for this time zone. For time zone objects that have
   * been instantiated from a time zone offset, the POSIX time zone id will be
   * returned.
   * 
   * @return time zone id
   */
  public String getID() {
    return timezoneID;
  }

  /**
   * To get ISO-style (+00:00) representation of the time zone for given date.
   *
   * @param date The date for which time to retrieve RFC time zone string
   * @return ISO-style time zone string
   */
  public String getISOTimeZoneString(Date date) {
    int offset = -getOffset(date);
    char data[] = {'+', '0', '0', ':', '0', '0'};
    if (offset < 0) {
      data[0] = '-';
      offset = -offset; // suppress the '-' sign for text display.
    }
    data[1] += (offset / 60) / 10;
    data[2] += (offset / 60) % 10;
    data[4] += (offset % 60) / 10;
    data[5] += offset % 10;
    return new String(data);
  }

  /**
   * Returns the long version of the time zone name for the given date; the
   * result of this method will be different if daylight savings time is in
   * effect.
   * 
   * @param date The date for which the long time zone name is returned
   * @return long time zone name
   */
  public String getLongName(Date date) {
    return tzNames[isDaylightTime(date) ? DLT_LONG_NAME : STD_LONG_NAME]; 
  }

  /**
   * Returns the RFC representation of the time zone name for the given date.
   * To be consistent with JDK/Javascript API, west of Greenwich will be
   * positive.
   * 
   *  @param date The date for which time to retrieve time zone offset
   *  @return time zone offset in minutes
   */
  public int getOffset(Date date) {
    return standardOffset - getDaylightAdjustment(date);
  }

  /**
   * To get RFC representation of certain time zone name for given date.
   * @param date The date for which time to retrieve RFC time zone string
   * @return RFC time zone string
   */
  public String getRFCTimeZoneString(Date date) {
    int offset = -getOffset(date);
    char data[] = {'+', '0', '0', '0', '0'};
    if (offset < 0) {
      data[0] = '-';
      offset = -offset; // suppress the '-' sign for text display.
    }
    data[1] += (offset / 60) / 10;
    data[2] += (offset / 60) % 10;
    data[3] += (offset % 60) / 10;
    data[4] += offset % 10;
    return new String(data);
  }

  /**
   * Returns the short time zone name for a given date.
   * 
   * @param date The date for which time to retrieve short time zone
   * @return short time zone name
   */
  public String getShortName(Date date) {
    return tzNames[isDaylightTime(date) ? DLT_SHORT_NAME : STD_SHORT_NAME]; 
  }

  /**
   * @return the standard time zone offset, in minutes.
   */
  public int getStandardOffset() {
    return standardOffset;
  }

  /**
   * Check whether the given date and time falls within a daylight savings time
   * period.
   * 
   * @param date and time to check
   * @return true if daylight savings time is in effect
   */
  public boolean isDaylightTime(Date date) {
    return getDaylightAdjustment(date) > 0;
  }
}
