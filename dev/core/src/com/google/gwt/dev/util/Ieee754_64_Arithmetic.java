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
package com.google.gwt.dev.util;

/**
 * A class to perform arithmetic that is consistent with JavaScript.
 */
public strictfp class Ieee754_64_Arithmetic {

  public static double add(double a, double b) {
    return a + b;
  }

  public static double multiply(double a, double b) {
    return a * b;
  }

  public static double divide(double a, double b) {
    return a / b;
  }

  public static double subtract(double a, double b) {
    return a - b;
  }

  public static double mod(double a, double b) {
    return a % b;
  }

  public static boolean eq(double a, double b) {
    return a == b;
  }

  public static boolean gt(double a, double b) {
    return a > b;
  }

  public static boolean ge(double a, double b) {
    return a >= b;
  }

  public static boolean le(double a, double b) {
    return a <= b;
  }

  public static boolean lt(double a, double b) {
    return a < b;
  }

  public static double neg(double a) {
    return -a;
  }
 }
