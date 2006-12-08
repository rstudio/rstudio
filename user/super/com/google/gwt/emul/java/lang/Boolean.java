/*
 * Copyright 2006 Google Inc.
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

/**
 * Wraps native <code>boolean</code> as an object.
 */
public final class Boolean {

  // CHECKSTYLE_OFF: These have to be created somewhere.
  public static Boolean FALSE = new Boolean(false);
  public static Boolean TRUE = new Boolean(true);

  // CHECKSTYLE_ON

  public static String toString(boolean x) {
    return String.valueOf(x);
  }

  public static Boolean valueOf(boolean b) {
    return b ? TRUE : FALSE;
  }

  public static Boolean valueOf(String s) {
    if (s != null && s.equalsIgnoreCase("true")) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  private boolean value;

  public Boolean(boolean value) {
    this.value = value;
  }

  public Boolean(String s) {
    this((s != null) && s.equalsIgnoreCase("true"));
  }

  public boolean booleanValue() {
    return value;
  }

  public boolean equals(Object o) {
    return (o instanceof Boolean) && (((Boolean) o).value == value);
  }

  public int hashCode() {
    // The Java API doc defines these magic numbers.
    final int hashCodeForTrue = 1231;
    final int hashCodeForFalse = 1237;
    return value ? hashCodeForTrue : hashCodeForFalse;
  }

  public String toString() {
    return value ? "true" : "false";
  }
}
