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
package com.google.gwt.i18n.client;

import com.google.gwt.i18n.client.constants.DateTimeConstants;

/**
 * Adapter that makes a {@link com.google.gwt.i18n.client.DateTimeFormatInfo}
 * implementation suitable for use with something that wants a
 * {@link DateTimeConstants}.
 */
@SuppressWarnings("deprecation")
class DateTimeConstantsAdapter implements DateTimeConstants {

  private final DateTimeFormatInfo dtfi;
  
  public DateTimeConstantsAdapter(DateTimeFormatInfo dtfi) {
    this.dtfi = dtfi;
  }

  public String[] ampms() {
    return dtfi.ampms();
  }

  public String[] dateFormats() {
    return new String[] {
        dtfi.dateFormatFull(), dtfi.dateFormatLong(), dtfi.dateFormatMedium(),
        dtfi.dateFormatShort(),
    };
  }

  public String[] eraNames() {
    return dtfi.erasFull();
  }

  public String[] eras() {
    return dtfi.erasShort();
  }

  public String firstDayOfTheWeek() {
    return String.valueOf(dtfi.firstDayOfTheWeek() + 1);
  }

  public String[] months() {
    return dtfi.monthsFull();
  }

  public String[] narrowMonths() {
    return dtfi.monthsNarrow();
  }

  public String[] narrowWeekdays() {
    return dtfi.weekdaysNarrow();
  }

  public String[] quarters() {
    return dtfi.quartersFull();
  }

  public String[] shortMonths() {
    return dtfi.monthsShort();
  }

  public String[] shortQuarters() {
    return dtfi.quartersShort();
  }

  public String[] shortWeekdays() {
    return dtfi.weekdaysShort();
  }

  public String[] standaloneMonths() {
    return dtfi.monthsFullStandalone();
  }

  public String[] standaloneNarrowMonths() {
    return dtfi.monthsNarrowStandalone();
  }

  public String[] standaloneNarrowWeekdays() {
    return dtfi.weekdaysNarrowStandalone();
  }

  public String[] standaloneShortMonths() {
    return dtfi.monthsShortStandalone();
  }

  public String[] standaloneShortWeekdays() {
    return dtfi.weekdaysShortStandalone();
  }

  public String[] standaloneWeekdays() {
    return dtfi.weekdaysFullStandalone();
  }

  public String[] timeFormats() {
    return new String[] {
        dtfi.timeFormatFull(), dtfi.timeFormatLong(), dtfi.timeFormatMedium(),
        dtfi.dateFormatShort(),
    };
  }

  public String[] weekdays() {
    return dtfi.weekdaysFull();
  }

  public String[] weekendRange() {
    return new String[] {
        String.valueOf(dtfi.weekendStart() + 1),
        String.valueOf(dtfi.weekendEnd() + 1),
    };
  }
}
