package com.google.gwt.user;

import com.google.gwt.user.client.rpc.CollectionsTest;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTest;
import com.google.gwt.user.client.rpc.InheritanceTest;
import com.google.gwt.user.client.rpc.ObjectGraphTest;
import com.google.gwt.user.client.rpc.ValueTypesTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RPCSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("Test for com.google.gwt.user.client.rpc");
    
    suite.addTestSuite(ValueTypesTest.class);
    suite.addTestSuite(InheritanceTest.class);
    suite.addTestSuite(CollectionsTest.class);
    suite.addTestSuite(CustomFieldSerializerTest.class);
    suite.addTestSuite(ObjectGraphTest.class);
    
    return suite;
  }
}
