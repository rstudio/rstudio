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
package com.google.gwt.text.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.text.ParseException;

/**
 * Eponymous unit test.
 */
public class DoubleParserTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.text.TextSuite";
  }
  
  public void testOuroborus() throws ParseException {
    assertEquals("123.5", DoubleRenderer.instance().render(DoubleParser.instance().parse("123.5")));
  }
  
  public void testNull() throws ParseException {
    assertEquals("", DoubleRenderer.instance().render(DoubleParser.instance().parse("")));
  }
}
