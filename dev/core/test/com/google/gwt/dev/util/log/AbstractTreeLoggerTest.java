/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests the <code>AbstractTreeLogger</code>.
 */
public class AbstractTreeLoggerTest extends TestCase {

  /**
   * Low-priority branch points don't actually show low-priority messages unless
   * they (later) get a child that is loggable.
   */
  public void testLazyBranchCommit() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(pw);
    logger.setMaxDetail(TreeLogger.WARN);

    final String tstDbgStr = "TEST-DEBUG-STRING";
    final String tstErrStr = "TEST-ERROR-STRING";

    // Emit something that's low-priority and wouldn't show up normally unless
    // it had a higher-priority child log event.
    TreeLogger branch = logger.branch(TreeLogger.DEBUG, tstDbgStr, null);
    assertEquals(-1, sw.toString().indexOf(tstDbgStr));

    // Emit something that's high-priority and will cause both to show up.
    branch.log(TreeLogger.ERROR, tstErrStr, null);

    // Make sure both are now there, in the right order.
    int posTstDbgStr = sw.toString().indexOf(tstDbgStr);
    int posTstErrStr = sw.toString().indexOf(tstErrStr);
    assertTrue(posTstDbgStr != -1);
    assertTrue(posTstErrStr != -1);
    assertTrue(posTstDbgStr < posTstErrStr);
  }

  /**
   * Low-priority branch points don't actually show low-priority messages unless
   * they (later) get a child that is loggable.
   */
  public void testLazyMultiBranchCommit() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(pw);
    logger.setMaxDetail(TreeLogger.WARN);

    final String tstDbg1Str = "TEST-DEBUG-STRING-1";
    final String tstDbg2Str = "TEST-DEBUG-STRING-2";
    final String tstErrStr = "TEST-ERROR-STRING";

    // Emit something that's low-priority and wouldn't show up normally unless
    // it had a higher-priority child log event.
    TreeLogger branch = logger.branch(TreeLogger.DEBUG, tstDbg1Str, null);
    assertEquals(-1, sw.toString().indexOf(tstDbg1Str));

    // Emit something that's low-priority and wouldn't show up normally unless
    // it had a higher-priority child log event.
    branch = branch.branch(TreeLogger.DEBUG, tstDbg2Str, null);
    assertEquals(-1, sw.toString().indexOf(tstDbg2Str));

    // Emit something that's high-priority and will cause both to show up.
    branch.log(TreeLogger.ERROR, tstErrStr, null);

    // Make sure both are now there, in the right order.
    int posTstDbg1Str = sw.toString().indexOf(tstDbg1Str);
    int posTstDbg2Str = sw.toString().indexOf(tstDbg2Str);
    int posTstErrStr = sw.toString().indexOf(tstErrStr);
    assertTrue(posTstDbg1Str != -1);
    assertTrue(posTstDbg2Str != -1);
    assertTrue(posTstErrStr != -1);
    assertTrue(posTstDbg1Str < posTstDbg2Str);
    assertTrue(posTstDbg2Str < posTstErrStr);
  }

  /**
   * We handle out-of-memory conditions specially in the logger to provide more
   * useful log output. It does some slightly weird stuff like turning a regular
   * log() into a branch(), so this test makes sure that doesn't break anything.
   */
  public void testOutOfMemoryLoggerCommitOrderForLog() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(pw);
    logger.setMaxDetail(TreeLogger.WARN);

    final String tstDbgStr = "TEST-DEBUG-STRING";
    final String tstErrStr = "TEST-ERROR-STRING";

    // Emit something that's low-priority and wouldn't show up normally unless
    // it had a higher-priority child log event.
    TreeLogger branch = logger.branch(TreeLogger.DEBUG, tstDbgStr, null);
    assertEquals(-1, sw.toString().indexOf(tstDbgStr));

    // Emit something that's low-priority but that also has a OOM.
    branch.log(TreeLogger.ERROR, tstErrStr, new OutOfMemoryError());

    // Make sure both are now there, in the right order.
    int posTstDbgStr = sw.toString().indexOf(tstDbgStr);
    int posTstErrStr = sw.toString().indexOf(tstErrStr);
    int posOutOfMemory = sw.toString().indexOf(
        AbstractTreeLogger.OUT_OF_MEMORY_MSG);
    assertTrue(posTstDbgStr != -1);
    assertTrue(posTstErrStr != -1);
    assertTrue(posOutOfMemory != -1);
    assertTrue(posTstDbgStr < posTstErrStr);
    assertTrue(posTstErrStr < posOutOfMemory);
  }

  /**
   * We handle stack overflow conditions specially in the logger to provide more
   * useful log output. It does some slightly weird stuff like turning a regular
   * log() into a branch(), so this test makes sure that doesn't break anything.
   */
  public void testStackOverflowLoggerCommitOrderForLog() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(pw);
    logger.setMaxDetail(TreeLogger.WARN);

    final String tstDbgStr = "TEST-DEBUG-STRING";
    final String tstErrStr = "TEST-ERROR-STRING";

    // Emit something that's low-priority and wouldn't show up normally unless
    // it had a higher-priority child log event.
    TreeLogger branch = logger.branch(TreeLogger.DEBUG, tstDbgStr, null);
    assertEquals(-1, sw.toString().indexOf(tstDbgStr));

    // Emit something that's low-priority but that also has a OOM.
    branch.log(TreeLogger.ERROR, tstErrStr, new StackOverflowError());

    // Make sure both are now there, in the right order.
    int posTstDbgStr = sw.toString().indexOf(tstDbgStr);
    int posTstErrStr = sw.toString().indexOf(tstErrStr);
    int posOutOfMemory = sw.toString().indexOf(
        AbstractTreeLogger.STACK_OVERFLOW_MSG);
    assertTrue(posTstDbgStr != -1);
    assertTrue(posTstErrStr != -1);
    assertTrue(posOutOfMemory != -1);
    assertTrue(posTstDbgStr < posTstErrStr);
    assertTrue(posTstErrStr < posOutOfMemory);
  }
}
