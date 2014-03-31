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
package com.google.gwt.user;

import com.google.gwt.dev.BootStrapPlatform;
import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.rpc.client.RpcCollectionsTest;
import com.google.gwt.rpc.client.RpcCustomFieldSerializerTest;
import com.google.gwt.rpc.client.RpcEnumsTest;
import com.google.gwt.rpc.client.RpcInheritanceTest;
import com.google.gwt.rpc.client.RpcObjectGraphTest;
import com.google.gwt.rpc.client.RpcRemoteServiceServletTest;
import com.google.gwt.rpc.client.RpcRunTimeSerializationErrorsTest;
import com.google.gwt.rpc.client.RpcUnicodeEscapingTest;
import com.google.gwt.rpc.client.RpcValueTypesTest;
import com.google.gwt.user.client.rpc.CollectionsTest;
import com.google.gwt.user.client.rpc.CollectionsTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.CoreJavaTest;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTest;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.EnumsTest;
import com.google.gwt.user.client.rpc.EnumsTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.ExceptionsTest;
import com.google.gwt.user.client.rpc.FailedRequestTest;
import com.google.gwt.user.client.rpc.FailingRequestBuilderTest;
import com.google.gwt.user.client.rpc.InheritanceTest;
import com.google.gwt.user.client.rpc.InheritanceTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.ObjectGraphTest;
import com.google.gwt.user.client.rpc.ObjectGraphTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.RecursiveClassTest;
import com.google.gwt.user.client.rpc.RpcTokenTest;
import com.google.gwt.user.client.rpc.RpcTokenTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.RunTimeSerializationErrorsTest;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTest;
import com.google.gwt.user.client.rpc.UnicodeEscapingTest;
import com.google.gwt.user.client.rpc.UnicodeEscapingTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.ValueTypesTest;
import com.google.gwt.user.client.rpc.ValueTypesTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.XsrfProtectionTest;

import junit.framework.Test;

/**
 * A collection of TestCases for the RPC system.
 */
public class RPCSuite {

  static {
    /*
     * Required for OS X Leopard. This call ensures we have a valid context
     * ClassLoader. Many of the tests test low-level RPC mechanisms and rely on
     * a ClassLoader to resolve classes and resources.
     */
    BootStrapPlatform.applyPlatformHacks();
  }

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test for com.google.gwt.user.client.rpc");

    // Non GWTTestCases: see RpcJreSuite

    // GWTTestCases
    suite.addTestSuite(ValueTypesTest.class);
    suite.addTestSuite(EnumsTest.class);
    suite.addTestSuite(InheritanceTest.class);
    suite.addTestSuite(CollectionsTest.class);
    suite.addTestSuite(CoreJavaTest.class);
    suite.addTestSuite(CustomFieldSerializerTest.class);
    suite.addTestSuite(ExceptionsTest.class);
    suite.addTestSuite(ObjectGraphTest.class);
    suite.addTestSuite(com.google.gwt.user.client.rpc.RemoteServiceServletTest.class);
    suite.addTestSuite(RpcTokenTest.class);
    suite.addTestSuite(UnicodeEscapingTest.class);
    suite.addTestSuite(RunTimeSerializationErrorsTest.class);
    suite.addTestSuite(RecursiveClassTest.class);
    suite.addTestSuite(TypeCheckedObjectsTest.class);
    suite.addTestSuite(XsrfProtectionTest.class);
    suite.addTestSuite(FailedRequestTest.class);
    suite.addTestSuite(FailingRequestBuilderTest.class);

    // This test turns on the type-elision feature of RPC
    suite.addTestSuite(ValueTypesTestWithTypeObfuscation.class);
    suite.addTestSuite(EnumsTestWithTypeObfuscation.class);
    suite.addTestSuite(InheritanceTestWithTypeObfuscation.class);
    suite.addTestSuite(CollectionsTestWithTypeObfuscation.class);
    suite.addTestSuite(CustomFieldSerializerTestWithTypeObfuscation.class);
    suite.addTestSuite(ObjectGraphTestWithTypeObfuscation.class);
    suite.addTestSuite(
        com.google.gwt.user.client.rpc.RemoteServiceServletTestWithTypeObfuscation.class);
    suite.addTestSuite(UnicodeEscapingTestWithTypeObfuscation.class);
    suite.addTestSuite(RpcTokenTestWithTypeObfuscation.class);

    // Client-side test cases for deRPC system
    if (false) {
      // Disabled due to https://code.google.com/p/google-web-toolkit/issues/detail?id=8136
      suite.addTestSuite(RpcValueTypesTest.class);
      suite.addTestSuite(RpcEnumsTest.class);
      suite.addTestSuite(RpcInheritanceTest.class);
      suite.addTestSuite(RpcCollectionsTest.class);
      suite.addTestSuite(RpcCustomFieldSerializerTest.class);
      suite.addTestSuite(RpcObjectGraphTest.class);
      suite.addTestSuite(RpcRemoteServiceServletTest.class);
      suite.addTestSuite(RpcUnicodeEscapingTest.class);
      suite.addTestSuite(RpcRunTimeSerializationErrorsTest.class);
    }

    return suite;
  }
}
