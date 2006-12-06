// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.http;

import com.google.gwt.http.client.RequestBuilderTest;
import com.google.gwt.http.client.RequestTest;
import com.google.gwt.http.client.ResponseTest;
import com.google.gwt.http.client.URLTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class HTTPSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite(
        "Test for suite for the com.google.gwt.http module");

    suite.addTestSuite(URLTest.class);
    suite.addTestSuite(RequestBuilderTest.class);
    suite.addTestSuite(RequestTest.class);
    suite.addTestSuite(ResponseTest.class);

    return suite;
  }
}
