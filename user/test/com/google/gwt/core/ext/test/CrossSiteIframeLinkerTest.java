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

package com.google.gwt.core.ext.test;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Integration test of the cross-site iframe linker.
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
// TODO(unnurg): Figure out why this test fails in HtmlUnit Dev Mode
public class CrossSiteIframeLinkerTest extends LinkerTest {
  @Override
  public String getModuleName() {
    return "com.google.gwt.core.ext.CrossSiteIframeLinkerTest";
  }
}
