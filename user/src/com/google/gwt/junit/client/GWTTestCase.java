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
package com.google.gwt.junit.client;

import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.PropertyDefiningStrategy;
import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import junit.framework.TestCase;
import junit.framework.TestResult;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
@SuppressWarnings("unused") 
public abstract class GWTTestCase extends TestCase {

  /**
   * The base class for strategies to use for tests.
   */
  public static class BaseStrategy implements Strategy {
    public String getModuleInherit() {
      return "com.google.gwt.junit.JUnit";
    }

    public String getSyntheticModuleExtension() {
      return "JUnit";
    }

    public void processModule(ModuleDef module) {
    }

    public void processResult(TestCase testCase, JUnitResult result) {
    }
  }

  /**
   * Information about a synthetic module used for testing.
   */
  public static final class TestModuleInfo {
    private String moduleName;
    private String syntheticModuleName;
    private Strategy strategy;

    /**
     * The ordered tests in this synthetic module.
     */
    private Set<TestInfo> tests = new LinkedHashSet<TestInfo>();

    /**
     * Construct a new {@link TestModuleInfo}.
     *
     * @param moduleName the module name
     * @param syntheticModuleName the synthetic module name
     * @param strategy the test {@link Strategy}
     */
    public TestModuleInfo(String moduleName, String syntheticModuleName,
        Strategy strategy) {
      this.moduleName = moduleName;
      this.syntheticModuleName = syntheticModuleName;
      this.strategy = strategy;
    }

    public String getModuleName() {
      return moduleName;
    }

    public Strategy getStrategy() {
      return strategy;
    }

    public String getSyntheticModuleName() {
      return syntheticModuleName;
    }

    /**
     * Returns the tests that are part of this module.
     */
    public Set<TestInfo> getTests() {
      return tests;
    }
  }

  /**
   * Records all live GWTTestCases by synthetic module name so we can optimize
   * run they are compiled and run.  Ordered so that we can precompile the
   * modules in the order that they will run.
   */
  public static final Map<String, TestModuleInfo> ALL_GWT_TESTS = new LinkedHashMap<String, TestModuleInfo>();

  /**
   * The lock for ALL_GWT_TESTS.
   */
  private static final Object ALL_GWT_TESTS_LOCK = new Object();

  /**
   * Get the names of all test modules.
   *
   * @return all test module names
   */
  public static String[] getAllTestModuleNames() {
    synchronized (ALL_GWT_TESTS_LOCK) {
      return ALL_GWT_TESTS.keySet().toArray(new String[ALL_GWT_TESTS.size()]);
    }
  }

  /**
   * Get the number of modules.
   *
   * @return the module count.
   */
  public static int getModuleCount() {
    synchronized (ALL_GWT_TESTS_LOCK) {
      return ALL_GWT_TESTS.size();
    }
  }

  /**
   * Get the set of all {@link TestInfo} for the specified module.
   *
   * @param syntheticModuleName the synthetic module name
   * @return all tests for the module
   */
  public static TestModuleInfo getTestsForModule(String syntheticModuleName) {
    synchronized (ALL_GWT_TESTS_LOCK) {
      return ALL_GWT_TESTS.get(syntheticModuleName);
    }
  }

  /**
   * Object that collects the results of this test case execution.
   */
  protected TestResult testResult = null;

  /**
   * Whether this test case should be always run in pure Java mode (non-GWT).
   * Setting this to <code>true</code> has the same effect as returning
   * <code>null</code> in {@link #getModuleName}.
   *
   * @see #isPureJava
   */
  private boolean forcePureJava;

  /**
   * The {@link Strategy} used by this test.
   */
  private Strategy strategy;

  /**
   * A new instance of your subclass is constructed for each test method that is
   * to be run. You should avoid running code in your subclass constructor,
   * initializer blocks, and field initializations, because if those code blocks
   * must be runnable outside of the GWT environment. As an example of what
   * could go wrong if you run code there, trying to run a JSNI method could
   * generate an {@link UnsatisfiedLinkError}, and trying to call
   * {@link com.google.gwt.core.client.GWT#create(Class)} could throw an
   * {@link UnsupportedOperationException}. Instead, override
   * {@link #gwtSetUp()} and perform any initialization code there.
   */
  public GWTTestCase() {
  }

  /**
   * Does nothing.
   *
   * @deprecated implementation removed
   */
  @Deprecated
  public final void addCheckpoint(String msg) {
  }

  /**
   * Determines whether or not exceptions will be caught by the test fixture.
   * Override this method and return <code>false</code> to let exceptions escape
   * to the browser. This will break the normal JUnit reporting functionality,
   * but can be useful in Production Mode with a JavaScript debugger to pin down
   * where exceptions are originating.
   *
   * @return <code>true</code> for normal JUnit behavior, or <code>false</code>
   *         to disable normal JUnit getException reporting
   */
  public boolean catchExceptions() {
    return true;
  }

  /**
   * Does nothing.
   *
   * @deprecated implementation removed
   */
  @Deprecated
  public final void clearCheckpoints() {
  }

  /**
   * Returns a zero-length array.
   *
   * @deprecated implementation removed
   */
  @Deprecated
  public final String[] getCheckpoints() {
    return new String[0];
  }

  /**
   * Specifies a module to use when running this test case. Subclasses must
   * return the name of a module that will cause the source for that subclass to
   * be included.
   *
   * @return the fully qualified name of a module, or <code>null</code> to run
   *         as a pure Java (non-GWT) test case (same effect as passing
   *         <code>true</code> to {@link #setForcePureJava})
   *
   * @see #isPureJava
   */
  public abstract String getModuleName();

