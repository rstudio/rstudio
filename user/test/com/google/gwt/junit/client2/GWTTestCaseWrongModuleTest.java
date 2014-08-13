/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.junit.client2;

import com.google.gwt.junit.JUnitFatalLaunchException;
import com.google.gwt.junit.client.ExpectedFailure;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@code GWTTestCase} behaves properly when getModuleName returns a module that doesn't
 * include the test itself.
 * <p>
 * This class is not under junit/client because junit synthesized module always inherits
 * com.google.gwt.junit.Junit which source globs junit/client hence includes this class regardless
 * of what getModuleName returns.
 */
public final class GWTTestCaseWrongModuleTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core"; // intentionally incorrect
  }

  @ExpectedFailure(withType = JUnitFatalLaunchException.class, withMessage = "no compilation unit")
  public void testCompileError() throws Exception {
    // Nothing to do here
  }
}
