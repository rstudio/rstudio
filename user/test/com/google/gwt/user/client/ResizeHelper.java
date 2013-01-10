/*
 * Copyright 2012 Google Inc.
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


import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window.Navigator;

import junit.framework.Assert;

/**
 * Calculates the sizes for Window extras such as border, menu, tool bar, and stores the original
 * sizes to restore at the end of test.
 */
public final class ResizeHelper {

  private static int extraWidth;
  private static int extraHeight;

  static {
    // FF4 on win can start in 'almost' fullscreen when the window title bar
    // is hidden but accounted incorrectly, so, move the window and resize to
    // smaller size first, to take it out of 'full screen mode'.
    Window.moveTo(10, 10);
    Window.resizeTo(700, 500);

    extraWidth = 700 - Window.getClientWidth();
    extraHeight = 500 - Window.getClientHeight();
  }

  public static void resizeBy(int width, int height) {
    Window.resizeBy(width, height);
  }

  public static void resizeTo(int width, int height) {
    Window.resizeTo(width, height);
  }

  public static void assertSize(int width, int height) {
    Assert.assertEquals(width, Window.getClientWidth() + extraWidth);
    Assert.assertEquals(height, Window.getClientHeight() + extraHeight);
  }

  public static boolean isResizeSupported() {
    String userAgent = Navigator.getUserAgent();
    if (userAgent.contains("Chrome")) {
      return false; // All versions of Chrome are upsupported
    }

    if (userAgent.contains("Firefox")) {
      RegExp versionRegExp = RegExp.compile("Firefox[\\/\\s](\\d+)\\.\\d+", "ig");
      MatchResult result = versionRegExp.exec(userAgent);
      if (result != null) {
        int version = Integer.parseInt(result.getGroup(1));
        return version < 7; // Resize is unsupported for Firefox 7 and newer.
      }
    }
    return true;
  }
}