  /**
   * Get the {@link Strategy} to use when compiling and running this test.
   *
   * @return the test {@link Strategy}
   */
  public Strategy getStrategy() {
    if (strategy == null) {
      strategy = createStrategy();
    }
    return strategy;
  }

  /**
   * Get the synthetic module name, which includes the synthetic extension
   * defined by the {@link Strategy}.
   *
   * @return the synthetic module name, or <code>null</code> if this test case
   *         is run in pure Java mode (non-GWT)
   *
   * @see #isPureJava
   */
  public final String getSyntheticModuleName() {
    if (isPureJava()) {
      return null;
    } else {
      return getModuleName() + "." + getStrategy().getSyntheticModuleExtension();
    }
  }

  /**
   * Returns whether this test case should be run in pure Java mode (non-GWT).
   * Returns <code>true</code> if and only if {@link #getModuleName} returns
   * <code>null</code>, or {@link #setForcePureJava} was last invoked with
   * <code>true</code>.
   */
  public boolean isPureJava() {
    return forcePureJava || (getModuleName() == null);
  }

  /**
   * Stashes <code>result</code> so that it can be accessed during
   * {@link #runTest()}.
   */
  @Override
  public final void run(TestResult result) {
    testResult = result;
    super.run(result);
  }

  /**
   * Specifies whether this test case should be always run in pure Java mode
   * (non-GWT). Passing <code>true</code> has the same effect as returning
   * <code>null</code> in {@link #getModuleName}. The setting is
   * <code>false</code> by default.
   *
   * @param forcePureJava <code>true</code> to always run this test case in pure
   *        Java mode (non-GWT); <code>false</code> to run this test case in GWT
   *        mode if {@link #getModuleName} does not return <code>null</code>
   *
   * @see #isPureJava
   */
  public void setForcePureJava(boolean forcePureJava) {
    this.forcePureJava = forcePureJava;
  }

  @Override
  public void setName(String name) {
    super.setName(name);

    synchronized (ALL_GWT_TESTS_LOCK) {
      // Once the name is set, we can add ourselves to the global set.
      String syntheticModuleName = getSyntheticModuleName();
      TestModuleInfo moduleInfo = ALL_GWT_TESTS.get(syntheticModuleName);
      if (moduleInfo == null) {
        // It should be safe to assume that tests with the same synthetic module
        // name have the same test strategy. If they didn't, they would compile
        // over each other.
        moduleInfo = new TestModuleInfo(getModuleName(), syntheticModuleName,
            getStrategy());
        ALL_GWT_TESTS.put(syntheticModuleName, moduleInfo);
      }
      moduleInfo.getTests().add(
          new TestInfo(syntheticModuleName, getClass().getName(), getName()));
    }
  }

  /**
   * Creates the test strategy to use (see {@link #getStrategy()}).
   */
  protected Strategy createStrategy() {
    return new PropertyDefiningStrategy(this);
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
   * <li> If any getException escapes from an event handler during the delay
   * period, the test will error with the thrown getException.</li>
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
   *
   * @throws UnsupportedOperationException if {@link #supportsAsync()} is false
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
   * @throws IllegalStateException if this test is not in asynchronous mode
   * @throws UnsupportedOperationException if {@link #supportsAsync()} is false
   *
   * @see #delayTestFinish(int)
   */
  protected final void finishTest() {
    // implemented in the translatable version of this class
  }

  /**
   * A replacement for JUnit's {@link #setUp()} method. This method runs once
   * per test method in your subclass, just before your each test method runs
   * and can be used to perform initialization. Override this method instead of
   * {@link #setUp()}. This method is run even in pure Java mode (non-GWT).
   *
   * @see #setForcePureJava
   */
  protected void gwtSetUp() throws Exception {
  }

  /**
   * A replacement for JUnit's {@link #tearDown()} method. This method runs once
   * per test method in your subclass, just after your each test method runs and
   * can be used to perform cleanup. Override this method instead of
   * {@link #tearDown()}. This method is run even in pure Java mode (non-GWT).
   *
   * @see #setForcePureJava
   */
  protected void gwtTearDown() throws Exception {
  }

  /**
   * Runs the test via the {@link JUnitShell} environment. Do not override or
   * call this method.
   */
  @Override
  protected void runTest() throws Throwable {
    if (this.getName() == null) {
      throw new IllegalArgumentException(
          "GWTTestCases require a name; \""
              + this.toString()
              + "\" has none.  Perhaps you used TestSuite.addTest() instead of addTestClass()?");
    }

    if (isPureJava()) {
      super.runTest();
    } else {
      JUnitShell.runTest(this, testResult);
    }
  }

  /**
   * This method has been made final to prevent you from accidentally running
   * client code outside of the GWT environment. Please override
   * {@link #gwtSetUp()} instead.
   */
  @Override
  protected final void setUp() throws Exception {
    if (isPureJava()) {
      gwtSetUp();
    }
  }

  /**
   * Returns true if this test case supports asynchronous mode. By default, this
   * is set to true.
   */
  protected boolean supportsAsync() {
    return true;
  }

  /**
   * This method has been made final to prevent you from accidentally running
   * client code outside of the GWT environment. Please override
   * {@link #gwtTearDown()} instead.
   */
  @Override
  protected final void tearDown() throws Exception {
    if (isPureJava()) {
      gwtTearDown();
    }
  }
}
