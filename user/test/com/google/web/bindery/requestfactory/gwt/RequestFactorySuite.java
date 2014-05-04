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
package com.google.web.bindery.requestfactory.gwt;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.web.bindery.requestfactory.gwt.client.FindServiceTest;
import com.google.web.bindery.requestfactory.gwt.client.JsonRpcRequestFactoryTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestBatcherTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryChainedContextTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryExceptionHandlerTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryExceptionPropagationTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryPolymorphicTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryTest;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryUnicodeEscapingTest;
import com.google.web.bindery.requestfactory.gwt.client.ui.EditorTest;
import com.google.web.bindery.requestfactory.shared.BoxesAndPrimitivesTest;
import com.google.web.bindery.requestfactory.shared.ComplexKeysTest;
import com.google.web.bindery.requestfactory.shared.FanoutReceiverTest;
import com.google.web.bindery.requestfactory.shared.LocatorTest;
import com.google.web.bindery.requestfactory.shared.MethodProvidedByServiceLayerTest;
import com.google.web.bindery.requestfactory.shared.MultipleFactoriesTest;
import com.google.web.bindery.requestfactory.shared.ServiceInheritanceTest;
import com.google.web.bindery.requestfactory.shared.impl.RequestPayloadTest;

import junit.framework.Test;

/**
 * Tests of RequestFactory that require GWT.
 */
public class RequestFactorySuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test suite for requestfactory gwt code.");
    suite.addTestSuite(RequestBatcherTest.class);
    suite.addTestSuite(BoxesAndPrimitivesTest.class);
    suite.addTestSuite(ComplexKeysTest.class);
    suite.addTestSuite(EditorTest.class);
    suite.addTestSuite(FanoutReceiverTest.class);
    suite.addTestSuite(FindServiceTest.class);
    suite.addTestSuite(JsonRpcRequestFactoryTest.class);
    suite.addTestSuite(LocatorTest.class);
    suite.addTestSuite(MethodProvidedByServiceLayerTest.class);
    suite.addTestSuite(MultipleFactoriesTest.class);
    suite.addTestSuite(RequestFactoryTest.class);
    suite.addTestSuite(RequestFactoryChainedContextTest.class);
    suite.addTestSuite(RequestFactoryExceptionHandlerTest.class);
    suite.addTestSuite(RequestFactoryExceptionPropagationTest.class);
    suite.addTestSuite(RequestFactoryPolymorphicTest.class);
    suite.addTestSuite(RequestFactoryUnicodeEscapingTest.class);
    suite.addTestSuite(RequestPayloadTest.class);
    suite.addTestSuite(ServiceInheritanceTest.class);
    return suite;
  }
}
