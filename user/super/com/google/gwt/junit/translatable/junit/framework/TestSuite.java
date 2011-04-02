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
package junit.framework;

import java.util.Enumeration;

/**
 * Translatable version of JUnit's <code>TestSuite</code>. Although
 * {@link com.google.gwt.junit.client.GWTTestCase GWTTestCase} does
 * not need to access anything above {@link TestCase}, also emulated
 * here, suites and tests are often intermixed. So having an empty
 * emulation tends to avoid spurious error messages, or out right
 * compile failures in -strict mode.
 * <p>
 * There are a few methods useful to TestRunners which we can't
 * emulate for GWT; fortunately, we don't have to.  But we do want to
 * include methods that a TestSuite might sensibly call on itself.
 * They don't have to do more than compile, though.
 * <p>
 * The missing methods are:
 * <ol>
 * <li> static Constructor<?> getTestConstructor(Class<?>)
 * <li> void run(TestResult)
 * <li> public void runTest(Test, TestResult)
 * </ol>
 */
public class TestSuite implements Test {

  public static Test createTest(Class<?> theClass, String name) {
    return null;
  }

  public static Test warning(final String message) {
    return null;
  }

  public TestSuite() {
  }

  public TestSuite(final Class<?> theClass) {
  }

  public TestSuite(Class<? extends TestCase> theClass, String name) {
  }

  public TestSuite(String name) {
  }

  public TestSuite(Class<?>... classes) {
  }

  public TestSuite(Class<? extends TestCase>[] classes, String name) {
  }

  public void addTest(Test test) {
  }

  public void addTestSuite(Class<? extends TestCase> testClass) {
  }

  public int countTestCases() {
    return 0;
  }

  public String getName() {
    return "** No op GWT emulation of TestSuite **";
  }

  public void setName(String name) {
  }

  public Test testAt(int index) {
    return null;
  }

  public int testCount() {
    return 0;
  }

  public Enumeration<Test> tests() {
    return null;
  }
}
