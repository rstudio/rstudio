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
 * Implementation of DateTimeFormatInfo for locale "pt_PT".
 */
public class DateTimeFormatInfoImpl_pt_PT extends DateTimeFormatInfoImpl_pt {

  @Override
  public String[] ampms() {
    return new String[] { 
        "Antes do meio-dia",
        "Depois do meio-dia"
    };
  }

  @Override
  public String dateFormatMedium() {
    return "d 'de' MMM 'de' yyyy";
  }

  @Override
  public String formatHour12Minute() {
    return "h:mm";
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "h:mm:ss";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQ 'de' yyyy";
  }

  @Override
  public String formatYearQuarterShort() {
    return "QQQ 'de' yyyy";
  }

  @Override
  public String[] monthsFull() {
    return new String[] { 
        "Janeiro",
        "Fevereiro",
        "Março",
        "Abril",
        "Maio",
        "Junho",
        "Julho",
        "Agosto",
        "Setembro",
        "Outubro",
        "Novembro",
        "Dezembro"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return monthsFull();
  }

  @Override
  public String[] monthsShort() {
    return new String[] { 
        "Jan",
        "Fev",
        "Mar",
        "Abr",
        "Mai",
        "Jun",
        "Jul",
        "Ago",
        "Set",
        "Out",
        "Nov",
        "Dez"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] { 
        "1.º trimestre",
        "2.º trimestre",
        "3.º trimestre",
        "4.º trimestre"
    };
  }
}
