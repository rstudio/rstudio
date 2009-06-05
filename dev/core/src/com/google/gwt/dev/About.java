/*
 * Copyright 2006 Google Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * About information for GWT.
 */
public class About {

  // TODO(zundel): These public constants should be removed some day.
  // Java inlines static final constants in compiled classes, leading to
  // version incompatibility warnings.
  /**
   * @deprecated use {@link #getGwtName()} instead.
   */
  @Deprecated
  public static String GWT_NAME;

  /**
   * @deprecated use {@link #getGwtSvnRev()} instead.
   */
  @Deprecated
  public static String GWT_SVNREV;

  /**
   * @deprecated use {@link #getGwtVersion()} instead.
   */
  @Deprecated
  public static String GWT_VERSION;

  /**
   * @deprecated use {@link #getGwtVersionArray()} or
   *             {@link #getGwtVersionNum()} instead.
   */
  @Deprecated
  public static String GWT_VERSION_NUM;

  private static final String gwtName = "Google Web Toolkit";
  private static final String gwtSvnRev;
  private static int[] gwtVersionArray = null;
  private static final String gwtVersionNum;

  static {
    Properties props = new Properties();
    try {
      InputStream instream = About.class.getResourceAsStream("About.properties");
      props.load(instream);
    } catch (IOException iox) {
      // okay... we use default values, then.
    }

    String tmp;
    tmp = props.getProperty("gwt.svnrev");
    // Check for null or sentinel value (break up to avoid text replace)
    if (tmp == null || tmp.equals("@GWT_" + "SVNREV@")) {
      gwtSvnRev = "unknown";
    } else {
      gwtSvnRev = tmp;
    }

    tmp = props.getProperty("gwt.version");
    // Check for null or sentinel value (break up to avoid text replace)
    if (tmp == null || tmp.equals("@GWT_" + "VERSION@")) {
      gwtVersionNum = "0.0.0";
    } else {
      gwtVersionNum = tmp;
    }

    // Initialize deprecated constants
    GWT_NAME = getGwtName();
    GWT_VERSION = getGwtVersion();
    GWT_VERSION_NUM = getGwtVersionNum();
    GWT_SVNREV = getGwtSvnRev();
  }

  /**
   * Returns the name of the product.
   */
  public static String getGwtName() {
    return gwtName;
  }

  /**
   * Returns the Subversion repository revision number.
   * 
   * @return the subversion revision or 'unknown' if the value couldn't be
   *         determined at build time.
   */
  public static String getGwtSvnRev() {
    return gwtSvnRev;
  }

  /**
   * Returns the product name and release number concatenated with a space.
   */
  public static String getGwtVersion() {
    return getGwtName() + " " + getGwtVersionNum();
  }

  /**
   * The Google Web Toolkit release number.
   * 
   * @return the release number or the array {0, 0, 0} if the value couldn't be
   *         determined at build time.
   */
  public static int[] getGwtVersionArray() {
    if (gwtVersionArray == null) {
      gwtVersionArray = parseGwtVersionString(getGwtVersionNum());
    }
    return gwtVersionArray;
  }

  /**
   * The Google Web Toolkit release number.
   * 
   * @return the release number or the string '0.0.0' if the value couldn't be
   *         determined at build time.
   */
  public static String getGwtVersionNum() {
    return gwtVersionNum;
  }

  /**
   * Takes a string formatted as 3 numbers separated by periods and returns an 3
   * element array. Non-numeric prefixes and suffixes are stripped.
   * 
   * @param versionString A string formatted as 3 numbers.
   * @return a 3 element array of the parsed string
   * @throws NumberFormatException if the string is malformed
   */
  public static int[] parseGwtVersionString(String versionString)
      throws NumberFormatException {
    int[] version = {0, 0, 0};
    if (versionString == null) {
      return version;
    }
    int len = versionString.length();
    int index = 0;
    // Skip leading characters that are not digits to support a
    // non-numeric prefix on a version string.
    for (; index < len; ++index) {
      if (Character.isDigit(versionString.charAt(index))) {
        break;
      }
    }
    int part = 0;
    int v = 0;
    for (; index < len; ++index) {
      char ch = versionString.charAt(index);
      if (ch == '.') {
        if (part >= version.length) {
          throw new NumberFormatException("Too many period chracters");
        }
        version[part++] = v;
        v = 0;
      } else if (Character.isDigit(ch)) {
        int digit = Character.digit(ch, 10);
        if (digit < 0) {
          throw new NumberFormatException("Negative number encountered");
        }
        v = v * 10 + digit;
      } else {
        // end the parse to support a non-numeric suffix
        break;
      }
    }
    if (part >= version.length) {
      throw new NumberFormatException("Too many digits in string. Expected 3");
    }
    version[part++] = v;
    if (part != version.length) {
      throw new NumberFormatException("Expected 3 elements in array");
    }
    return version;
  }

  private About() {
  }
}
