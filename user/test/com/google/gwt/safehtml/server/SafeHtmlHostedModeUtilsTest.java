/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.safehtml.server;

import com.google.gwt.safehtml.shared.GwtSafeHtmlHostedModeUtilsTest;
import com.google.gwt.safehtml.shared.SafeHtmlHostedModeUtils;

/**
 * JUnit tests for {@link SafeHtmlHostedModeUtils}.
 */
public class SafeHtmlHostedModeUtilsTest
    extends GwtSafeHtmlHostedModeUtilsTest {

  // This forces a GWTTestCase to run as a vanilla JUnit TestCase.
  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    // Since we can't assume assertions are enabled, force
    // SafeHtmlHostedModeUtils#maybeCheckCompleteHtml to perform its check
    // when running in JRE.
    System.setProperty(
        SafeHtmlHostedModeUtils.FORCE_CHECK_COMPLETE_HTML, "true");
    SafeHtmlHostedModeUtils.setForceCheckCompleteHtmlFromProperty();
  }
}
