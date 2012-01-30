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
package com.google.gwt.typedarrays.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.typedarrays.shared.TypedArrays;

/**
 * Test that client-side code has support on the user agents
 * where it is expected.
 */
public class ClientSupportTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.typedarrays.TypedArraysTest";
  }

  public void testSupported() {
    boolean isSupported = TypedArrays.isSupported();
    String ua = getUserAgent();
    if (ua.contains("msie")) {
      assertEquals("IE10+ should support typed arrays",
          getIeDocumentMode() >= 10, isSupported);
      return;
    }
    if (ua.contains("firefox/")) {
      int idx = ua.indexOf("firefox/") + 8;
      int endIdx = idx;
      int len = ua.length();
      while (endIdx < len && Character.isDigit(ua.charAt(endIdx))) {
        endIdx++;
      }
      int majorVers = Integer.parseInt(ua.substring(idx, endIdx), 10);
      // FF4+ should support typed arrays
      assertEquals("FF" + majorVers, majorVers >= 4, isSupported);
      return;
    }
    if (ua.contains("opera")) {
      // which versions support typed arrays?
      assertTrue(isSupported);
      return;
    }
    if (ua.contains("webkit")) {
      // which versions support typed arrays?
      assertTrue(isSupported);
      return;
    }
    assertFalse("Unknown browser (" + ua + ") assumed not to support typed arrays",
        isSupported);
  }

  private static native String getUserAgent() /*-{
    return navigator.userAgent.toLowerCase();
  }-*/;

  private static native int getIeDocumentMode() /*-{
    return $doc.documentMode || 0;
  }-*/;
}
