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
package com.google.gwt.user.client;

/**
 * This class can be used as a substitute for {@link java.util.Random}. The
 * semantics differ in that the underlying browser's implementation is used. The
 * random generator cannot be seeded or otherwise used to reproduce a particular
 * sequence of results.
 */
public final class Random {

  /**
   * Returns true or false with roughly equal probability. The underlying
   * browser's random implementation is used.
   */
  public static native boolean nextBoolean() /*-{
    return Math.random() < 0.5;
  }-*/;

  /**
   * Returns a random <code>double</code> between 0 (inclusive) and 1
   * (exclusive). The underlying browser's random implementation is used.
   */
  public static native double nextDouble() /*-{
    return Math.random();
  }-*/;

  /**
   * Returns a random <code>int</code> between -2147483648 and 2147483647
   * (inclusive) with roughly equal probability of returning any particular
   * <code>int</code> in this range. The underlying browser's random
   * implementation is used.
   */
  public static native int nextInt() /*-{
    // "~~" forces the value to a 32 bit integer.
    return ~~(Math.floor(Math.random() * 4294967296) - 2147483648);
  }-*/;

  /**
   * Returns a random <code>int</code> between 0 (inclusive) and
   * <code>upperBound</code> (exclusive) with roughly equal probability of
   * returning any particular <code>int</code> in this range. The underlying
   * browser's random implementation is used.
   */
  public static native int nextInt(int upperBound) /*-{
    // "~~" forces the value to a 32 bit integer.
    return ~~(Math.floor(Math.random() * upperBound));
  }-*/;

  /**
   * Not instantiable. Having different instances of this class would not be
   * meaningful because no state is stored and the common browser implementation
   * is shared.
   */
  private Random() {
  }
}
