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
package com.google.gwt.i18n.client;

import java.util.Map;

/**
 * Interface to represent the contents of resourcePattern bundle
 * com/google/gwt/i18n/client/TestConstants.properties.
 */
public interface TestConstants extends com.google.gwt.i18n.client.Constants {

  /**
   * @gwt.key string
   */
  String getString();

  String stringTrimsLeadingWhitespace();

  String stringDoesNotTrimTrailingThreeSpaces();

  String stringEmpty();

  String stringJapaneseRed();

  String stringJapaneseGreen();

  String stringJapaneseBlue();

  int intZero();

  int intOne();

  int intNegOne();

  int intMax();

  int intMin();

  float floatPi();

  float floatZero();

  float floatOne();

  float floatNegOne();

  float floatPosMax();

  float floatPosMin();

  float floatNegMax();

  float floatNegMin();

  double doublePi();

  double doubleZero();

  double doubleOne();

  double doubleNegOne();

  double doublePosMax();

  double doublePosMin();

  double doubleNegMax();

  double doubleNegMin();

  String[] stringArrayABCDEFG();

  String[] stringArraySizeOneEmptyString();

  String[] stringArraySizeOneX();

  String[] stringArraySizeTwoBothEmpty();

  String[] stringArraySizeThreeAllEmpty();

  String[] stringArraySizeTwoWithEscapedComma();

  String[] stringArraySizeOneWithBackslashX();

  String[] stringArraySizeThreeWithDoubleBackslash();

  boolean booleanFalse();

  boolean booleanTrue();

  Map<String, String> mapABCD();

  // raw type test
  @SuppressWarnings("unchecked")
  Map mapDCBA();

  Map<String, String> mapBACD();

  Map<String, String> mapBBB();

  Map<String, String> mapXYZ();

  // uncomment for desk tests
  // Map mapWithMissingKey();

  // uncomment for desk tests
  // Map mapEmpty();
}
