/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.i18n.shared.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=21.0
//  number=$Revision: 4582 $
//  date=$Date: 2010-02-05 11:51:25 -0500 (Fri, 05 Feb 2010) $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "sr_Cyrl_BA" locale.
 */
public class DateTimeFormatInfoImpl_sr_Cyrl_BA extends DateTimeFormatInfoImpl_sr {

  @Override
  public String dateFormatMedium() {
    return "yyyy-MM-dd";
  }

  @Override
  public String dateFormatShort() {
    return "yy-MM-dd";
  }

  @Override
  public String formatHour24Minute() {
    return "HH:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "HH:mm:ss";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "јануар",
        "фебруар",
        "март",
        "април",
        "мај",
        "јуни",
        "јули",
        "август",
        "септембар",
        "октобар",
        "новембар",
        "децембар"
    };
  }

  @Override
  public String timeFormatFull() {
    return "HH 'часова', mm 'минута', ss 'секунди' zzzz";
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
        "недеља",
        "понедељак",
        "уторак",
        "сриједа",
        "четвртак",
        "петак",
        "субота"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "нед",
        "пон",
        "уто",
        "сри",
        "чет",
        "пет",
        "суб"
    };
  }
}
