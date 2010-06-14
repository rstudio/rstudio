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
package com.google.gwt.i18n.client.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR DATA

/**
 * Implementation of DateTimeFormatInfo for locale "eu".
 */
public class DateTimeFormatInfoImpl_eu extends DateTimeFormatInfoImpl {

  @Override
  public String dateFormatFull() {
    return "EEEE, y'eko' MMMM'ren' dd'a'";
  }

  @Override
  public String dateFormatLong() {
    return "y'eko' MMM'ren' dd'a'";
  }

  @Override
  public String dateFormatMedium() {
    return "y MMM d";
  }

  @Override
  public String dateFormatShort() {
    return "yyyy-MM-dd";
  }

  @Override
  public String[] erasFull() {
    return new String[] { 
        "BCE",
        "CE"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] { 
        "BCE",
        "CE"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 1;
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "y'eko' MMM'ren' d'a'";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y'eko' MMMM'ren' d'a'";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, y'eko' MMM'ren' d'a'";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ y";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] { 
        "urtarrila",
        "otsaila",
        "martxoa",
        "apirila",
        "maiatza",
        "ekaina",
        "uztaila",
        "abuztua",
        "iraila",
        "urria",
        "azaroa",
        "abendua"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return monthsFull();
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] { 
        "U",
        "O",
        "M",
        "A",
        "M",
        "E",
        "U",
        "A",
        "I",
        "U",
        "A",
        "A"
    };
  }

  @Override
  public String[] monthsNarrowStandalone() {
    return monthsNarrow();
  }

  @Override
  public String[] monthsShort() {
    return new String[] { 
        "urt",
        "ots",
        "mar",
        "api",
        "mai",
        "eka",
        "uzt",
        "abu",
        "ira",
        "urr",
        "aza",
        "abe"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] { 
        "1. hiruhilekoa",
        "2. hiruhilekoa",
        "3. hiruhilekoa",
        "4. hiruhilekoa"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] { 
        "1Hh",
        "2Hh",
        "3Hh",
        "4Hh"
    };
  }

  @Override
  public String timeFormatFull() {
    return "HH:mm:ss zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "HH:mm:ss z";
  }

  @Override
  public String timeFormatMedium() {
    return "HH:mm:ss";
  }

  @Override
  public String timeFormatShort() {
    return "HH:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] { 
        "igandea",
        "astelehena",
        "asteartea",
        "asteazkena",
        "osteguna",
        "ostirala",
        "larunbata"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return weekdaysFull();
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] { 
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7"
    };
  }

  @Override
  public String[] weekdaysNarrowStandalone() {
    return weekdaysNarrow();
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] { 
        "ig",
        "al",
        "as",
        "az",
        "og",
        "or",
        "lr"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
