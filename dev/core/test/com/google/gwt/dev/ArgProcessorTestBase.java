/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Base class for argument processor testing.
 */
public abstract class ArgProcessorTestBase extends TestCase {
  /*
   * The "compute installation directory" dance.
   */
  static {
    String oldValue = System.getProperty("gwt.devjar");
    System.setProperty("gwt.devjar", "gwt-dev-windows.jar");
    Utility.getInstallPath();
    if (oldValue == null) {
      System.getProperties().remove("gwt.devjar");
    } else {
      System.setProperty("gwt.devjar", oldValue);
    }
  }

  protected static void assertProcessFailure(ArgProcessorBase argProcessor,
      String stringContainedInErrorMessage, String[] args) {
    PrintStream oldErrStream = System.err;
    ByteArrayOutputStream myErrStream = new ByteArrayOutputStream();
    try {
      System.setErr(new PrintStream(myErrStream, true));
      assertFalse(argProcessor.processArgs(args));
    } finally {
      System.err.flush();
      System.setErr(oldErrStream);
    }
    String outputString = new String(myErrStream.toByteArray()).split("[\n\r]")[0];
    assertFalse(outputString.isEmpty());

    assertTrue("\"" + stringContainedInErrorMessage + "\" is not part of the error message: \""
        + outputString + "\"", outputString.contains(stringContainedInErrorMessage));
  }

  protected static void assertProcessSuccess(ArgProcessorBase argProcessor,
      String[] args) {
    PrintStream oldErrStream = System.err;
    ByteArrayOutputStream myErrStream = new ByteArrayOutputStream();
    try {
      System.setErr(new PrintStream(myErrStream, true));
      if (!argProcessor.processArgs(args)) {
        fail(new String(myErrStream.toByteArray()));
      }
      assertEquals(0, myErrStream.size());
    } finally {
      System.setErr(oldErrStream);
    }
  }
}
