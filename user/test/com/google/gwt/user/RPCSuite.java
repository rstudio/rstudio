/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.client.rpc.CollectionsTest;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTest;
import com.google.gwt.user.client.rpc.EnumsTest;
import com.google.gwt.user.client.rpc.InheritanceTest;
import com.google.gwt.user.client.rpc.ObjectGraphTest;
import com.google.gwt.user.client.rpc.UnicodeEscapingTest;
import com.google.gwt.user.client.rpc.ValueTypesTest;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilderTest;
import com.google.gwt.user.server.rpc.RPCTest;
import com.google.gwt.user.server.rpc.SerializationPolicyLoaderTest;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicyTest;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicyTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A collection of TestCases for the RPC system.
 */
public class RPCSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("Test for com.google.gwt.user.client.rpc");

    suite.addTestSuite(SerializableTypeOracleBuilderTest.class);
    suite.addTestSuite(RPCTest.class);
    suite.addTestSuite(ValueTypesTest.class);
    suite.addTestSuite(EnumsTest.class);
    suite.addTestSuite(InheritanceTest.class);
    suite.addTestSuite(CollectionsTest.class);
    suite.addTestSuite(CustomFieldSerializerTest.class);
    suite.addTestSuite(ObjectGraphTest.class);
    suite.addTestSuite(com.google.gwt.user.client.rpc.RemoteServiceServletTest.class);
    suite.addTestSuite(com.google.gwt.user.server.rpc.RemoteServiceServletTest.class);
    suite.addTestSuite(UnicodeEscapingTest.class);
    suite.addTestSuite(LegacySerializationPolicyTest.class);
    suite.addTestSuite(StandardSerializationPolicyTest.class);
    suite.addTestSuite(SerializationPolicyLoaderTest.class);

    return suite;
  }
}
