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
package java.sql;

/**
 * An implementation of java.sql.Date. Derived from
 * http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Date.html
 */
public class Date extends java.util.Date {
  public static Date valueOf(String s) {
    String[] split = s.split("-");
    if (split.length != 3) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    try {
      // Years are relative to 1900
      int y = Integer.parseInt(split[0]) - 1900;
      // Months are internally 0-based
      int m = Integer.parseInt(split[1]) - 1;
      int d = Integer.parseInt(split[2]);

      return new Date(y, m, d);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }
  }

  @Deprecated
  public Date(int year, int month, int day) {
    super(year, month, day);
  }

  @Deprecated
  public Date(long date) {
    super(date);
  }

  @Deprecated
  @Override
  public int getHours() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public int getMinutes() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public int getSeconds() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public void setHours(int i) {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public void setMinutes(int i) {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public void setSeconds(int i) {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  /**
   * Returns the date in <code>yyyy-mm-dd</code> format.
   */
  public String toString() {
    // Months are internally 0-based
    return String.valueOf(1900 + getYear()) + "-" + padTwo(getMonth() + 1)
        + "-" + padTwo(getDate());
  }
}
