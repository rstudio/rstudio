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
package com.google.gwt.junit.client.impl;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * A place holder for test cases that are not found in {@code TypeOracle} which usually indicates a
 * compilation error in the test class.
 */
public class MissingTestPlaceHolder extends GWTTestCase {

  @Override
  public String getModuleName() {
    throw new AssertionError("unexpected call");
  }

  protected void doRunTest(String name) throws Throwable {
    throw new RuntimeException("Test class is missing.\n"
        + "(Previous compiler errors may have made this test unavailable. See the test log.)");
  }
}
