/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.logging;

import com.google.gwt.junit.GWTMockUtilities;
import com.google.gwt.logging.client.LogConfiguration;

import junit.framework.TestCase;

import java.util.logging.Level;

/**
 * Tests that LogConfiguration can be used outside a GWT context (e.g. JRE tests)
 */
public class LogConfigurationJreTest extends TestCase {

  @Override
  public void setUp() {
    GWTMockUtilities.disarm();
  }

  @Override
  public void tearDown() {
    GWTMockUtilities.restore();
  }

  public void testLogConfiguration() {
    assertTrue(LogConfiguration.loggingIsEnabled());
    assertTrue(LogConfiguration.loggingIsEnabled(Level.FINEST));
  }
}
