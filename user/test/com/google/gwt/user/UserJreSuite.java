/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user;

import com.google.gwt.user.rebind.AbstractSourceCreatorTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * User tests running as a regular JRE test.
 */
public class UserJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("Non-browser tests for com.google.gwt.user");

    // $JUnit-BEGIN$
    suite.addTestSuite(AbstractSourceCreatorTest.class);
    // $JUnit-END$

    return suite;
  }

}
