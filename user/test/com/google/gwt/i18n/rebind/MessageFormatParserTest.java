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
import com.google.gwt.i18n.rebind.MessageFormatParser.StaticArgChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.StringChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.TemplateChunk;

import junit.framework.TestCase;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link MessageFormatParser}.
 */
public class MessageFormatParserTest extends TestCase {

  public void testList() throws ParseException {
    String str = "{0,list:max=3,number:curcode=1:space,currency}";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    ArgumentChunk chunk = (ArgumentChunk) parsed.get(0);
    assertTrue(chunk.isList());
    assertEquals("number", chunk.getFormat());
    assertEquals("currency", chunk.getSubFormat());
    Map<String, String> args = chunk.getListArgs();
    assertEquals(1, args.size());
    assertEquals("3", args.get("max"));
    args = chunk.getFormatArgs();
    assertEquals(2, args.size());
    assertEquals("1", args.get("curcode"));
    assertEquals("", args.get("space"));

    str = "{0,list,number}";
    parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    chunk = (ArgumentChunk) parsed.get(0);
    assertTrue(chunk.isList());
    assertEquals("number", chunk.getFormat());
    assertNull(chunk.getSubFormat());
    args = chunk.getListArgs();
    assertEquals(0, args.size());
    args = chunk.getFormatArgs();
    assertEquals(0, args.size());
}

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
    // Note that the quoting will not necessarily be reproduced, only that the
    // returned result is functionally equivalent.  So, some of these strings
    // are carefully constructed to match the form which will be returned.
    String[] testStrings = new String[] {
      "Simple string literal",
      "{0}",
      "'{'0'}'",
      "Don''t tell me it''s broken",
      "'{'0'}' {1,list:max=3,a'{'0'}'=''b''}",
    };
    for (String str : testStrings) {
      List<TemplateChunk> parsed = MessageFormatParser.parse(str);
      String out = MessageFormatParser.assemble(parsed);
      assertEquals(str, out);
    }
  }

  public void testStaticArg() throws ParseException {
    String str = "{beginBold,<b>}bold{endBold,</b>}";
    List<TemplateChunk> parsed = MessageFormatParser.parse(str);
    assertEquals(3, parsed.size());
    StaticArgChunk staticArg = (StaticArgChunk) parsed.get(0);
    assertEquals("beginBold", staticArg.getArgName());
    assertEquals("<b>", staticArg.getReplacement());
    StringChunk stringChunk = (StringChunk) parsed.get(1);
    assertEquals("bold", stringChunk.getString());
    staticArg = (StaticArgChunk) parsed.get(2);
    assertEquals("endBold", staticArg.getArgName());
    assertEquals("</b>", staticArg.getReplacement());
    
    str = "{test,'{}'''}";
    parsed = MessageFormatParser.parse(str);
    assertEquals(1, parsed.size());
    staticArg = (StaticArgChunk) parsed.get(0);
    assertEquals("test", staticArg.getArgName());
    assertEquals("{}'", staticArg.getReplacement());
  }
}
