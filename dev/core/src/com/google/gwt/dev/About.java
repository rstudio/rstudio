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
   * @deprecated use {@link #getGwtVersionObject()} or
   *             {@link #getGwtVersionNum()} instead.
   */
  @Deprecated
  public static String GWT_VERSION_NUM;

  /**
   * Tag used for text replacement of the SVN version (split up to avoid
   * replacing it here).
   */ 
  private static final String GWT_SVNREV_TAG = "@GWT_" + "SVNREV@";

  /**
   * Tag used for text replacement of the GWT version (split up to avoid
   * replacing it here).
   */ 
  private static final String GWT_VERSION_TAG = "@GWT_" + "VERSION@";

  private static final String gwtName = "Google Web Toolkit";
  private static final String gwtSvnRev;
  private static final GwtVersion gwtVersion;

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
    // Check for null or sentinel value
    if (tmp == null || tmp.equals(GWT_SVNREV_TAG)) {
      gwtSvnRev = "unknown";
    } else {
      gwtSvnRev = tmp;
    }

    tmp = props.getProperty("gwt.version");
    // Check for null or sentinel value
    if (tmp == null || tmp.equals(GWT_VERSION_TAG)) {
      gwtVersion = new GwtVersion();
    } else {
      gwtVersion = new GwtVersion(tmp);
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
    return gwtVersion.getComponents();
  }

  /**
   * The Google Web Toolkit release number.
   * 
   * @return the release number or the string '0.0.0' if the value couldn't be
   *         determined at build time.
   */
  public static String getGwtVersionNum() {
    return gwtVersion.toString();
  }

  /**
   * The Google Web Toolkit release number.
   * 
   * @return the release number or a version equivalent to "0.0.0" if the value
   *     couldn't be determined at build time.
   */
  public static GwtVersion getGwtVersionObject() {
    // This is public because CheckForUpdates and WebAppCreator need access.
    return gwtVersion;
  }

  private About() {
  }
}
