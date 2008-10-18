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
import com.google.gwt.i18n.client.constants.TimeZoneConstants;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

/**
 * Tests TimeZone facilities.
 */
public class TimeZoneTest  extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.TimeZoneTest";
  }

  public void testGetters() {
    final TimeZoneConstants timeZoneConstants = GWT.create(TimeZoneConstants.class);
    TimeZone usPacific =
      TimeZone.createTimeZone(TimeZoneInfo.buildTimeZoneData(timeZoneConstants.americaLosAngeles()));
    
    assertEquals(480, usPacific.getStandardOffset());
    assertEquals("America/Los_Angeles", usPacific.getID());
  }

  public void testIsDaylightTime() {
    // All times in this test are in Los Angeles.
    // We especially want to test time around daylight time switch.
    // In 2007, the switch in Pacific time zone happens in 3/11 and 11/4.
    // We create the dates with fmt.parse because this allows us to specify
    // the time zone to which they pertain -- trying to create them by
    // directly manipulating Date objects was making our tests fail when run in
    // timezones outside of California.
    
    TimeZoneConstants timeZoneData = GWT.create(TimeZoneConstants.class);
    String str = timeZoneData.americaLosAngeles();
    TimeZone usPacific = TimeZone.createTimeZone(TimeZoneInfo.buildTimeZoneData(str));
    
    DateTimeFormat fmt = DateTimeFormat.getLongDateTimeFormat(); 
    Date dateMarchBefore = fmt.parse("March 11, 2007 01:00:00 AM GMT-800");
    Date dateMarchAfter = fmt.parse("March 11, 2007 03:01:00 AM GMT-800");
    Date dateJuly = fmt.parse("July 11, 2007 07:00:00 AM GMT-800");
    
    Date dateNovemberBefore = fmt.parse("November 4, 2007 00:00:00 AM GMT-800");
    Date dateNovemberAfter = fmt.parse("November 4, 2007 02:00:00 AM GMT-800");
        
    assertTrue("July should be DST.", usPacific.isDaylightTime(dateJuly));
    assertTrue("Late March should be DST", usPacific.isDaylightTime(dateMarchAfter));
    assertTrue("Early March should not be DST", !usPacific.isDaylightTime(dateMarchBefore));
    
    assertTrue("Late November should not be DST", !usPacific.isDaylightTime(dateNovemberAfter));
    assertTrue("Early November should be DST", usPacific.isDaylightTime(dateNovemberBefore));
  }
  
  public void testNames() {
    final TimeZoneConstants timeZoneConstants = GWT.create(TimeZoneConstants.class);
    TimeZone usPacific = TimeZone.createTimeZone(
        TimeZoneInfo.buildTimeZoneData(timeZoneConstants.americaLosAngeles()));
    Date dt = new Date(2007 - 1900, 7 - 1, 1);
    assertTrue(usPacific.isDaylightTime(dt));
    assertEquals("PDT", usPacific.getShortName(dt));
    assertEquals("Pacific Daylight Time", usPacific.getLongName(dt));
    
    dt = new Date(2007 - 1900, 12 - 1, 1);
    assertTrue(!usPacific.isDaylightTime(dt));
    assertEquals("PST", usPacific.getShortName(dt));
    assertEquals("Pacific Standard Time", usPacific.getLongName(dt));
  }
  
  public void testSimpleTimeZoneNegative() {
    Date date = new Date();
    TimeZone simpleTimeZone = TimeZone.createTimeZone(-480);
    assertEquals(-480, simpleTimeZone.getOffset(date));
    assertEquals(-480, simpleTimeZone.getStandardOffset());
    assertEquals("GMT+08:00", simpleTimeZone.getGMTString(date));
    assertEquals("Etc/GMT-8", simpleTimeZone.getID());
    assertEquals("UTC+8", simpleTimeZone.getLongName(date));
    assertEquals("UTC+8", simpleTimeZone.getShortName(date));
    assertEquals("+0800", simpleTimeZone.getRFCTimeZoneString(date));
    assertEquals(false, simpleTimeZone.isDaylightTime(date));
  }

  public void testSimpleTimeZonePositive() {
    Date date = new Date();
    TimeZone simpleTimeZone = TimeZone.createTimeZone(480);
    assertEquals(480, simpleTimeZone.getOffset(date));
    assertEquals(480, simpleTimeZone.getStandardOffset());
    assertEquals("GMT-08:00", simpleTimeZone.getGMTString(date));
    assertEquals("Etc/GMT+8", simpleTimeZone.getID());
    assertEquals("UTC-8", simpleTimeZone.getLongName(date));
    assertEquals("UTC-8", simpleTimeZone.getShortName(date));
    assertEquals("-0800", simpleTimeZone.getRFCTimeZoneString(date));
    assertEquals(false, simpleTimeZone.isDaylightTime(date));

    // check half-hour time zone offset.
    simpleTimeZone = TimeZone.createTimeZone(630);
    assertEquals(630, simpleTimeZone.getOffset(date));
    assertEquals(630, simpleTimeZone.getStandardOffset());
    assertEquals("GMT-10:30", simpleTimeZone.getGMTString(date));
    assertEquals("Etc/GMT+10:30", simpleTimeZone.getID());
    assertEquals("UTC-10:30", simpleTimeZone.getLongName(date));
    assertEquals("UTC-10:30", simpleTimeZone.getShortName(date));
    assertEquals("-1030", simpleTimeZone.getRFCTimeZoneString(date));
    assertEquals(false, simpleTimeZone.isDaylightTime(date));
  }

  public void testSimpleTimeZoneZero() {
    Date date = new Date();
    TimeZone simpleTimeZone = TimeZone.createTimeZone(0);
    assertEquals(0, simpleTimeZone.getOffset(date));
    assertEquals(0, simpleTimeZone.getStandardOffset());
    assertEquals("GMT+00:00", simpleTimeZone.getGMTString(date));
    assertEquals("Etc/GMT", simpleTimeZone.getID());
    assertEquals("UTC", simpleTimeZone.getLongName(date));
    assertEquals("UTC", simpleTimeZone.getShortName(date));
    assertEquals("+0000", simpleTimeZone.getRFCTimeZoneString(date));
    assertEquals(false, simpleTimeZone.isDaylightTime(date));
  }

}
