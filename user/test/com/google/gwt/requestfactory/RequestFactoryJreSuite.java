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
package com.google.gwt.requestfactory;

import com.google.gwt.requestfactory.rebind.model.RequestFactoryModelTest;
import com.google.gwt.requestfactory.server.BoxesAndPrimitivesJreTest;
import com.google.gwt.requestfactory.server.ComplexKeysJreTest;
import com.google.gwt.requestfactory.server.FindServiceJreTest;
import com.google.gwt.requestfactory.server.LocatorJreTest;
import com.google.gwt.requestfactory.server.RequestFactoryExceptionPropagationJreTest;
import com.google.gwt.requestfactory.server.RequestFactoryInterfaceValidatorTest;
import com.google.gwt.requestfactory.server.RequestFactoryJreTest;
import com.google.gwt.requestfactory.server.RequestFactoryUnicodeEscapingJreTest;
import com.google.gwt.requestfactory.shared.impl.SimpleEntityProxyIdTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite of RequestFactory tests that require the JRE.
 * <p>
 * Note: these tests require gwt-user src on the classpath. To run in
 * Eclipse, use Google Plugin for Eclipse to run as a GWT JUnit test
 * or edit the Eclipse launch config and add the src folder to the classpath
 * (click Classpath tab, User entries, Advanced..., Add folders)
 */
public class RequestFactoryJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite(
        "requestfactory package tests that require the JRE");
    suite.addTestSuite(BoxesAndPrimitivesJreTest.class);
    suite.addTestSuite(ComplexKeysJreTest.class);
    suite.addTestSuite(FindServiceJreTest.class);
    suite.addTestSuite(LocatorJreTest.class);
    suite.addTestSuite(RequestFactoryExceptionPropagationJreTest.class);
    suite.addTestSuite(RequestFactoryInterfaceValidatorTest.class);
    suite.addTestSuite(RequestFactoryJreTest.class);
    suite.addTestSuite(RequestFactoryModelTest.class);
    suite.addTestSuite(RequestFactoryUnicodeEscapingJreTest.class);
    suite.addTestSuite(SimpleEntityProxyIdTest.class);
    return suite;
  }
}
