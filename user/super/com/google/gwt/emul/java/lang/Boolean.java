/*
 * Copyright 2007 Google Inc.
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
package java.lang;

import java.io.Serializable;

/**
 * Wraps native <code>boolean</code> as an object.
 */
public final class Boolean implements Comparable<Boolean>, Serializable {
  /*
   * TODO: figure out how to not clinit this class on direct field access.
   */

  // CHECKSTYLE_OFF: These have to be created somewhere.
  public static final Boolean FALSE = new Boolean(false);
  public static final Boolean TRUE = new Boolean(true);

  // CHECKSTYLE_ON

  public static final Class<Boolean> TYPE = boolean.class;

  public static boolean parseBoolean(String s) {
    return "true".equalsIgnoreCase(s);
  }

  public static String toString(boolean x) {
    return String.valueOf(x);
  }

  public static Boolean valueOf(boolean b) {
    return b ? TRUE : FALSE;
  }

  public static Boolean valueOf(String s) {
    return valueOf(parseBoolean(s));
  }

  private final transient boolean value;

  public Boolean(boolean value) {
    this.value = value;
  }

  public Boolean(String s) {
    this(parseBoolean(s));
  }

  public boolean booleanValue() {
    return value;
  }

  public int compareTo(Boolean other) {
    return (this.value == other.value) ? 0 : (this.value ? 1 : -1);
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Boolean) && (((Boolean) o).value == value);
  }

  @Override
  public int hashCode() {
    // The Java API doc defines these magic numbers.
    return value ? 1231 : 1237;
  }

  @Override
  public String toString() {
    return value ? "true" : "false";
  }
}
