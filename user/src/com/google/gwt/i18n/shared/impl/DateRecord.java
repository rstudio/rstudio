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
package com.google.gwt.i18n.shared.impl;

import java.util.Date;

/**
 * Implementation detail of DateTimeFormat -- not a public API and subject to
 * change.
 * 
 * DateRecord class exposes almost the same set of interface as Date class with
 * only a few exceptions. The main purpose is the record all the information
 * during parsing phase and resolve them in a later time when all information
 * can be processed together.
 */
@SuppressWarnings("deprecation")
public class DateRecord extends Date {

  /*
   * The serial version UID is only defined because this class is implicitly
   * serializable, causing warnings due to its absence. It will generally not be
   * used.
   */
  private static final long serialVersionUID = -1278816193740448162L;

  public static final int AM = 0;
  public static final int PM = 1;

  private static final int JS_START_YEAR = 1900;

  private int era;
  private int year;
  private int month;
  private int dayOfMonth;
  private int ampm;
  private boolean midnightIs24;
  private int hours;
  private int minutes;
  private int seconds;
  private int milliseconds;

  private int tzOffset;
  private int dayOfWeek;
  private boolean ambiguousYear;

  /**
   * Initialize DateExt object with default value. Here we use -1 for most of
   * the field to indicate that field is not set.
   */
  public DateRecord() {
    era = -1;
    ambiguousYear = false;
    year = Integer.MIN_VALUE;
    month = -1;
    dayOfMonth = -1;
    ampm = -1;
    midnightIs24 = false;
    hours = -1;
    minutes = -1;
    seconds = -1;
    milliseconds = -1;
    dayOfWeek = -1;
    tzOffset = Integer.MIN_VALUE;
  }

  /**
   * calcDate uses all the field available so far to fill a Date object. For
   * those information that is not provided, the existing value in 'date' will
   * be kept. Ambiguous year will be resolved after the date/time values are
   * resolved.
   * 
   * If the strict option is set to true, calcDate will calculate certain
   * invalid dates by wrapping around as needed. For example, February 30 will
   * wrap to March 2.
   * 
   * @param date The Date object being filled. Its value should be set to an
   *          acceptable default before pass in to this method
   * @param strict true to be strict when parsing
   * @return true if successful, otherwise false.
   */
  public boolean calcDate(Date date, boolean strict) {
    // Year 0 is 1 BC, and so on.
    if (this.era == 0 && this.year > 0) {
      this.year = -(this.year - 1);
    }

    if (this.year > Integer.MIN_VALUE) {
      date.setYear(this.year - JS_START_YEAR);
    }

    // "setMonth" and "setDate" is a little bit tricky. Suppose content in
    // date is 11/30, switch month to 02 will lead to 03/02 since 02/30 does
    // not exist. And you certain won't like 02/12 turn out to be 03/12. So
    // here to set date to a smaller number before month, and later setMonth.
    // Real date is set after, and that might cause month switch. However,
    // that's desired.
    int orgDayOfMonth = date.getDate();
    date.setDate(1);

    if (this.month >= 0) {
      date.setMonth(this.month);
    }

    if (this.dayOfMonth >= 0) {
      date.setDate(this.dayOfMonth);
    } else if (this.month >= 0) {
      // If the month was parsed but dayOfMonth was not, then the current day of
      // the month shouldn't affect the parsed month. For example, if "Feb2006"
      // is parse on January 31, the resulting date should be in February, not
      // March. So, we limit the day of the month to the maximum day within the
      // parsed month.
      Date tmp = new Date(date.getYear(), date.getMonth(), 35);
      int daysInCurrentMonth = 35 - tmp.getDate();
      date.setDate(Math.min(daysInCurrentMonth, orgDayOfMonth));
    } else {
      date.setDate(orgDayOfMonth);
    }

    // adjust ampm
    if (this.hours < 0) {
      this.hours = date.getHours();
    }

    if (this.ampm > 0) {
      if (this.hours < 12) {
        this.hours += 12;
      }
    }
    date.setHours(this.hours == 24 && this.midnightIs24 ? 0 : this.hours);

    if (this.minutes >= 0) {
      date.setMinutes(this.minutes);
    }

    if (this.seconds >= 0) {
      date.setSeconds(this.seconds);
    }

    if (this.milliseconds >= 0) {
      date.setTime(date.getTime() / 1000 * 1000 + this.milliseconds);
    }

    // If strict, verify that the original date fields match the calculated date
    // fields. We do this before we set the timezone offset, which will skew all
    // of the dates.
    //
    // We don't need to check the day of week as it is guaranteed to be correct
    // or return false below.
    if (strict) {
      if ((this.year > Integer.MIN_VALUE)
          && ((this.year - JS_START_YEAR) != date.getYear())) {
        return false;
      }
      if ((this.month >= 0) && (this.month != date.getMonth())) {
        return false;
      }
      if ((this.dayOfMonth >= 0) && (this.dayOfMonth != date.getDate())) {
        return false;
      }
      // Times have well defined maximums
      if (this.hours == 24 && this.midnightIs24) {
        if (this.ampm > 0) {
          return false;
        }
      } else if (this.hours >= 24) {
        return false;
      } else if (this.hours == 0 && this.midnightIs24) {
        return false;
      }
      if (this.minutes >= 60) {
        return false;
      }
      if (this.seconds >= 60) {
        return false;
      }
      if (this.milliseconds >= 1000) {
        return false;
      }
    }

    // Resolve ambiguous year if needed.
    if (this.ambiguousYear) { // the two-digit year == the default start year
      Date defaultCenturyStart = new Date();
      defaultCenturyStart.setYear(defaultCenturyStart.getYear() - 80);
      if (date.before(defaultCenturyStart)) {
        date.setYear(defaultCenturyStart.getYear() + 100);
      }
    }

    // Date is resolved to the nearest dayOfWeek if date is not explicitly
    // specified. There is one exception, if the nearest dayOfWeek falls
    // into a different month, the 2nd nearest dayOfWeek, which is on the
    // other direction, will be used.
    if (this.dayOfWeek >= 0) {
      if (this.dayOfMonth == -1) {
        // Adjust to the nearest day of the week.
        int adjustment = (7 + this.dayOfWeek - date.getDay()) % 7;
        if (adjustment > 3) {
          adjustment -= 7;
        }
        int orgMonth = date.getMonth();
        date.setDate(date.getDate() + adjustment);

        // If the nearest weekday fall into a different month, we will use the
        // 2nd nearest weekday, which will be on the other direction, and is
        // sure fall into the same month.
        if (date.getMonth() != orgMonth) {
          date.setDate(date.getDate() + (adjustment > 0 ? -7 : 7));
        }
      } else {
        if (date.getDay() != this.dayOfWeek) {
          return false;
        }
      }
    }

    // Adjust time zone.
    if (this.tzOffset > Integer.MIN_VALUE) {
      int offset = date.getTimezoneOffset();
      date.setTime(date.getTime() + (this.tzOffset - offset) * 60 * 1000);
      // HBJ date.setTime(date.getTime() + this.tzOffset * 60 * 1000);
    }

    return true;
  }

