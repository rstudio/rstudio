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

import com.google.gwt.dom.builder.shared.GwtHtmlBuilderImplTest;
import com.google.gwt.dom.builder.shared.GwtHtmlDivBuilderTest;
import com.google.gwt.dom.builder.shared.GwtHtmlOptionBuilderTest;
import com.google.gwt.dom.builder.shared.GwtHtmlSelectBuilderTest;
import com.google.gwt.dom.builder.shared.GwtHtmlStylesBuilderTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the html implementation of element builders.
 */
public class HtmlBuilderGwtSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("GWT tests for all html builders");

    suite.addTestSuite(GwtHtmlBuilderImplTest.class);
    suite.addTestSuite(GwtHtmlDivBuilderTest.class);
    suite.addTestSuite(GwtHtmlOptionBuilderTest.class);
    suite.addTestSuite(GwtHtmlSelectBuilderTest.class);
    suite.addTestSuite(GwtHtmlStylesBuilderTest.class);

    return suite;
  }
}
