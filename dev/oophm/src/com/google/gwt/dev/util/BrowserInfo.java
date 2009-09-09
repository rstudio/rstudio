/**
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
package com.google.gwt.dev.util;

import com.google.gwt.dev.shell.Icons;

import javax.swing.ImageIcon;

/**
 * Holds information about the browser used in the UI.
 */
public class BrowserInfo {

  private static final String UNKNOWN = "Unknown";
  private static final String FIREFOX = "FF";
  private static final String SAFARI = "Safari";
  private static final String OPERA = "Opera";
  private static final String CHROME = "Chrome";
  private static final String IE = "IE";

  /**
   * Choose an icon and short name appropriate for this browser.  The icon
   * may be null.
   * 
   * @param userAgent User-Agent string from browser
   * @return icon or null if none
   */
  public static BrowserInfo getBrowserInfo(String userAgent) {
    ImageIcon browserIcon = null;
    String shortName = getShortName(userAgent);
    if (shortName.equals(IE)) {
      browserIcon = Icons.getIE24();
    } else if (shortName.equals(CHROME)) {
      browserIcon = Icons.getChrome24();
    } else if (shortName.equals(OPERA)) {
      browserIcon = Icons.getOpera24();
    } else if (shortName.equals(SAFARI)) {
      browserIcon = Icons.getSafari24();
    } else if (shortName.equals(FIREFOX)) {
      browserIcon = Icons.getFirefox24();
    }
    return new BrowserInfo(browserIcon, shortName);
  }

  public static String getShortName(String userAgent) {
    String lcAgent = userAgent.toLowerCase();
    if (lcAgent.contains("msie")) {
      return IE;
    } else if (lcAgent.contains("chrome")) {
      return CHROME;
    } else if (lcAgent.contains("opera")) {
      return OPERA;
    } else if (lcAgent.contains("webkit") || lcAgent.contains("safari")) {
      return SAFARI;
    } else if (lcAgent.contains("firefox")) {
      return FIREFOX;
    }
    return UNKNOWN;
  }
  private final ImageIcon icon;
  private final String shortName;

  /**
   * Create a BrowserInfo instance.
   * 
   * @param icon
   * @param shortName
   */
  private BrowserInfo(ImageIcon icon, String shortName) {
    this.icon = icon;
    this.shortName = shortName;
  }

  /**
   * @return the icon used to identify this browser, or null if none.
   */
  public ImageIcon getIcon() {
    return icon;
  }

  /**
   * @return the short name used to identify this browser, or null if none.
   */
  public String getShortName() {
    return shortName;
  }
}