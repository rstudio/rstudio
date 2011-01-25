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
package com.google.gwt.sample.dynatablerf.domain;

/**
 * Hold relevant data for a time slot.
 */
public class TimeSlot implements Comparable<TimeSlot> {

  private static final transient String[] DAYS = new String[] {
      "Sun", "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat"};

  private int endMinutes;

  private int startMinutes;

  private int zeroBasedDayOfWeek;

  public TimeSlot() {
  }

  public TimeSlot(int zeroBasedDayOfWeek, int startMinutes, int endMinutes) {
    this.zeroBasedDayOfWeek = zeroBasedDayOfWeek;
    this.startMinutes = startMinutes;
    this.endMinutes = endMinutes;
  }

  public int compareTo(TimeSlot o) {
    if (zeroBasedDayOfWeek < o.zeroBasedDayOfWeek) {
      return -1;
    } else if (zeroBasedDayOfWeek > o.zeroBasedDayOfWeek) {
      return 1;
    } else {
      if (startMinutes < o.startMinutes) {
        return -1;
      } else if (startMinutes > o.startMinutes) {
        return 1;
      } else if (endMinutes < o.endMinutes) {
        return -1;
      } else if (endMinutes > o.endMinutes) {
        return 1;
      } 
    }

    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TimeSlot)) {
      return false;
    }
    return compareTo((TimeSlot) obj) == 0;
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

  @Override
  public int hashCode() {
    return endMinutes + 7 * startMinutes + 31 * zeroBasedDayOfWeek;
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
}
