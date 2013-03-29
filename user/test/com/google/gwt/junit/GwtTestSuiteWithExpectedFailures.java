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
package com.google.gwt.junit;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.TestResult;

/**
 * A {@link GWTTestSuite} that can interpret {@link com.google.gwt.junit.client.ExpectedFailure} on
 * test methods.
 */
class GwtTestSuiteWithExpectedFailures extends GWTTestSuite {

  public GwtTestSuiteWithExpectedFailures(String name) {
    super(name);
  }

  @Override
  public void run(TestResult result) {
    super.run(decorateResult(result));
  }

  private TestResult decorateResult(TestResult result) {
    return new TestResultWithExpectedFailures(result);
  }
}
