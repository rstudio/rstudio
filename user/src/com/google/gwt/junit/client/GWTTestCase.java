/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.junit.client;

import com.google.gwt.junit.JUnitShell;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * Acts as a bridge between the JUnit environment and the GWT environment. We
 * hook the run method and stash the TestResult object for later communication
 * between the test runner and the unit test shell that drives the test case
 * inside a hosted browser.
 * 
 * <p>
 * There are two versions of this class. This version is the binary version that
 * derives from JUnit's {@link TestCase} and handles all the work of starting up
 * the GWT environment. The other version is a translatable class that is used
 * within the browser. See the <code>translatable</code> subpackage for the
 * translatable implementation.
 * </p>
 */
public abstract class GWTTestCase extends TestCase {

  /*
   * Object that collects the results of this test case execution.
   */
  private TestResult testResult = null;

  /**
   * Add a checkpoint message to the current test. If this test fails, all
   * checkpoint messages will be appended to the exception description. This can
   * be useful in web mode for determining how far test execution progressed
   * before a failure occurs.
   * 
   * @param msg the checkpoint message to add
   * @deprecated This method will be removed when web mode supports stack
   *             traces. It can be useful for debugging web mode failures, but
   *             production code should not depend on it.
   */
  public final void addCheckpoint(String msg) {
    // implemented in the translatable version of this class
  }

  /**
   * Determines whether or not exceptions will be caught by the test fixture.
   * Override this method and return <code>false</code> to let exceptions
   * escape to the browser. This will break the normal JUnit reporting
   * functionality, but can be useful in web mode with a JavaScript debugger to
   * pin down where exceptions are originating.
   * 
   * @return <code>true</code> for normal JUnit behavior, or
   *         <code>false</code> to disable normal JUnit exception reporting
   */
  public boolean catchExceptions() {
    return true;
  }

  /**
   * Clears the accumulated list of checkpoint messages.
   * 
   * @see #addCheckpoint(String)
   * @deprecated This method will be removed when web mode supports stack
   *             traces. It can be useful for debugging web mode failures, but
   *             production code should not depend on it.
   */
  public final void clearCheckpoints() {
    // implemented in the translatable version of this class
  }

  /**
   * Returns the current set of checkpoint messages.
   * 
   * @return a non-<code>null</code> array of checkpoint messages
   * @see #addCheckpoint(String)
   * @deprecated This method will be removed when web mode supports stack
   *             traces. It can be useful for debugging web mode failures, but
   *             production code should not depend on it.
   */
  public final String[] getCheckpoints() {
    // implemented in the translatable version of this class
    return null;
  }

  /**
   * Specifies a module to use when running this test case. Subclasses must
   * return the name of a module that will cause the source for that subclass to
   * be included.
   * 
   * @return the fully qualified name of a module
   */
  public abstract String getModuleName();

  /**
   * Stashes <code>result</code> so that it can be accessed during
   * {@link #runTest()}.
   */
  public final void run(TestResult result) {
    testResult = result;
    super.run(result);
  }

  /**
   * Put the current test in asynchronous mode. If the test method completes
   * normally, this test will not immediately succeed. Instead, a <i>delay
   * period</i> begins. During the delay period, the test system will wait for
   * one of three things to happen:
   * 
   * <ol>
   * <li> If {@link #finishTest()} is called before the delay period expires,
   * the test will succeed.</li>
   * <li> If any exception escapes from an event handler during the delay
   * period, the test will error with the thrown exception.</li>
   * <li> If the delay period expires and neither of the above has happened, the
   * test will error with a {@link TimeoutException}. </li>
   * </ol>
   * 
   * <p>
   * This method is typically used to test event driven functionality.
   * </p>
   * 
   * <p>
   * <b>Example:</b>
   * {@example com.google.gwt.examples.AsyncJUnitExample#testTimer()}
   * </p>
   * 
   * @param timeoutMillis how long to wait before the current test will time out
   * @tip Subsequent calls to this method reset the timeout.
   * @see #finishTest()
   */
  protected final void delayTestFinish(int timeoutMillis) {
    // implemented in the translatable version of this class
  }

  /**
   * Cause this test to succeed during asynchronous mode. After calling
   * {@link #delayTestFinish(int)}, call this method during the delay period to
   * cause this test to succeed. This method is typically called from an event
   * handler some time after the test method returns control to the caller.
   * 
   * <p>
   * Calling this method before the test method completes, will undo the effect
   * of having called <code>delayTestFinish()</code>. The test will revert to
   * normal, non-asynchronous mode.
   * </p>
   * 
   * <p>
   * <b>Example:</b>
   * {@example com.google.gwt.examples.AsyncJUnitExample#testTimer()}
   * </p>
   * 
   * @throws IllegalStateException if this test is not in asynchronous mode.
   * @see #delayTestFinish(int)
   */
  protected final void finishTest() {
    // implemented in the translatable version of this class
  }

  /**
   * Runs the test via the {@link JUnitShell} environment.
   */
  protected final void runTest() throws Throwable {
    JUnitShell.runTest(getModuleName(), this, testResult);
  }
}
