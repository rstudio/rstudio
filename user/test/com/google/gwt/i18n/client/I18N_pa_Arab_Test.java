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
package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.impl.CldrImpl;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests Punjabi written in Arabic script.
 */
public class I18N_pa_Arab_Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_pa_Arab";
  }

  public void testCldrImpl() {
    CldrImpl cldr = GWT.create(CldrImpl.class);
    assertTrue(cldr.isRTL());
  }
}
