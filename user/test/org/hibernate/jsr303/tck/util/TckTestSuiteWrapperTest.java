/*
 * Copyright 2010 Google Inc.
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
package org.hibernate.jsr303.tck.util;

import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.TestCase;

import org.hibernate.jsr303.tck.util.client.Failing;
import org.hibernate.jsr303.tck.util.client.NonTckTest;
import org.hibernate.jsr303.tck.util.client.NotSupported;
import org.hibernate.jsr303.tck.util.client.NotSupported.Reason;

/**
 * Tests for {@link TckTestSuiteWrapper}.
 */
public class TckTestSuiteWrapperTest extends TestCase {

  /**
   * Sample test with a method annotated as {@link Failing}.
   */
  public static class Fail extends Base {

    public Fail() {
    }

    @Failing(issue = 123)
    public void testFailing() {
    }

    public void testOne() {
    }

    public void testTwo() {
    }
  }

  /**
   * Sample test with a method annotated as {@link NonTckTest}.
   */
  public static class Non extends Base {

    public Non() {
    }

    @NonTckTest
    public void testNon() {
    }

    public void testOne() {
    }

    public void testTwo() {
    }
  }

  /**
   * Sample test without annotated test messages.
   */
  public static class Normal extends Base {

    public Normal() {
    }

    public void testOne() {
    }

    public void testTwo() {
    }
  }

  /**
   * Sample test with a method annotated as {@link NotSupported}.
   */
  public static class Not extends Base {

    public Not() {
    }

    @NotSupported(reason = Reason.IO)
    public void testFailing() {
    }

    public void testOne() {
    }

    public void testTwo() {
    }
  }

  private abstract static class Base extends GWTTestCase {

    public Base() {
    }

    @Override
    public final String getModuleName() {
      return "org.hibernate.jsr303.tck.tests.metadata.TckTest";
    }
  }

  private TckTestSuiteWrapper suite;

  public void checkAddTest(Class<? extends GWTTestCase> clazz, int expected) {
    suite.addTestSuite(clazz);
    assertEquals(expected, suite.testAt(0).countTestCases());
  }

  public void testAddTestMarkedFailing() throws Exception {
    checkAddTest(Fail.class, 2);
  }

  public void testAddTestMarkedNotSupported() throws Exception {
    checkAddTest(Not.class, 2);
  }

  public void testAddTestNon() throws Exception {
    checkAddTest(Non.class, 3);
  }

  public void testAddTestNormal() throws Exception {
    checkAddTest(Normal.class, 2);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    suite = new TckTestSuiteWrapper("Test");
  }

}
