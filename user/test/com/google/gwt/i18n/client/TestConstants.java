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

import com.google.gwt.i18n.client.LocalizableResource.Generate;

import java.util.Map;

/**
 * Interface to represent the contents of resourcePattern bundle
 * com/google/gwt/i18n/client/TestConstants.properties.
 */
@Generate(format = "com.google.gwt.i18n.server.PropertyCatalogFactory")
public interface TestConstants extends com.google.gwt.i18n.client.Constants {

  boolean booleanFalse();

  boolean booleanTrue();

  double doubleNegMax();

  double doubleNegMin();

  double doubleNegOne();

  double doubleOne();

  double doublePi();

  double doublePosMax();

  double doublePosMin();

  double doubleZero();

  float floatNegMax();

  float floatNegMin();

  float floatNegOne();

  float floatOne();

  float floatPi();

  float floatPosMax();

  float floatPosMin();

  float floatZero();

  @Key("string")
  String getString();

  int intMax();

  int intMin();

  int intNegOne();

  int intOne();

  int intZero();

  Map<String, String> mapABCD();

  Map<String, String> mapBACD();

  Map<String, String> mapBBB();

  // raw type test
  @SuppressWarnings("unchecked")
  Map mapDCBA();

  Map<String, String> mapEmpty();

  // Map<String, String> mapWithMissingKey();

  Map<String, String> mapXYZ();

  String[] stringArrayABCDEFG();

  String[] stringArraySizeOneEmptyString();

  String[] stringArraySizeOneWithBackslashX();

  String[] stringArraySizeOneX();

  String[] stringArraySizeThreeAllEmpty();

  String[] stringArraySizeThreeWithDoubleBackslash();

  String[] stringArraySizeTwoBothEmpty();

  String[] stringArraySizeTwoWithEscapedComma();

  String stringDoesNotTrimTrailingThreeSpaces();

  String stringEmpty();

  String stringJapaneseBlue();

  String stringJapaneseGreen();

  String stringJapaneseRed();

  String stringTrimsLeadingWhitespace();
}
