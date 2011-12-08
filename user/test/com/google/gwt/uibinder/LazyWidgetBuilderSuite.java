/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.uibinder.test.client.CellPanelParserIntegrationTest;
import com.google.gwt.uibinder.test.client.IsRenderableIntegrationTest;
import com.google.gwt.uibinder.test.client.LazyPanelParserIntegrationTest;
import com.google.gwt.uibinder.test.client.LazyWidgetBuilderSafeUriIntegrationTest;
import com.google.gwt.uibinder.test.client.SafeHtmlAsComponentsTest;
import com.google.gwt.uibinder.test.client.UiBinderParserUiWithAttributesTest;
import com.google.gwt.uibinder.test.client.UiProvidedNullTest;
import com.google.gwt.uibinder.test.client.UiRendererEventsTest;
import com.google.gwt.uibinder.test.client.UiRendererTest;

import junit.framework.Test;

/**
 * Test suite for UiBinder GWTTestCases.
 */
public class LazyWidgetBuilderSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Tests that rely on the useLazyWidgetBuilders switch");

    suite.addTestSuite(CellPanelParserIntegrationTest.class);
    suite.addTestSuite(IsRenderableIntegrationTest.class);
    suite.addTestSuite(LazyPanelParserIntegrationTest.class);
    suite.addTestSuite(LazyWidgetBuilderSafeUriIntegrationTest.class);
    suite.addTestSuite(SafeHtmlAsComponentsTest.class);
    suite.addTestSuite(UiBinderParserUiWithAttributesTest.class);
    suite.addTestSuite(UiProvidedNullTest.class);
    suite.addTestSuite(UiRendererTest.class);
    suite.addTestSuite(UiRendererEventsTest.class);

    return suite;
  }

  private LazyWidgetBuilderSuite() {
  }
}