  /**
   * Set ambiguous year field. This flag indicates that a 2 digit years's
   * century need to be determined by its date/time value. This can only be
   * resolved after its date/time is known.
   * 
   * @param ambiguousYear true if it is ambiguous year.
   */
  public void setAmbiguousYear(boolean ambiguousYear) {
    this.ambiguousYear = ambiguousYear;
  }

  /**
   * Set morning/afternoon field.
   * 
   * @param ampm ampm value.
   */
  public void setAmpm(int ampm) {
    this.ampm = ampm;
  }

  /**
   * Set dayOfMonth field.
   * 
   * @param day dayOfMonth value
   */
  public void setDayOfMonth(int day) {
    this.dayOfMonth = day;
  }

  /**
   * Set dayOfWeek field.
   * 
   * @param dayOfWeek day of the week.
   */
  public void setDayOfWeek(int dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  /**
   * Set Era field.
   * 
   * @param era era value being set.
   */
  public void setEra(int era) {
    this.era = era;
  }

  /**
   * Set hour field.
   * 
   * @param hours hour value.
   */
  @Override
  public void setHours(int hours) {
    this.hours = hours;
  }

  /**
   * Set midnightIs24 field.
   *
   * @param midnightIs24 whether an hour value of 24 signifies midnight.
   */
  public void setMidnightIs24(boolean midnightIs24) {
    this.midnightIs24 = midnightIs24;
  }

  /**
   * Set milliseconds field.
   * 
   * @param milliseconds milliseconds value.
   */
  public void setMilliseconds(int milliseconds) {
    this.milliseconds = milliseconds;
  }

  /**
   * Set minute field.
   * 
   * @param minutes minute value.
   */
  @Override
  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }

  /**
   * Set month field.
   * 
   * @param month month value.
   */
  @Override
  public void setMonth(int month) {
    this.month = month;
  }

  /**
   * Set seconds field.
   * 
   * @param seconds second value.
   */
  @Override
  public void setSeconds(int seconds) {
    this.seconds = seconds;
  }

  /**
   * Set timezone offset, in minutes.
   * 
   * @param tzOffset timezone offset.
   */
  public void setTzOffset(int tzOffset) {
    this.tzOffset = tzOffset;
  }

  /**
   * Set year field.
   * 
   * @param value year value.
   */
  @Override
  public void setYear(int value) {
    this.year = value;
  }
}
