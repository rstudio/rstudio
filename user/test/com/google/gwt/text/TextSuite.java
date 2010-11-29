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
package com.google.gwt.text;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.text.client.DateTimeFormatRendererTest;
import com.google.gwt.text.client.DoubleParserTest;
import com.google.gwt.text.client.IntegerParserTest;
import com.google.gwt.text.client.LongParserTest;
import com.google.gwt.text.client.NumberFormatRendererTest;

import junit.framework.Test;

/**
 * Tests of com.google.gwt.text.
 */
public class TextSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Tests of com.google.gwt.text");
    suite.addTestSuite(DateTimeFormatRendererTest.class);
    suite.addTestSuite(DoubleParserTest.class);
    suite.addTestSuite(IntegerParserTest.class);
    suite.addTestSuite(LongParserTest.class);
    suite.addTestSuite(NumberFormatRendererTest.class);
    return suite;
  }
}
