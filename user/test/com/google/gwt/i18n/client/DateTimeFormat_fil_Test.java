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
package com.google.gwt.i18n.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

/**
 * Tests unique functionality in {@link DateTimeFormat} for the Filipino
 * language.
 */
@SuppressWarnings("deprecation")
public class DateTimeFormat_fil_Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_fil";
  }

  public void test_ccc() {
    Date date = new Date(2006 - 1900, 6, 26, 13, 10, 10);
    assertEquals("Miy", DateTimeFormat.getFormat("ccc").format(date));
  }

  public void test_cccc() {
    Date date = new Date(2006 - 1900, 6, 26, 13, 10, 10);
    assertEquals("Miyerkules", DateTimeFormat.getFormat("cccc").format(date));
  }

  public void test_ccccc() {
    Date date = new Date(2006 - 1900, 6, 26, 13, 10, 10);
    assertEquals("M", DateTimeFormat.getFormat("ccccc").format(date));
  }

  public void test_EEE() {
    Date date = new Date(2006 - 1900, 6, 26, 13, 10, 10);
    assertEquals("Mye", DateTimeFormat.getFormat("EEE").format(date));
  }

  public void test_EEEE() {
    Date date = new Date(2006 - 1900, 6, 26, 13, 10, 10);
    assertEquals("Miyerkules", DateTimeFormat.getFormat("EEEE").format(date));
  }

  public void test_EEEEE() {
    Date date = new Date(2006 - 1900, 6, 26, 13, 10, 10);
    assertEquals("M", DateTimeFormat.getFormat("EEEEE").format(date));
  }
}
