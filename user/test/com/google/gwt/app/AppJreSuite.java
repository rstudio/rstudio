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
package com.google.gwt.app;

import com.google.gwt.app.place.ActivityManagerTest;
import com.google.gwt.app.place.AbstractPlaceHistoryHandlerTest;
import com.google.gwt.app.place.PlaceChangeRequestEventTest;
import com.google.gwt.app.place.PlaceControllerTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite of app package tests that require the JRE.
 */
public class AppJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("app package tests that require the JRE");
    suite.addTestSuite(ActivityManagerTest.class);
    suite.addTestSuite(AbstractPlaceHistoryHandlerTest.class);
    suite.addTestSuite(PlaceControllerTest.class);
    suite.addTestSuite(PlaceChangeRequestEventTest.class);
    return suite;
  }
}
