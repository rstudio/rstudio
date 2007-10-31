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
package junit.framework;

/**
 * Translatable version of JUnit's <code>TestCase</code>.
 */
public class TestCase extends Assert implements Test {

  private String name;

  public int countTestCases() {
    return 1;
  }

  public String getName() {
    return name;
  }

  public void runBare() throws Throwable {
    setUp();
    try {
      runTest();
    } finally {
      try {
        tearDown();
      } catch (Throwable e) {
        // ignore any exceptions thrown from teardown
      }
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public String toString() {
    return getName() + "(" + this.getClass().getName() + ")";
  }

  /**
   * Do not override this method, the generated class will override it for you.
   */
  protected void doRunTest(String name) throws Throwable {
  }

  protected void runTest() throws Throwable {
    assertNotNull(name);
    doRunTest(name);
  }

  protected void setUp() throws Exception {
  }

  protected void tearDown() throws Exception {
  }

}
