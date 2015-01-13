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
import com.google.gwt.resources.client.CSSResourceWithGSSTest;
import com.google.gwt.resources.client.DataResourceDoNotEmbedTest;
import com.google.gwt.resources.client.DataResourceMimeTypeTest;
import com.google.gwt.resources.client.ExternalTextResourceJsonpTest;
import com.google.gwt.resources.client.ExternalTextResourceTest;
import com.google.gwt.resources.client.ImageResourceNoInliningTest;
import com.google.gwt.resources.client.ImageResourceTest;
import com.google.gwt.resources.client.NestedBundleTest;
import com.google.gwt.resources.client.TextResourceTest;
import com.google.gwt.resources.client.gss.AutoConversionTest;
import com.google.gwt.resources.client.gss.DebugObfuscationStyleTest;
import com.google.gwt.resources.client.gss.GssResourceTest;
import com.google.gwt.resources.client.gss.PrettyObfuscationStyleTest;
import com.google.gwt.resources.client.gss.StableNoTypeObfuscationStyleTest;
import com.google.gwt.resources.client.gss.StableObfuscationStyleTest;
import com.google.gwt.resources.client.gss.StableShortTypeObfuscationStyleTest;

import junit.framework.Test;

/**
 * Tests the ClientBundle framework.
 */
public class ResourcesGwtSuite {
  public static Test suite() {

    GWTTestSuite suite = new GWTTestSuite("Test for com.google.gwt.resources");
    suite.addTestSuite(DataResourceDoNotEmbedTest.class);
    suite.addTestSuite(DataResourceMimeTypeTest.class);
    suite.addTestSuite(ExternalTextResourceJsonpTest.class);
    suite.addTestSuite(ExternalTextResourceTest.class);
    suite.addTestSuite(ImageResourceNoInliningTest.class);
    suite.addTestSuite(ImageResourceTest.class);
    suite.addTestSuite(NestedBundleTest.class);
    suite.addTestSuite(TextResourceTest.class);
    suite.addTestSuite(CSSResourceTest.class);
    suite.addTestSuite(CSSResourceWithGSSTest.class);

    // GSS
    suite.addTestSuite(AutoConversionTest.class);
    suite.addTestSuite(GssResourceTest.class);
    suite.addTestSuite(DebugObfuscationStyleTest.class);
    suite.addTestSuite(PrettyObfuscationStyleTest.class);
    suite.addTestSuite(StableShortTypeObfuscationStyleTest.class);
    suite.addTestSuite(StableNoTypeObfuscationStyleTest.class);
    suite.addTestSuite(StableObfuscationStyleTest.class);
    return suite;
  }
}
