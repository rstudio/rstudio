/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.place;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.place.impl.PlaceHistoryMapperGeneratorTest;
import com.google.gwt.place.rebind.PlaceHistoryGeneratorContextTest;
import com.google.gwt.place.shared.PlaceChangeRequestEventTest;
import com.google.gwt.place.shared.PlaceControllerTest;
import com.google.gwt.place.shared.PlaceHistoryHandlerTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests of the place package.
 */
public class PlaceSuite {
  public static Test suite() {
    TestSuite suite = new GWTTestSuite("Tests of the place package");
    suite.addTestSuite(PlaceHistoryMapperGeneratorTest.class);
    suite.addTestSuite(PlaceControllerTest.class);
    suite.addTestSuite(PlaceChangeRequestEventTest.class);
    suite.addTestSuite(PlaceHistoryGeneratorContextTest.class);
    suite.addTestSuite(PlaceHistoryHandlerTest.class);
    return suite;
  }
}
