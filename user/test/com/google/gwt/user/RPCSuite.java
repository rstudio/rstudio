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
import com.google.gwt.user.client.rpc.RunTimeSerializationErrorsTest;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTest;
import com.google.gwt.user.client.rpc.UnicodeEscapingTest;
import com.google.gwt.user.client.rpc.UnicodeEscapingTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.ValueTypesTest;
import com.google.gwt.user.client.rpc.ValueTypesTestWithTypeObfuscation;
import com.google.gwt.user.client.rpc.XsrfProtectionTest;
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReaderTest;
import com.google.gwt.user.rebind.rpc.BlacklistTypeFilterTest;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilderTest;
import com.google.gwt.user.rebind.rpc.TypeHierarchyUtilsTest;
import com.google.gwt.user.server.Base64Test;
import com.google.gwt.user.server.UtilTest;
import com.google.gwt.user.server.rpc.AbstractXsrfProtectedServiceServletTest;
import com.google.gwt.user.server.rpc.RPCRequestTest;
import com.google.gwt.user.server.rpc.RPCServletUtilsTest;
import com.google.gwt.user.server.rpc.RPCTest;
import com.google.gwt.user.server.rpc.SerializationPolicyLoaderTest;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicyTest;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriterTest;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicyTest;

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

    // Non GWTTestCases
    suite.addTestSuite(BlacklistTypeFilterTest.class);
    suite.addTestSuite(SerializableTypeOracleBuilderTest.class);
    suite.addTestSuite(TypeHierarchyUtilsTest.class);
    suite.addTestSuite(RPCTest.class);
    suite.addTestSuite(com.google.gwt.user.server.rpc.RemoteServiceServletTest.class);
    suite.addTestSuite(LegacySerializationPolicyTest.class);
    suite.addTestSuite(StandardSerializationPolicyTest.class);
    suite.addTestSuite(SerializationPolicyLoaderTest.class);
    suite.addTestSuite(RPCServletUtilsTest.class);
    suite.addTestSuite(RPCRequestTest.class);
    suite.addTestSuite(FailedRequestTest.class);
    suite.addTestSuite(FailingRequestBuilderTest.class);
    suite.addTestSuite(Base64Test.class);
    suite.addTestSuite(UtilTest.class);
    suite.addTestSuite(AbstractXsrfProtectedServiceServletTest.class);
    suite.addTestSuite(ClientSerializationStreamReaderTest.class);
    suite.addTestSuite(ServerSerializationStreamWriterTest.class);

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

    // Client-side test cases for deRPC system
    suite.addTestSuite(RpcValueTypesTest.class);
    suite.addTestSuite(RpcEnumsTest.class);
    suite.addTestSuite(RpcInheritanceTest.class);
    suite.addTestSuite(RpcCollectionsTest.class);
    suite.addTestSuite(RpcCustomFieldSerializerTest.class);
    suite.addTestSuite(RpcObjectGraphTest.class);
    suite.addTestSuite(RpcRemoteServiceServletTest.class);
    suite.addTestSuite(RpcUnicodeEscapingTest.class);
    suite.addTestSuite(RpcRunTimeSerializationErrorsTest.class);
    return suite;
  }
}
