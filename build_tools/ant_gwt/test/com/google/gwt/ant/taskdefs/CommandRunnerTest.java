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
package com.google.gwt.ant.taskdefs;

import junit.framework.TestCase;

import java.io.File;

/**
 * Tests for {@link CommandRunner}.
 */
public class CommandRunnerTest extends TestCase {

  /**
   * Test that "java -help" runs successfully.
   */
  public void testGetCommandOutput() {
    String output = CommandRunner.getCommandOutput(new File("."), new File(
        System.getProperty("java.home"), "bin/java").getAbsolutePath(), "-help");
    assertNotNull(output);
  }

  /**
   * Test that a command array is correctly turned into a printable string.
   */
  public void testMakeCmdString() {
    assertEquals("java", CommandRunner.makeCmdString("java"));
    assertEquals("java -version", CommandRunner.makeCmdString("java",
        "-version"));
  }

}
