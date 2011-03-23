/*
 * Copyright 2011 Google Inc.
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

import junit.framework.TestCase;

/**
 * Tests code in {@link JUnitShell}.
 */
public class JUnitShellTest extends TestCase {
  private JUnitShell shell;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    shell = new JUnitShell();
  }

  public void testDefaultModuleUrl() throws Exception {
    parseGoodArgs();
    assertEquals("http://localhost:1234/example/junit-standards.html?gwt.codesvr=localhost:456",
        shell.getModuleUrl("localhost", 1234, "example", 456));
  }

  public void testExplicitStandardsModeModuleUrl() throws Exception {
    parseGoodArgs("-standardsMode");
    assertEquals("http://localhost:1234/example/junit-standards.html?gwt.codesvr=localhost:456",
        shell.getModuleUrl("localhost", 1234, "example", 456));
  }

  public void testExplicitQuirksModeModuleUrl() throws Exception {
    parseGoodArgs("-quirksMode");
    assertEquals("http://localhost:1234/example/junit.html?gwt.codesvr=localhost:456",
        shell.getModuleUrl("localhost", 1234, "example", 456));
  }

  private void parseGoodArgs(String... argsToUse) {
    JUnitShell.ArgProcessor processor = new JUnitShell.ArgProcessor(shell);
    assertTrue("didn't accept good args", processor.processArgs(argsToUse));
  }
}
