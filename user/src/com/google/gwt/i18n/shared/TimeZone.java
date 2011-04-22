/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.shared;

import java.util.Date;

/**
 * Abstracts a GWT timezone.
 */
public interface TimeZone {

  /**
   * Returns the daylight savings time adjustment, in minutes, for the given
   * date. If daylight savings time is in effect on the given date, the number
   * will be positive, otherwise 0.
   *
   * @param date the date to check
   * @return offset amount
   */
  int getDaylightAdjustment(Date date);

  /**
   * Returns the GMT representation of this time zone object.
   *
   * @param date The date from which the time information should be extracted
   * @return A GMT representation of the time given by the date
   */
  String getGMTString(Date date);

  /**
   * Returns time zone id for this time zone. For time zone objects that have
   * been instantiated from a time zone offset, the POSIX time zone id will be
   * returned.
   *
   * @return time zone id
   */
  String getID();

  /**
   * To get ISO-style (+00:00) representation of the time zone for given date.
   *
   * @param date The date for which time to retrieve RFC time zone string
   * @return ISO-style time zone string
   */
  String getISOTimeZoneString(Date date);

  /**
   * Returns the long version of the time zone name for the given date; the
   * result of this method will be different if daylight savings time is in
   * effect.
   *
   * @param date The date for which the long time zone name is returned
   * @return long time zone name
   */
  String getLongName(Date date);

  /**
   * Returns the RFC representation of the time zone name for the given date.
   * To be consistent with JDK/Javascript API, west of Greenwich will be
   * positive.
   *
   *  @param date The date for which time to retrieve time zone offset
   *  @return time zone offset in minutes
   */
  int getOffset(Date date);

  /**
   * To get RFC representation of certain time zone name for given date.
   * @param date The date for which time to retrieve RFC time zone string
   * @return RFC time zone string
   */
  String getRFCTimeZoneString(Date date);

  /**
   * Returns the short time zone name for a given date.
   *
   * @param date The date for which time to retrieve short time zone
   * @return short time zone name
   */
  String getShortName(Date date);

  /**
   * Returns the standard time zone offset, in minutes.
   */
  int getStandardOffset();

  /**
   * Check whether the given date and time falls within a daylight savings time
   * period.
   *
   * @param date and time to check
   * @return true if daylight savings time is in effect
   */
  boolean isDaylightTime(Date date);
}