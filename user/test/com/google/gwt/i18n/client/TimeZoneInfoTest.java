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

package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.constants.TimeZoneConstants;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test the overlay type TimeZoneData. We get our JSON from TimeZoneConstants.
 */
public class TimeZoneInfoTest extends GWTTestCase {
  private TimeZoneInfo mawson;
  private TimeZoneInfo anchorage;
  private TimeZoneInfo madrid;

  @Override
  protected void gwtSetUp() throws Exception {
    TimeZoneConstants timeZoneConstants = GWT.create(TimeZoneConstants.class);
    mawson = TimeZoneInfo.buildTimeZoneData(timeZoneConstants.antarcticaMawson());
    anchorage = TimeZoneInfo.buildTimeZoneData(timeZoneConstants.americaAnchorage());
    madrid = TimeZoneInfo.buildTimeZoneData(timeZoneConstants.europeMadrid());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.TimeZoneTest";
  }

  public void testGetID() {
    assertEquals("Antarctica/Mawson", mawson.getID());
    assertEquals("America/Anchorage", anchorage.getID());
  }

  public void testGetNames() {
    JsArrayString mawsonNames = mawson.getNames();
    JsArrayString anchorageNames = anchorage.getNames();

    assertEquals(2, mawsonNames.length());
    assertEquals(4, anchorageNames.length());
  }

  public void testGetStandardOffset() {
    assertEquals(60, madrid.getStandardOffset());
    assertEquals(300, mawson.getStandardOffset());
    assertEquals(-540, anchorage.getStandardOffset());
  }
}
