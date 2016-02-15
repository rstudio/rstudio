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

import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.util.tools.Utility;

/**
 * Java source level compatibility constants.
 */
public enum SourceLevel {
  // Source levels must appear in ascending order for the default setting logic to work.
  JAVA8("1.8", "8");

  /**
   * The default java sourceLevel.
   */
  public static final SourceLevel DEFAULT_SOURCE_LEVEL = getJvmBestMatchingSourceLevel();

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

  /**
   * Returns the SourceLevel given the string or alternate string representation; returns {@code
   * null} if none is found.
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

  private static SourceLevel getJvmBestMatchingSourceLevel() {
    // If everything fails set default to JAVA8.
    String javaSpecLevel = System.getProperty("java.specification.version");
    return getBestMatchingVersion(javaSpecLevel);
  }

  @VisibleForTesting
  public static SourceLevel getBestMatchingVersion(String javaVersionString) {
    try {
      // Find the first version that is less than or equal to javaSpecLevel by iterating in reverse
      // order.
      SourceLevel[] sourceLevels = SourceLevel.values();
      for (int i = sourceLevels.length - 1; i >= 0; i--) {
        if (Utility.versionCompare(javaVersionString, sourceLevels[i].stringValue) >= 0) {
          // sourceLevel is <= javaSpecLevel, so keep this one.
          return sourceLevels[i];
        }
      }
    } catch (IllegalArgumentException e) {
      // If the version can not be parsed fallback to JAVA8.
    }
    // If everything fails set default to JAVA8.
    return JAVA8;
  }
}

