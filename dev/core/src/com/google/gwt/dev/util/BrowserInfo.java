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
package com.google.gwt.dev.util;

import java.util.Locale;

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
   * Retrieve a short name, suitable for use in a tab or filename, for a given
   * user agent.
   *
   * @param userAgent
   * @return short name of user agent
   */
  public static String getShortName(String userAgent) {
    String lcAgent = userAgent.toLowerCase(Locale.ENGLISH);
    if (lcAgent.contains("msie")) {
      return IE;
    } else if (lcAgent.contains("opr")) {
      return OPERA;
    } else if (lcAgent.contains("chrome")) {
      return CHROME;
    } else if (lcAgent.contains("webkit") || lcAgent.contains("safari")) {
      return SAFARI;
    } else if (lcAgent.contains("firefox") || lcAgent.contains("minefield")) {
      return FIREFOX;
    }
    return UNKNOWN;
  }

  private BrowserInfo() {
  }
}
