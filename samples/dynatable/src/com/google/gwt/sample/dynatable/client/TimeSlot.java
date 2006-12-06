/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.sample.dynatable.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TimeSlot implements IsSerializable, Comparable {

  private final static transient String[] DAYS = new String[]{
    "Sun", "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat"};

  public TimeSlot() {
  }

  public TimeSlot(int zeroBasedDayOfWeek, int startMinutes, int endMinutes) {
    this.zeroBasedDayOfWeek = zeroBasedDayOfWeek;
    this.startMinutes = startMinutes;
    this.endMinutes = endMinutes;
  }

  public int compareTo(Object o) {
    TimeSlot other = (TimeSlot) o;
    if (zeroBasedDayOfWeek < other.zeroBasedDayOfWeek) {
      return -1;
    } else if (zeroBasedDayOfWeek > other.zeroBasedDayOfWeek) {
      return 1;
    } else {
      if (startMinutes < other.startMinutes) {
        return -1;
      } else if (startMinutes > other.startMinutes) {
        return 1;
      }
    }

    return 0;
  }

  public int getDayOfWeek() {
    return zeroBasedDayOfWeek;
  }

  public String getDescription() {
    return DAYS[zeroBasedDayOfWeek] + " " + getHrsMins(startMinutes) + "-"
      + getHrsMins(endMinutes);
  }

  public int getEndMinutes() {
    return endMinutes;
  }

  public int getStartMinutes() {
    return startMinutes;
  }

  public void setDayOfWeek(int zeroBasedDayOfWeek) {
    if (0 <= zeroBasedDayOfWeek && zeroBasedDayOfWeek < 7) {
      this.zeroBasedDayOfWeek = zeroBasedDayOfWeek;
    } else {
      throw new IllegalArgumentException("day must be in the range 0-6");
    }
  }

  public void setEndMinutes(int endMinutes) {
    this.endMinutes = endMinutes;
  }

  public void setStartMinutes(int startMinutes) {
    this.startMinutes = startMinutes;
  }

  private String getHrsMins(int mins) {
    int hrs = mins / 60;
    if (hrs > 12) {
      hrs -= 12;
    }

    int remainder = mins % 60;

    return hrs + ":"
      + (remainder < 10 ? "0" + remainder : String.valueOf(remainder));
  }

  private int endMinutes;
  private int startMinutes;
  private int zeroBasedDayOfWeek;
}
