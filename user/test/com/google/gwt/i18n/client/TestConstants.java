// Copyright 2006 Google Inc. All Rights Reserved.
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
  
  Map mapABCD();
  Map mapDCBA();
  Map mapBACD();
  Map mapBBB();
  Map mapXYZ();
  
  // uncomment for desk tests
  //Map mapWithMissingKey();
  
  // uncomment for desk tests
  //Map mapEmpty();
}
