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
package com.google.web.bindery.requestfactory.vm;

import com.google.web.bindery.requestfactory.server.BoxesAndPrimitivesJreTest;
import com.google.web.bindery.requestfactory.server.ComplexKeysJreTest;
import com.google.web.bindery.requestfactory.server.FanoutReceiverJreTest;
import com.google.web.bindery.requestfactory.server.FindServiceJreTest;
import com.google.web.bindery.requestfactory.server.LocatorJreTest;
import com.google.web.bindery.requestfactory.server.RequestFactoryExceptionPropagationJreTest;
import com.google.web.bindery.requestfactory.server.RequestFactoryInterfaceValidatorTest;
import com.google.web.bindery.requestfactory.server.RequestFactoryJreTest;
import com.google.web.bindery.requestfactory.server.RequestFactoryPolymorphicJreTest;
import com.google.web.bindery.requestfactory.server.RequestFactoryUnicodeEscapingJreTest;
import com.google.web.bindery.requestfactory.server.ServiceInheritanceJreTest;
import com.google.web.bindery.requestfactory.server.ServiceLocatorTest;
import com.google.web.bindery.requestfactory.shared.impl.SimpleEntityProxyIdTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite of RequestFactory tests that require the JRE (without GWT).
 * 
 * @see com.google.web.bindery.requestfactory.gwt.RequestFactoryGwtJreSuite
 */
public class RequestFactoryJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("requestfactory package tests that require the JRE");
    suite.addTestSuite(BoxesAndPrimitivesJreTest.class);
    suite.addTestSuite(ComplexKeysJreTest.class);
    suite.addTestSuite(FanoutReceiverJreTest.class);
    suite.addTestSuite(FindServiceJreTest.class);
    suite.addTestSuite(LocatorJreTest.class);
    suite.addTestSuite(RequestFactoryExceptionPropagationJreTest.class);
    suite.addTestSuite(RequestFactoryInterfaceValidatorTest.class);
    suite.addTestSuite(RequestFactoryJreTest.class);
    suite.addTestSuite(RequestFactoryPolymorphicJreTest.class);
    suite.addTestSuite(RequestFactoryUnicodeEscapingJreTest.class);
    suite.addTestSuite(ServiceInheritanceJreTest.class);
    suite.addTestSuite(ServiceLocatorTest.class);
    suite.addTestSuite(SimpleEntityProxyIdTest.class);

    return suite;
  }

  /**
   * Used to test the JVM-only client package.
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
