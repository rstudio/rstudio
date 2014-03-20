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
  /**
   * Tag used for text replacement of the SVN version (split up to avoid
   * replacing it here).
   */ 
  private static final String GWT_GITREV_TAG = "@GWT_" + "GITREV@";

  /**
   * Tag used for text replacement of the GWT version (split up to avoid
   * replacing it here).
   */ 
  private static final String GWT_VERSION_TAG = "@GWT_" + "VERSION@";

  private static final String gwtName = "Google Web Toolkit";
  private static final String gwtGitRev;
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
    tmp = props.getProperty("gwt.gitrev");
    // Check for null or sentinel value
    if (tmp == null || tmp.equals(GWT_GITREV_TAG)) {
      gwtGitRev = "unknown";
    } else {
      gwtGitRev = tmp;
    }

    tmp = props.getProperty("gwt.version");
    // Check for null or sentinel value
    if (tmp == null || tmp.equals(GWT_VERSION_TAG)) {
      gwtVersion = new GwtVersion();
    } else {
      gwtVersion = new GwtVersion(tmp);
    }
  }

  /**
   * Returns the name of the product.
   */
  public static String getGwtName() {
    return gwtName;
  }

  /**
   * Deprecated as GWT is no longer hosted in Subversion, see {@link #getGwtGitRev}.
   * 
   * @return the subversion revision or 'unknown' if the value couldn't be
   *         determined at build time.
   * @deprecated See {@link #getGwtGitRev()}.
   */
  @Deprecated
  public static String getGwtSvnRev() {
    return gwtGitRev;
  }

  /**
   * Returns the Git repository commit id.
   * 
   * @return the Git commit id or 'unknown' if the value couldn't be
   *         determined at build time.
   */
  public static String getGwtGitRev() {
    return gwtGitRev;
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
