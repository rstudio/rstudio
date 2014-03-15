/*
 * Copyright 2012 Google Inc.
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
import com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReaderTest;
import com.google.gwt.user.rebind.rpc.BlacklistTypeFilterTest;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilderTest;
import com.google.gwt.user.rebind.rpc.SerializationUtilsTest;
import com.google.gwt.user.rebind.rpc.TypeHierarchyUtilsTest;
import com.google.gwt.user.server.Base64Test;
import com.google.gwt.user.server.UtilTest;
import com.google.gwt.user.server.rpc.AbstractXsrfProtectedServiceServletTest;
import com.google.gwt.user.server.rpc.DequeMapTest;
import com.google.gwt.user.server.rpc.RPCRequestTest;
import com.google.gwt.user.server.rpc.RPCServletUtilsTest;
import com.google.gwt.user.server.rpc.RPCTest;
import com.google.gwt.user.server.rpc.RPCTypeCheckTest;
import com.google.gwt.user.server.rpc.RemoteServiceServletTest;
import com.google.gwt.user.server.rpc.SerializationPolicyLoaderTest;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicyTest;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriterTest;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicyTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * RPC tests that don't extend GWTTestCase.
 */
public class RpcJreSuite {

  static {
    /*
     * Required for OS X Leopard. This call ensures we have a valid context
     * ClassLoader. Many of the tests test low-level RPC mechanisms and rely on
     * a ClassLoader to resolve classes and resources.
     */
    BootStrapPlatform.applyPlatformHacks();
  }

  public static Test suite() {
    TestSuite suite = new TestSuite("Non-browser tests for com.google.gwt.user.client.rpc");
    suite.addTestSuite(BlacklistTypeFilterTest.class);
    suite.addTestSuite(DequeMapTest.class);
    suite.addTestSuite(SerializationUtilsTest.class);
    suite.addTestSuite(SerializableTypeOracleBuilderTest.class);
    suite.addTestSuite(TypeHierarchyUtilsTest.class);
    suite.addTestSuite(RPCTest.class);
    suite.addTestSuite(RPCTypeCheckTest.class);
    suite.addTestSuite(RemoteServiceServletTest.class);
    suite.addTestSuite(LegacySerializationPolicyTest.class);
    suite.addTestSuite(StandardSerializationPolicyTest.class);
    suite.addTestSuite(SerializationPolicyLoaderTest.class);
    suite.addTestSuite(RPCServletUtilsTest.class);
    suite.addTestSuite(RPCRequestTest.class);
    suite.addTestSuite(Base64Test.class);
    suite.addTestSuite(UtilTest.class);
    suite.addTestSuite(AbstractXsrfProtectedServiceServletTest.class);
    suite.addTestSuite(ClientSerializationStreamReaderTest.class);
    suite.addTestSuite(ServerSerializationStreamWriterTest.class);
    return suite;
  }
}
