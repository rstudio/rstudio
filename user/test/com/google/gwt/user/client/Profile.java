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
package com.google.gwt.user.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * TODO: document me.
 */
public abstract class Profile extends GWTTestCase {
  
  /**
   * An enumeration defining the possible report types.
   */
  public enum ReportType {
    REPORT_TO_BROWSER, REPORT_TO_EXCEL, REPORT_TO_WIKI
  }

  private static String browser;
  private static ReportType reportType = ReportType.REPORT_TO_WIKI;
  private static double time;

  public static void setReportType(ReportType type) {
    reportType = type;
  }

  private void browserTiming(String s) {
    double elapsed = Duration.currentTimeMillis() - time;
    RootPanel.get().add(
        new Label("|" + browser + "|" + s + "|" + elapsed + " milliseconds|"));
  }

  protected void resetTimer() {
    time = Duration.currentTimeMillis();
    if (browser == null) {
      browser = getBrowser();
    }
  }

  protected void timing(String s) {
    double elapsed = Duration.currentTimeMillis() - time;
    switch (reportType) {
      case REPORT_TO_BROWSER:
        browserTiming(s);
        break;
      case REPORT_TO_WIKI:
        this.addCheckpoint("|" + browser + "|" + s + "|" + elapsed
            + " milliseconds|");
        break;
      case REPORT_TO_EXCEL:
        s = s.replace('|', '\t');
        this.addCheckpoint(browser + "\t" + s + "\t" + elapsed);
        break;
      default:
        throw new IllegalStateException("Should not ever get here");
    }
  }

  public native String getBrowser() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("opera") != -1) {
     return "opera";
    }
    else if (ua.indexOf("safari") != -1) {
     return "safari";
    }
    else if ((ua.indexOf("msie 6.0") != -1) ||
     (ua.indexOf("msie 7.0") != -1)) {
    return "ie6";
    }
    else if (ua.indexOf("gecko") != -1) {
      return "gecko1_8";
    }
    return "unknown";
  }-*/;
}
