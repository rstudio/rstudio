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
package com.google.gwt.safehtml.rebind;

import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.HtmlContext;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.LiteralChunk;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.ParameterChunk;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.TemplateChunk;

import junit.framework.TestCase;

import java.util.Iterator;
import java.util.List;

/**
 * Tests for {@link ParsedHtmlTemplate}.
 *
 */
public final class ParsedHtmlTemplateTest extends TestCase {
  public void testAddLiteral() {
    ParsedHtmlTemplate parsed = new ParsedHtmlTemplate();
    parsed.addLiteral("<foo>");

    List<TemplateChunk> chunks = parsed.getChunks();
    LiteralChunk chunk = (LiteralChunk) chunks.get(0);
    assertEquals(TemplateChunk.Kind.LITERAL, chunk.getKind());
    assertEquals("<foo>", chunk.getLiteral());
    assertEquals("L(<foo>)", chunk.toString());

    assertEquals("[L(<foo>)]", parsed.toString());
  }

  public void testAddParameter() {
    ParsedHtmlTemplate parsed = new ParsedHtmlTemplate();
    parsed.addParameter(new ParameterChunk(new HtmlContext(
        HtmlContext.Type.TEXT), 0));

    List<TemplateChunk> chunks = parsed.getChunks();

    ParameterChunk chunk = (ParameterChunk) chunks.get(0);
    assertEquals(TemplateChunk.Kind.PARAMETER, chunk.getKind());
    assertEquals(HtmlContext.Type.TEXT, chunk.getContext().getType());
    assertNull(chunk.getContext().getTag());
    assertNull(chunk.getContext().getAttribute());
    assertEquals(0, chunk.getParameterIndex());
    assertEquals("P((TEXT,null,null),0)", chunk.toString());

    assertEquals("[P((TEXT,null,null),0)]", parsed.toString());
  }

  /**
   * Tests that calling addLiteral(), addParameter(), addLiteral() in sequence
   * results in the expected ParsedHtmlTemplate.
   */
  public void testAddLiteralAddParameterSequence() {
    ParsedHtmlTemplate parsed = new ParsedHtmlTemplate();

    parsed.addLiteral("<foo>");
    parsed.addParameter(new ParameterChunk(new HtmlContext(
        HtmlContext.Type.TEXT), 0));
    parsed.addLiteral("</foo>");

    List<TemplateChunk> chunks = parsed.getChunks();
    assertEquals(3, chunks.size());
    Iterator<TemplateChunk> it = chunks.iterator();

    LiteralChunk litChunk;
    ParameterChunk paramChunk;

    litChunk = (LiteralChunk) it.next();
    assertEquals(TemplateChunk.Kind.LITERAL, litChunk.getKind());
    assertEquals("<foo>", litChunk.getLiteral());
    assertEquals("L(<foo>)", litChunk.toString());

    paramChunk = (ParameterChunk) it.next();
    assertEquals(TemplateChunk.Kind.PARAMETER, paramChunk.getKind());
    assertEquals(HtmlContext.Type.TEXT, paramChunk.getContext().getType());
    assertNull(paramChunk.getContext().getTag());
    assertNull(paramChunk.getContext().getAttribute());
    assertEquals(0, paramChunk.getParameterIndex());
    assertEquals("P((TEXT,null,null),0)", paramChunk.toString());

    litChunk = (LiteralChunk) it.next();
    assertEquals(TemplateChunk.Kind.LITERAL, litChunk.getKind());
    assertEquals("</foo>", litChunk.getLiteral());
    assertEquals("L(</foo>)", litChunk.toString());

    // Assert that the string representation of the parsed template has the
    // expected format, to allow us to use the string representation in unit
    // tests for the template parser.
    assertEquals("[L(<foo>), P((TEXT,null,null),0), L(</foo>)]",
                 parsed.toString());
  }

  /**
   * Tests that calling addParameter(), addLiteral(), addLiteral(),
   * addParameter() in sequence results in the expected ParsedHtmlTemplate.
   *
   * <p>In particular, two calls to addLiteral() in sequence should result in
   * only a single LiteralChunk.
   */
  public void testAddParameterAddLiteralSequence() {
    ParsedHtmlTemplate parsed = new ParsedHtmlTemplate();

    parsed.addParameter(new ParameterChunk(new HtmlContext(
        HtmlContext.Type.TEXT), 0));
    parsed.addLiteral("<a");
    parsed.addLiteral(" href=\"");
    parsed.addParameter(new ParameterChunk(new HtmlContext(
        HtmlContext.Type.URL_ATTRIBUTE_START, "a", "href"), 1));

    List<TemplateChunk> chunks = parsed.getChunks();
    assertEquals(3, chunks.size());
    Iterator<TemplateChunk> it = chunks.iterator();

    LiteralChunk litChunk;
    ParameterChunk paramChunk;

    paramChunk = (ParameterChunk) it.next();
    assertEquals(TemplateChunk.Kind.PARAMETER, paramChunk.getKind());
    assertEquals(HtmlContext.Type.TEXT, paramChunk.getContext().getType());
    assertNull(paramChunk.getContext().getTag());
    assertNull(paramChunk.getContext().getAttribute());
    assertEquals(0, paramChunk.getParameterIndex());
    assertEquals("P((TEXT,null,null),0)", paramChunk.toString());

    litChunk = (LiteralChunk) it.next();
    assertEquals(TemplateChunk.Kind.LITERAL, litChunk.getKind());
    assertEquals("<a href=\"", litChunk.getLiteral());
    assertEquals("L(<a href=\")", litChunk.toString());

    paramChunk = (ParameterChunk) it.next();
    assertEquals(TemplateChunk.Kind.PARAMETER, paramChunk.getKind());
    assertEquals(
        HtmlContext.Type.URL_ATTRIBUTE_START, paramChunk.getContext().getType());
    assertEquals("a", paramChunk.getContext().getTag());
    assertEquals("href", paramChunk.getContext().getAttribute());
    assertEquals(1, paramChunk.getParameterIndex());
    assertEquals("P((URL_ATTRIBUTE_START,a,href),1)", paramChunk.toString());

    assertEquals(
        "[P((TEXT,null,null),0), L(<a href=\"), "
            + "P((URL_ATTRIBUTE_START,a,href),1)]",
        parsed.toString());
  }
}
