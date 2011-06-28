/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.builder;

import com.google.gwt.dom.builder.client.GwtDomBuilderImplTest;
import com.google.gwt.dom.builder.client.GwtDomDivBuilderTest;
import com.google.gwt.dom.builder.client.GwtDomOptionBuilderTest;
import com.google.gwt.dom.builder.client.GwtDomSelectBuilderTest;
import com.google.gwt.dom.builder.client.GwtDomStylesBuilderTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the dom implementation of element builders.
 */
public class DomBuilderGwtSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("GWT tests for all dom builders");

    suite.addTestSuite(GwtDomBuilderImplTest.class);
    suite.addTestSuite(GwtDomDivBuilderTest.class);
    suite.addTestSuite(GwtDomOptionBuilderTest.class);
    suite.addTestSuite(GwtDomSelectBuilderTest.class);
    suite.addTestSuite(GwtDomStylesBuilderTest.class);

    return suite;
  }
}
