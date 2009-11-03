/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import java.util.Arrays;

/**
 * Represents a GWT version.
 */
public final class GwtVersion implements Comparable<GwtVersion> {

  private static final int NO_NAG = 999;
  private static final String DEFAULT_NO_NAG_VERSION = "0.0." + NO_NAG;

  private static final int COMPONENT_COUNT = 3;

  /**
   *  Array of 3 integers.
   */
  private final int[] components = new int[COMPONENT_COUNT];

  /**
   * The suffix of the release, such as -ms1, -rc2, or random garbage.
   */
  private final String suffix;
  
  /**
   * Create a version that avoids any nagging -- "0.0.999".
   */
  public GwtVersion() {
    this(DEFAULT_NO_NAG_VERSION);
  }

  /**
   * Parse a version number as a string. An empty or null string are
   * explicitly allowed and are equivalent to "0.0.0".
   * 
   * <p>Acceptable format:
   * <ul>
   * <li>prefix before first digit is ignored
   * <li>one or more digits or strings separated by a period
   * <li>optional release number suffix, such as -ms1, -rc3, etc.
   * <li>stops parsing at first space or dash
   * </ul>
   * 
   * <p>The returned version always contains at least 3 components (padding with
   * "0" to 3 components) followed by a release number (which is always last).
   * 
   * @param versionString GWT version in string form, ex: "2.1.0-rc2"
   * @throws NumberFormatException
   */
  public GwtVersion(String versionString) throws NumberFormatException {
    suffix = parse(versionString);
  }

  public int compareTo(GwtVersion other) {
    for (int i = 0; i < COMPONENT_COUNT; ++i) {
      int c = components[i] - other.components[i];
      if (c != 0) {
        return c;
      }
    }
    return compareSuffixes(suffix, other.suffix);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof GwtVersion)) {
      return false;
    }
    GwtVersion other = (GwtVersion) o;
    if (!Arrays.equals(components, other.components)) {
      return false;
    }
    return compareSuffixes(suffix, other.suffix) == 0;
  }

  /**
   * @return a copy of the array of version components, always exactly length 3.
   */
  public int[] getComponents() {
    // Avoid Arrays.copyOf since it was added in JDK1.6
    int[] returnVal = new int[COMPONENT_COUNT];
    System.arraycopy(components, 0, returnVal, 0, COMPONENT_COUNT);
    return returnVal;
  }
  
  /**
   * @return the suffix of this version.  Null indicates no suffix and that this
   * is a released version.
   */
  public String getSuffix() {
    return suffix;
  }

  @Override
  public int hashCode() {
    // all non-null suffixes are treated identically
    return Arrays.hashCode(components) * 2 + (suffix == null ? 0 : 1);
  }

  /**
   * @return true if this version is a special no-nag version (where the user
   * isn't notified that a newer version is available).  This is defined as any
   * version number with 999 in the third component.
   */
  public boolean isNoNagVersion() {
    return components[2] == NO_NAG;
  }
  
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    String prefix = "";
    for (int i = 0; i < COMPONENT_COUNT; ++i) {
      buf.append(prefix).append(components[i]);
      prefix = ".";
    }
    if (suffix != null) {
      buf.append(suffix);
    }
    return buf.toString();
  }

  /**
   * Compare two version number suffixes.  A null suffix is considered a
   * released version and comes after any with a suffix, and all non-null
   * suffixes are considered equal.
   *  
   * @param suffix1
   * @param suffix2
   * @return negative if suffix1 < suffix2, positive if suffix2 > suffix1,
   *     or 0 if they are considered equal
   */
  private int compareSuffixes(String suffix1, String suffix2) {
    if (suffix1 == null) {
      return suffix2 == null ? 0 : 1;
    }
    if (suffix2 == null) {
      return -1;
    }
    return 0;
  }

  /**
   * Parse a string containing a GwtVersion.
   * 
   * <p>Acceptable format:
   * <ul>
   * <li>prefix before first digit is ignored
   * <li>one or more digits or strings separated by a period (at most 3 sets of
   * digits)
   * <li>optional release number suffix, such as -ms1, -rc3, etc.
   * </ul>
   * 
   * <p>The returned version always contains at least 3 components (padding with
   * "0" to 3 components) followed by a release number (which is always last).
   * 
   * @param versionString GWT version in string form, ex: "2.1.0-rc2"
   * @return the trailing suffix, or null if none
   */
  private String parse(String versionString) {
    components[0] = components[1] = components[2] = 0;
    int len = versionString == null ? 0 : versionString.length();
    // Skip leading characters that are not digits to support a
    // non-numeric prefix on a version string.
    int index = 0;
    for (; index < len; ++index) {
      if (Character.isDigit(versionString.charAt(index))) {
        break;
      }
    }
    for (int component = 0; component < COMPONENT_COUNT; ++component) {
      int componentStart = index;
      while (index < len && Character.isDigit(versionString.charAt(index))) {
        ++index;
      }
      if (index > componentStart) {
        components[component] = Integer.parseInt(versionString.substring(
            componentStart, index));
      }
      if (index >= len || versionString.charAt(index) != '.') {
        break;
      }
      ++index;
    }
    return index < len ? versionString.substring(index) : null;
  }
}
