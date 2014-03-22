/*
 * Copyright 2014 Google Inc.
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

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=25
//  date=$Date: 2014-03-01 06:57:43 +0100 (Sat, 01 Mar 2014) $
//  number=$Revision: 9852 $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "hy" locale.
 */
public class DateTimeFormatInfoImpl_hy extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "կեսօրից առաջ",
        "կեսօրից հետո"
    };
  }

  @Override
  public String dateFormatFull() {
    return "yթ. MMMM d, EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "dd MMMM, yթ.";
  }

  @Override
  public String dateFormatMedium() {
    return "dd MMM, y թ.";
  }

  @Override
  public String dateFormatShort() {
    return "dd.MM.yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "մ.թ.ա.",
        "մ.թ."
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "մ.թ.ա.",
        "մ.թ."
    };
  }

  @Override
  public String formatHour24Minute() {
    return "H:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "H:mm:ss";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d MMM";
  }

  @Override
  public String formatMonthFullDay() {
    return "d MMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "d MMMM, EEEE";
  }

  @Override
  public String formatMonthNumDay() {
    return "dd.MM";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "yթ. LLL";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM, yթ.";
  }

  @Override
  public String formatYearMonthFull() {
    return "yթ. LLLL";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM, yթ.";
  }

  @Override
  public String formatYearMonthNum() {
    return "MM.y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "dd.MM.y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "yթ. MMM d, EEE";
  }

  @Override
  public String formatYearQuarterFull() {
    return "y թ, QQQQ";
  }

  @Override
  public String formatYearQuarterShort() {
    return "y թ, Q";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "հունվարի",
        "փետրվարի",
        "մարտի",
        "ապրիլի",
        "մայիսի",
        "հունիսի",
        "հուլիսի",
        "օգոստոսի",
        "սեպտեմբերի",
        "հոկտեմբերի",
        "նոյեմբերի",
        "դեկտեմբերի"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "հունվար",
        "փետրվար",
        "մարտ",
        "ապրիլ",
        "մայիս",
        "հունիս",
        "հուլիս",
        "օգոստոս",
        "սեպտեմբեր",
        "հոկտեմբեր",
        "նոյեմբեր",
        "դեկտեմբեր"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "Հ",
        "Փ",
        "Մ",
        "Ա",
        "Մ",
        "Հ",
        "Հ",
        "Օ",
        "Ս",
        "Հ",
        "Ն",
        "Դ"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "հնվ",
        "փտվ",
        "մրտ",
        "ապր",
        "մյս",
        "հնս",
        "հլս",
        "օգս",
        "սպտ",
        "հկտ",
        "նյմ",
        "դկտ"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1-ին եռամսյակ",
        "2-րդ եռամսյակ",
        "3-րդ եռամսյակ",
        "4-րդ եռամսյակ"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "1-ին եռմս.",
        "2-րդ եռմս.",
        "3-րդ եռմս.",
        "4-րդ եռմս."
    };
  }

  @Override
  public String timeFormatFull() {
    return "H:mm:ss, zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "H:mm:ss, z";
  }

  @Override
  public String timeFormatMedium() {
    return "H:mm:ss";
  }

  @Override
  public String timeFormatShort() {
    return "H:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "կիրակի",
        "երկուշաբթի",
        "երեքշաբթի",
        "չորեքշաբթի",
        "հինգշաբթի",
        "ուրբաթ",
        "շաբաթ"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "Կ",
        "Ե",
        "Ե",
        "Չ",
        "Հ",
        "Ու",
        "Շ"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "կիր",
        "երկ",
        "երք",
        "չրք",
        "հնգ",
        "ուր",
        "շբթ"
    };
  }
}
