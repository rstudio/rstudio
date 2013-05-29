/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.util.arg;

/**
 * Java source level compatibility constants.
 * Java versions range from 1.0 to 1.7.
 * Versions 1.5, 1.6 and 1.7 are also referred as 5, 6 and 7 respectively.
 *
 * Both names can be used indistinctly.
 */
public enum SourceLevel {
  // Source levels must appear in ascending order in order.
  JAVA6("1.6", "6"),
  JAVA7("1.7", "7");
  /**
   * The default GWT source level is the one that matches best that of the runtime environment.
   */
  public static final SourceLevel DEFAULT_SOURCE_LEVEL;

  private final String stringValue;
  private final String altStringValue;

  SourceLevel(String stringValue, String altStringValue) {
    this.stringValue = stringValue;
    this.altStringValue = altStringValue;
  }

  /**
   * Returns a string value representation for the source level.
   */
  public String getStringValue() {
    return stringValue;
  }

  /**
   * Returns an alternate string value representation for the source level.
   */
  public String getAltStringValue() {
    return altStringValue;
  }

  @Override
  public String toString() {
    return stringValue;
  }

  static {
    SourceLevel result = SourceLevel.values()[0];
    /* TODO(rluble): source level should be set automatically to match the java version
     * but a JDT bug is preventing the change (some correct code does not compile
     * under sourceLevel 7).
     * Uncomment the following code use the JDT is fixed or patched.

    String javaSpecLevel = System.getProperty("java.specification.version");
    try {
      for (SourceLevel sourceLevel : SourceLevel.values()) {
        if (Utility.versionCompare(javaSpecLevel, sourceLevel.stringValue) >= 0) {
          result = sourceLevel;
        }
      }
    } catch (IllegalArgumentException e) {
    }
    */
    DEFAULT_SOURCE_LEVEL = result;
  }

  /**
   * Returns the SourceLevel given the string or alternate string representation;
   * returns {@code null} if none is found.
   */
  public static SourceLevel fromString(String sourceLevelString) {
    if (sourceLevelString == null) {
      return null;
    }
    for (SourceLevel sourceLevel : SourceLevel.values()) {
      if (sourceLevel.stringValue.equals(sourceLevelString) ||
          sourceLevel.altStringValue.equals(sourceLevelString)) {
        return sourceLevel;
      }
    }
    return null;
  }

}
