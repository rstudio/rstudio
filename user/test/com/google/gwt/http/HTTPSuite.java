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
package com.google.gwt.http;

import com.google.gwt.http.client.RequestBuilderTest;
import com.google.gwt.http.client.RequestTest;
import com.google.gwt.http.client.ResponseTest;
import com.google.gwt.http.client.URLTest;
import com.google.gwt.http.client.UrlBuilderTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * TODO: document me.
 */
@Deprecated
public class HTTPSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test for suite for the com.google.gwt.http module");

    suite.addTestSuite(URLTest.class);
    suite.addTestSuite(RequestBuilderTest.class);
    suite.addTestSuite(RequestTest.class);
    suite.addTestSuite(ResponseTest.class);
    suite.addTestSuite(UrlBuilderTest.class);

    return suite;
  }
}
