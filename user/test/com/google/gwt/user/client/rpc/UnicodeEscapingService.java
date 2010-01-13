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
package com.google.gwt.user.client.rpc;

/**
 * Service used to test unicode escaping.
 */
public interface UnicodeEscapingService extends RemoteService {
  
  /**
   * Exception for escaping errors.
   */
  public static class InvalidCharacterException extends Exception {

    private static String toHex(int val) {
      String hex = Integer.toHexString(val);
      return "00000".substring(hex.length()) + hex;
    }

    private int index;
    private int expected;
    private int actual;

    protected InvalidCharacterException() { }

    public InvalidCharacterException(int index, int expected, int actual) {
      super(index < 0 ? "String length mismatch: expected = " + expected + ", actual = " + actual
          : "At index " + index + ", expected = U+" + toHex(expected) + ", actual = U+"
          + toHex(actual));
      this.index = index;
      this.expected = expected;
      this.actual = actual;
    }

    public int getActual() {
      return actual;
    }

    public int getExpected() {
      return expected;
    }

    public int getIndex() {
      return index;
    }
  }

  /**
   * Returns a string containing the characters from start to end.
   * 
   * Used to verify server->client escaping.
   * 
   * @param start start character value, inclusive -- note if greater
   *     than {@link Character#MIN_SUPPLEMENTARY_CODE_POINT} it will
   *     be included as surrogate pairs in the returned string.
   * @param end end character value, exclusive (see above comment)
   * @return a string containing the characters from start to end
   */
  String getStringContainingCharacterRange(int start, int end);

  /**
   * Verifies that the string contains the specified characters.
   * 
   * Used to verify client->server escaping.
   * 
   * @param start start code point value included
   * @param end first code point not included
   * @param str string to verify
   * @throws UnicodeEscapingService.InvalidCharacterException if the string does
   *           not contain the specified characters
   * @return true if the verification succeeded
   */
  boolean verifyStringContainingCharacterRange(int start, int end, String str)
      throws UnicodeEscapingService.InvalidCharacterException;
}
