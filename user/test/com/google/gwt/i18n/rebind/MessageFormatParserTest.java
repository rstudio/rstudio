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
package com.google.gwt.i18n.rebind;

import com.google.gwt.i18n.rebind.MessageFormatParser.ArgumentChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.TemplateChunk;

import junit.framework.TestCase;

import java.text.ParseException;
import java.util.List;

/**
 * Test for {@link MessageFormatParser}
 */
public class MessageFormatParserTest extends TestCase {

  public void testParseLiteral() throws ParseException {
    String str = "Simple string literal";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    TemplateChunk chunk = parsed.get(0);
    assertTrue(chunk.isLiteral());
    assertEquals(str, chunk.getString());
  }

  public void testParseNestedQuoting() throws ParseException {
    String str = "'Don''t worry about nested quotes'";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    TemplateChunk chunk = parsed.get(0);
    assertTrue(chunk.isLiteral());
    assertEquals("Don't worry about nested quotes", chunk.getString());
  }

  public void testParseQuoting() throws ParseException {
    String str = "Don''t replace '{0}' or '{'0'}'";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    TemplateChunk chunk = parsed.get(0);
    assertTrue(chunk.isLiteral());
    assertEquals("Don't replace {0} or {0}", chunk.getString());
  }

  public void testParseSimple1() throws ParseException {
    String str = "{0}";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    TemplateChunk chunk = parsed.get(0);
    assertFalse(chunk.isLiteral());
    assertEquals("{0}", chunk.getString());
    ArgumentChunk argChunk = (ArgumentChunk) chunk;
    assertEquals(0, argChunk.getArgumentNumber());
    assertNull(argChunk.getFormat());
    assertNull(argChunk.getSubFormat());
  }

  public void testParseSimple2() throws ParseException {
    String str = "Message {0} has one arg, '{0}'";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(3, parsed.size());
    TemplateChunk chunk = parsed.get(0);
    assertTrue(chunk.isLiteral());
    assertEquals("Message ", chunk.getString());
    chunk = parsed.get(1);
    assertFalse(chunk.isLiteral());
    assertEquals("{0}", chunk.getString());
    ArgumentChunk argChunk = (ArgumentChunk) chunk;
    assertEquals(0, argChunk.getArgumentNumber());
    assertNull(argChunk.getFormat());
    assertNull(argChunk.getSubFormat());
    chunk = parsed.get(2);
    assertTrue(chunk.isLiteral());
    assertEquals(" has one arg, {0}", chunk.getString());
  }

  public void testRoundTrip() throws ParseException {
    // Note that we the quoting will not necessarily be reproduced, only
    // that the returned result is functionally equivalent.
    String[] testStrings = new String[] {
      "Simple string literal",
      "{0}",
      "'{'0'}'",
      "Don''t tell me it''s broken",
    };
    for (String str : testStrings) {
      List<TemplateChunk> parsed = MessageFormatParser.parse(str);
      String out = MessageFormatParser.assemble(parsed);
      assertEquals(str, out);
    }
  }
}
