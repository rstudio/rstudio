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
 * Implementation of DateTimeFormatInfo for locale "de_BE".
 */
public class DateTimeFormatInfoImpl_de_BE extends DateTimeFormatInfoImpl_de {

  @Override
  public String dateFormatFull() {
    return "EEEE d MMMM y";
  }

  @Override
  public String dateFormatLong() {
    return "d MMMM y";
  }

  @Override
  public String dateFormatShort() {
    return "d/MM/yy";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE d MMM y";
  }

  @Override
  public String timeFormatFull() {
    return "HH 'h' mm 'min' ss 's' zzzz";
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] { 
        "Son",
        "Mon",
        "Die",
        "Mit",
        "Don",
        "Fre",
        "Sam"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
