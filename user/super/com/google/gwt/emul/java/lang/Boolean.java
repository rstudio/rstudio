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

  public static int compare(boolean x, boolean y) {
    return (x == y) ? 0 : (x ? 1 : -1);
  }

  public static int hashCode(boolean value) {
    // The Java API doc defines these magic numbers.
    return value ? 1231 : 1237;
  }

  public static boolean logicalAnd(boolean a, boolean b) {
    return a && b;
  }

  public static boolean logicalOr(boolean a, boolean b) {
    return a || b;
  }

  public static boolean logicalXor(boolean a, boolean b) {
    return a ^ b;
  }

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

  public Boolean(boolean value) {
    /*
     * Call to $createBoolean(value) must be here so that the method is referenced and not pruned
     * before new Boolean(value) is replaced by $createBoolean(value) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createBoolean(value);
  }

  public Boolean(String s) {
     /*
     * Call to $createBoolean(value) must be here so that the method is referenced and not pruned
     * before new Boolean(value) is replaced by $createBoolean(value) by
     * RewriteConstructorCallsForUnboxedTypes.
     */
    $createBoolean(s);
  }

  public native boolean booleanValue() /*-{
    return @javaemul.internal.InternalPreconditions::checkNotNull(Ljava/lang/Object;)(this);
  }-*/;

  @Override
  public int compareTo(Boolean b) {
    return compare(booleanValue(), b.booleanValue());
  };

  @Override
  public native boolean equals(Object o) /*-{
    return this === o;
  }-*/;

  @Override
  public int hashCode() {
    return hashCode(booleanValue());
  }

  @Override
  public String toString() {
    return toString(booleanValue());
  }

  // CHECKSTYLE_OFF: Utility Methods for unboxed Boolean.
  static native Boolean $createBoolean(boolean x) /*-{
    return x;
  }-*/;

  static Boolean $createBoolean(String x) {
    return $createBoolean(Boolean.parseBoolean(x));
  }
  // CHECKSTYLE_ON: End utility methods
}
