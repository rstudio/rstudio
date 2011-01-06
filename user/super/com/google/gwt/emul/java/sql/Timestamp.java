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
 * An implementation of java.sql.Timestame. Derived from
 * http://java.sun.com/j2se/1.5.0/docs/api/java/sql/Timestamp.html. This is
 * basically just regular Date decorated with a nanoseconds field.
 */
public class Timestamp extends java.util.Date {

  public static Timestamp valueOf(String s) {
    String[] components = s.split(" ");
    if (components.length != 2) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    String[] timeComponents = components[1].split("\\.");
    boolean hasNanos = true;
    int nanos = 0;
 
    if (timeComponents.length == 1) {
      // Allow timestamps without .fffffffff nanoseconds field
      hasNanos = false;
    } else if (timeComponents.length != 2) {
      throw new IllegalArgumentException("Invalid escape format: " + s);
    }

    Date d = Date.valueOf(components[0]);
    Time t = Time.valueOf(timeComponents[0]);
    if (hasNanos) {
      String nanosString = timeComponents[1];
      int len = nanosString.length();
      assert len > 0; // len must be > 0 if hasNanos is true
      if (len > 9) {
        throw new IllegalArgumentException("Invalid escape format: " + s);
      }

      // Pad zeros on the right up to a total of 9 digits
      if (len < 9) {
        nanosString += "00000000".substring(len - 1);
      }

      try {
        nanos = Integer.valueOf(nanosString);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid escape format: " + s);
      }
    }

    return new Timestamp(d.getYear(), d.getMonth(), d.getDate(), t.getHours(),
        t.getMinutes(), t.getSeconds(), nanos);
  }

  private static String padNine(int value) {
    StringBuffer toReturn = new StringBuffer("000000000");
    assert toReturn.length() == 9;

    String asString = String.valueOf(value);
    toReturn = toReturn.replace(9 - asString.length(), 9, asString);

    assert toReturn.length() == 9;
    return toReturn.toString();
  }

  /**
   * Stores the nanosecond resolution of the timestamp; must be kept in sync
   * with the sub-second part of Date.millis.
   */
  private int nanos;

  @Deprecated
  public Timestamp(int year, int month, int date, int hour, int minute,
      int second, int nano) {
    super(year, month, date, hour, minute, second);
    setNanos(nano);
  }

  public Timestamp(long time) {
    super(time);

    // Seed the milliseconds in nanos
    nanos = (((int) (time % 1000)) * 1000000);
  }

  public boolean after(Timestamp ts) {
    return (getTime() > ts.getTime())
        || (getTime() == ts.getTime() && getNanos() > ts.getNanos());
  }

  public boolean before(Timestamp ts) {
    return (getTime() < ts.getTime())
        || (getTime() == ts.getTime() && getNanos() < ts.getNanos());
  }

  @Override
  public int compareTo(java.util.Date o) {
    if (o instanceof Timestamp) {
      return compareTo((Timestamp) o);
    } else {
      return compareTo(new Timestamp(o.getTime()));
    }
  }

  public int compareTo(Timestamp o) {
    int sign = Long.signum(getTime() - o.getTime());
    return sign == 0 ? getNanos() - o.getNanos() : sign;
  }

  @Override
  public boolean equals(Object ts) {
    // Timestamps can't be compared to java.util.Date
    // This is known to not be symmetric, which follows the JRE.
    return ts instanceof Timestamp ? equals((Timestamp) ts) : false;
  }

  public boolean equals(Timestamp ts) {
    return (getTime() == ts.getTime() && getNanos() == ts.getNanos());
  }

  public int getNanos() {
    return nanos;
  }

  @Override
  public long getTime() {
    return super.getTime();
  }

  @Override
  public int hashCode() {
    // This is correct, per the Javadoc
    return super.hashCode();
  }

  public void setNanos(int n) {
    if (n < 0 || n > 999999999) {
      throw new IllegalArgumentException("nanos out of range " + n);
    }

    nanos = n;
    // Clear the sub-second part of the date; replace with new nanos.
    super.setTime((getTime() / 1000) * 1000 + (nanos / 1000000));
  }

  @Override
  public void setTime(long time) {
    super.setTime(time);
    nanos = (((int) (time % 1000)) * 1000000);
  }

  @Override
  public String toString() {
    return String.valueOf(1900 + getYear()) + "-" + padTwo(1 + getMonth())
        + "-" + padTwo(getDate()) + " " + padTwo(getHours()) + ":"
        + padTwo(getMinutes()) + ":" + padTwo(getSeconds()) + "."
        + padNine(getNanos());
  }
}
