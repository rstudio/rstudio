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
package com.google.gwt.resources;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.resources.client.CSSResourceTest;
import com.google.gwt.resources.client.DataResourceDoNotEmbedTest;
import com.google.gwt.resources.client.DataResourceMimeTypeTest;
import com.google.gwt.resources.client.ExternalTextResourceJsonpTest;
import com.google.gwt.resources.client.ExternalTextResourceTest;
import com.google.gwt.resources.client.ImageResourceNoInliningTest;
import com.google.gwt.resources.client.ImageResourceTest;
import com.google.gwt.resources.client.NestedBundleTest;
import com.google.gwt.resources.client.TextResourceTest;
import com.google.gwt.resources.css.CssExternalTest;
import com.google.gwt.resources.css.CssNodeClonerTest;
import com.google.gwt.resources.css.CssReorderTest;
import com.google.gwt.resources.css.CssRtlTest;
import com.google.gwt.resources.css.ExtractClassNamesVisitorTest;
import com.google.gwt.resources.css.UnknownAtRuleTest;
import com.google.gwt.resources.ext.ResourceGeneratorUtilTest;
import com.google.gwt.resources.rg.CssClassNamesTestCase;

import junit.framework.Test;

/**
 * Tests the ClientBundle framework.
 */
public class ResourcesSuite {
  public static Test suite() {

    GWTTestSuite suite = new GWTTestSuite("Test for com.google.gwt.resources");
    suite.addTestSuite(CssClassNamesTestCase.class);
    suite.addTestSuite(CssExternalTest.class);
    suite.addTestSuite(CssNodeClonerTest.class);
    suite.addTestSuite(CssReorderTest.class);
    suite.addTestSuite(CSSResourceTest.class);
    suite.addTestSuite(CssRtlTest.class);
    suite.addTestSuite(DataResourceDoNotEmbedTest.class);
    suite.addTestSuite(DataResourceMimeTypeTest.class);
    suite.addTestSuite(ExternalTextResourceJsonpTest.class);
    suite.addTestSuite(ExternalTextResourceTest.class);
    suite.addTestSuite(ExtractClassNamesVisitorTest.class);
    suite.addTestSuite(ImageResourceNoInliningTest.class);
    suite.addTestSuite(ImageResourceTest.class);
    suite.addTestSuite(NestedBundleTest.class);
    suite.addTestSuite(ResourceGeneratorUtilTest.class);
    suite.addTestSuite(TextResourceTest.class);
    suite.addTestSuite(UnknownAtRuleTest.class);
    return suite;
  }
}
