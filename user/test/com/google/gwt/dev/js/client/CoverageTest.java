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
package com.google.gwt.dev.js.client;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.storage.client.Storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests coverage instrumentation.
 */
public class CoverageTest extends GWTTestCase {
  private static final Map<String, Double> EXPECTED_COVERAGE = new HashMap<String, Double>() { {
      put("25", 1.0);
      put("26", 1.0);
      put("27", 1.0);
      put("29", 1.0);
      put("31", 0.0);
  }};

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.js.CoverageTestModule";
  }

  /*
   * Trigger the onbeforeunload handler. It would be nice to do this by refreshing the page or
   * something, but that causes the test to fail.
   */
  private static native void fireOnBeforeUnloadEvent() /*-{
    for (var i = 0; i < $wnd.frames.length; i++) {
      if (typeof $wnd.frames[i].onbeforeunload === 'function') {
        $wnd.frames[i].onbeforeunload();
      }
    }
  }-*/;

  public void testCoverageDataIsFlushedToLocalStorageOnBeforeUnload() {
    Storage localStorage = Storage.getLocalStorageIfSupported();
    assertNotNull("Test browser does not support localStorage", localStorage);
    // No coverage initially
    assertNull("Found unexpected initial coverage", localStorage.getItem("gwt_coverage"));

    CoverageTestModule.method();

    // Trigger the onbeforeunload handler to flush the coverage information to localStorage.
    fireOnBeforeUnloadEvent();
    String coverageAsJson = localStorage.getItem("gwt_coverage");
    assertNotNull("No coverage data found", coverageAsJson);
    JSONObject coverage = JSONParser.parseStrict(coverageAsJson).isObject();
    assertNotNull("Coverage data was not valid JSON", coverage);

    JSONObject fileCoverage =
        coverage.get("com/google/gwt/dev/js/client/CoverageTestModule.java").isObject();
    assertNotNull(fileCoverage);
    for (Map.Entry<String, Double> lineCoverage : EXPECTED_COVERAGE.entrySet()) {
      assertTrue(fileCoverage.containsKey(lineCoverage.getKey()));
      JSONNumber value = fileCoverage.get(lineCoverage.getKey()).isNumber();
      assertNotNull(value);
      assertEquals(lineCoverage.getValue(), value.doubleValue(), 0.0001);
    }
  }
}
