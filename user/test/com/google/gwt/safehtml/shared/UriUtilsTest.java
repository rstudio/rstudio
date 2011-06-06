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
package com.google.gwt.safehtml.shared;

/**
 * Unit tests for {@link UriUtils}.
 */
public class UriUtilsTest extends GwtUriUtilsTest {

  // This forces a GWTTestCase to run as a vanilla JUnit TestCase.
  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    // Since we can't assume assertions are enabled, force
    // UriUtilsImpl#maybeCheckValidUri to perform its check
    // when running in JRE.
    SafeUriHostedModeUtils.setForceCheckValidUri(true);
  }
}
