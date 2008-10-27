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
 * An implementation of java.sql.Time. Derived from
 * http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Time.html
 */
public class Time extends java.util.Date {
  public static Time valueOf(String s) {
    String[] split = s.split(":");
    if (split.length != 3) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    try {
      int hh = Integer.parseInt(split[0]);
      int mm = Integer.parseInt(split[1]);
      int ss = Integer.parseInt(split[2]);

      return new Time(hh, mm, ss);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }
  }

  @Deprecated
  public Time(int hour, int minute, int second) {
    super(70, 0, 1, hour, minute, second);
  }

  public Time(long time) {
    super(time);
  }

  @Deprecated
  @Override
  public int getDate() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public int getDay() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public int getMonth() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public int getYear() {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public void setDate(int i) {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public void setMonth(int i) {
    throw new IllegalArgumentException();
  }

  @Deprecated
  @Override
  public void setYear(int i) {
    throw new IllegalArgumentException();
  }

  @Override
  public String toString() {
    return padTwo(getHours()) + ":" + padTwo(getMinutes()) + ":"
        + padTwo(getSeconds());
  }
}
