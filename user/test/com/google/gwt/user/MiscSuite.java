/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.user;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.layout.client.LayoutTest;
import com.google.gwt.user.client.CommandExecutorTest;
import com.google.gwt.user.client.CookieTest;
import com.google.gwt.user.client.DoubleClickEventSinkTest;
import com.google.gwt.user.client.DragAndDropEventsSinkTest;
import com.google.gwt.user.client.EventTest;
import com.google.gwt.user.client.GestureEventSinkTest;
import com.google.gwt.user.client.TimerTest;
import com.google.gwt.user.client.TouchEventSinkTest;
import com.google.gwt.user.client.WindowTest;
import com.google.gwt.user.datepicker.client.CalendarUtilTest;
import com.google.gwt.user.datepicker.client.DateChangeEventTest;
import com.google.gwt.user.rebind.ui.ImageBundleGeneratorTest;
import com.google.gwt.xml.client.XMLTest;

import junit.framework.Test;

/**
 * Various tests split out from UISuite because they're not in gwt.client.ui.
 */
public class MiscSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Miscellaneous GWT tests");
    suite.addTestSuite(CalendarUtilTest.class);
    suite.addTestSuite(CommandExecutorTest.class);
    suite.addTestSuite(CookieTest.class);
    suite.addTestSuite(DateChangeEventTest.class);
    suite.addTestSuite(DoubleClickEventSinkTest.class);
    suite.addTestSuite(DragAndDropEventsSinkTest.class);
    suite.addTestSuite(EventTest.class);
    suite.addTestSuite(GestureEventSinkTest.class);
    suite.addTestSuite(ImageBundleGeneratorTest.class);
    suite.addTestSuite(LayoutTest.class);
    suite.addTestSuite(TimerTest.class);
    suite.addTestSuite(TouchEventSinkTest.class);
    suite.addTestSuite(WindowTest.class);
    suite.addTestSuite(XMLTest.class);
    return suite;
  }
}
