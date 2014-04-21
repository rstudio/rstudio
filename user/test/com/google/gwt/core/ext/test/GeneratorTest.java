/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext.test;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.dev.util.Name;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Tests the Generator base class.
 */
public class GeneratorTest extends TestCase {

  /**
   * Characters to permute into strings to test escaping accuracy.<br />
   *
   * '_' and 'U' are escape characters<br />
   * '{' has the character code of 123<br />
   * '1', '2', '3' are valid but could potentially collide with the escaped {
   */
  private static char[] testCharacters = {'_', '0', '1', '2', '3', '{' /* char 123 */};

  private static Map<String, String> unescapedStringsByEscapedString = Maps.newHashMap();

  /**
   * Appends one of the test characters to the provided base string until the base string reaches
   * the provided size. Once the base string is large enough it is escaped as a class name and
   * tested for correctness.
   */
  private static void appendCharacterOrTestEscapedClassNamesAreUnique(
      String baseString, int appendCount) {
    if (appendCount == 0) {
      String unescapedString = baseString;
      String escapedString = Generator.escapeClassName(unescapedString);

      assertFalse("collision: " + unescapedString + " -> " + escapedString + ", and "
          + unescapedStringsByEscapedString.get(escapedString) + " -> " + escapedString,
          unescapedStringsByEscapedString.containsKey(escapedString));
      unescapedStringsByEscapedString.put(escapedString, unescapedString);
      return;
    }

    appendCount--;
    for (char testCharacter : testCharacters) {
      appendCharacterOrTestEscapedClassNamesAreUnique(baseString + testCharacter, appendCount);
    }
  }

  private static void permuteStringsAndTestEscapedClassNamesAreUnique(int permutedStringLength) {
    appendCharacterOrTestEscapedClassNamesAreUnique("", permutedStringLength);
  }

  public void testEscapedClassName() {
    assertTrue(Name.isSourceName(Generator.escapeClassName("5{gwt-rpc}")));
  }

  public void testEscapedClassNamesAreUnique() {
    permuteStringsAndTestEscapedClassNamesAreUnique(7);
  }
}
