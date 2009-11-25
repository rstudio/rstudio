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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.uibinder.attributeparsers.StringAttributeParser.FieldReferenceDelegate;

import junit.framework.TestCase;

/**
 * Tests StringAttributeParser. 
 */
public class StringAttributeParserTest extends TestCase {
  FieldReferenceConverter converter = new FieldReferenceConverter(null);

  public void testSimpleParse() {
    String before = "snot";
    String expected = "\"snot\"";
    assertEquals(expected, converter.convert(before, new FieldReferenceDelegate(null)));
  }
  
  public void testParseEmpty() {
    String before = "";
    String expected = "\"\"";
    assertEquals(expected, converter.convert(before, new FieldReferenceDelegate(null)));
  }
  
  public void testSimpleFieldRef() {
    String before = "{able.baker.charlie.prawns}";
    String expected = "\"\" + able.baker().charlie().prawns() + \"\"";
    assertEquals(expected, converter.convert(before, new FieldReferenceDelegate(null)));
  }

  public void testBraceEscaping() {
    String before = "{able.baker.charlie} \"Howdy\nfriend\" {prawns.are.yummy}";
    String expected = "\"\" + able.baker().charlie() + \" \\\"Howdy\\nfriend\\\" \" + prawns.are().yummy() + \"\"";
    String after = converter.convert(before, new FieldReferenceDelegate(null));
    assertEquals(expected, after);
  }
}
