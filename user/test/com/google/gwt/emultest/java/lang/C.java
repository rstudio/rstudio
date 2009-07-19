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
package com.google.gwt.emultest.java.lang;

/**
 * Interface to contain constants shared between StringTest.java and String.java.
 */
public class C {
  public static String FLOAT_STRING = "123.4599";
  public static String DOUBLE_STRING = "123.4599";
  public static float FLOAT_VALUE = 123.459980f;
  public static double DOUBLE_VALUE = 123.459980d;
  public static char CHAR_VALUE = 'd';
  public static String CHAR_STRING = "d";
  public static String CHAR_ARRAY_STRING = "abcdef";
  public static String CHAR_ARRAY_STRING_SUB = "bcde";
  public static char[] CHAR_ARRAY_VALUE = new char[] {
      'a', 'b', 'c', 'd', 'e', 'f'};
  public static String FALSE_STRING = "false";
  public static boolean FALSE_VALUE = false;
  public static boolean TRUE_VALUE = true;
  public static String TRUE_STRING = "true";
  public static final String INT_STRING = "123456789";
  public static final String LONG_STRING = "1234567890123456";
  public static final int INT_VALUE = 123456789;
  public static final long LONG_VALUE = 1234567890123456L;
}
