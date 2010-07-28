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
package com.google.gwt.validation;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.validation.client.SimpleSampleTest;

import junit.framework.Test;

/**
 * All validation tests.
 */
public class ValidationSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test suite for all validation code.");
    suite.addTestSuite(SimpleSampleTest.class);
    return suite;
  }

}
