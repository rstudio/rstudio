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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.safehtml.rebind.ParsedHtmlTemplate.HtmlContext;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.StringReader;

/**
 * Tests for {@link HtmlTemplateParser}.
 */
//TODO(xtof): unit tests for parse failures
public final class HtmlTemplateParserTest extends TestCase {

  private TreeLogger logger;

  @Override
  public void setUp() {
    logger = new PrintWriterTreeLogger();
  }
  
  /*
   * We use the string representation of ParsedHtmlTemplate to express
   * expected results. The unit test for ParsedHtmlTemplate establishes that
   * this string representation accurately represents the structure of the
   * parsed template.
   */
  private void assertParseTemplateStringResult(String expected,
                                               String template) {
    HtmlTemplateParser parser = new HtmlTemplateParser(logger);
    HtmlContext ctx = new HtmlContext(HtmlContext.Type.UNDEFINED);
    parser.parseTemplateString(ctx, template);
    assertEquals(expected, parser.getParsedTemplate().toString());
  }

  public void testParseTemplateString() {
    assertParseTemplateStringResult("[]", "");
    assertParseTemplateStringResult("[L(foo)]", "foo");
    assertParseTemplateStringResult(
        "[L(foo), P((UNDEFINED,null,null),0), L(bar)]",
        "foo{0}bar");
    assertParseTemplateStringResult(
        "[L(foo), P((UNDEFINED,null,null),0), "
            + "P((UNDEFINED,null,null),1), L(bar)]",
        "foo{0}{1}bar");
    assertParseTemplateStringResult(
        "[L(foo), P((UNDEFINED,null,null),0), L(b), "
            + "P((UNDEFINED,null,null),1), L(bar)]",
        "foo{0}b{1}bar");
    assertParseTemplateStringResult(
        "[L(foo), P((UNDEFINED,null,null),0), L(baz), "
            + "P((UNDEFINED,null,null),1), L(bar)]",
        "foo{0}baz{1}bar");
    assertParseTemplateStringResult(
        "[P((UNDEFINED,null,null),0), L(foo), P((UNDEFINED,null,null),0), "
            + "P((UNDEFINED,null,null),1), L(bar)]",
        "{0}foo{0}{1}bar");
    assertParseTemplateStringResult(
        "[P((UNDEFINED,null,null),0), L(foo), P((UNDEFINED,null,null),0), "
            + "P((UNDEFINED,null,null),1), L(b)]",
        "{0}foo{0}{1}b");
    assertParseTemplateStringResult(
        "[L(foo), P((UNDEFINED,null,null),0), P((UNDEFINED,null,null),1), "
            + "L(bar), P((UNDEFINED,null,null),2)]",
        "foo{0}{1}bar{2}");
    assertParseTemplateStringResult(
        "[L(f), P((UNDEFINED,null,null),2), P((UNDEFINED,null,null),1), "
            + "L(bar), P((UNDEFINED,null,null),2)]",
        "f{2}{1}bar{2}");
    
    // Test degenerate cases with curly braces that don't match a parameter
    // pattern; these are treated as regular string literals.
    assertParseTemplateStringResult("[L(foo{)]", "foo{");
    assertParseTemplateStringResult("[L(}foo)]", "}foo");
    assertParseTemplateStringResult("[L(foo{text})]", "foo{text}");
  }

  private void assertParseTemplateStringResultWithAtStartContext(
      String expected, String template) {
    HtmlTemplateParser parser = new HtmlTemplateParser(logger);
    HtmlContext ctx = new HtmlContext(HtmlContext.Type.ATTRIBUTE, "img", "src");
    HtmlContext ctxAtStart =
        new HtmlContext(HtmlContext.Type.ATTRIBUTE_START, "img", "src");
    parser.parseTemplateString(ctx, ctxAtStart, template);
    assertEquals(expected, parser.getParsedTemplate().toString());
  }

  public void testParseTemplateStringWithAtStartContext() {
    assertParseTemplateStringResultWithAtStartContext(
        "[P((ATTRIBUTE_START,img,src),0)]",
        "{0}");
    assertParseTemplateStringResultWithAtStartContext(
        "[L(x), P((ATTRIBUTE,img,src),0)]",
        "x{0}");
    assertParseTemplateStringResultWithAtStartContext(
        "[L(boo), P((ATTRIBUTE,img,src),0)]",
        "boo{0}");
 
    assertParseTemplateStringResultWithAtStartContext(
        "[P((ATTRIBUTE_START,img,src),0), L(foo)]",
        "{0}foo");
    assertParseTemplateStringResultWithAtStartContext(
        "[P((ATTRIBUTE_START,img,src),0), P((ATTRIBUTE,img,src),1)]",
        "{0}{1}");
    assertParseTemplateStringResultWithAtStartContext(
        "[P((ATTRIBUTE_START,img,src),0), L(X), P((ATTRIBUTE,img,src),1)]",
        "{0}X{1}");
    assertParseTemplateStringResultWithAtStartContext(
        "[L(X), P((ATTRIBUTE,img,src),0), P((ATTRIBUTE,img,src),1)]",
        "X{0}{1}");
   }

  private void assertParseXHtmlResult(String expected, String template)
      throws UnableToCompleteException {
    HtmlTemplateParser parser = new HtmlTemplateParser(logger);
    parser.parseXHtml(new StringReader(template));
    assertEquals(expected, parser.getParsedTemplate().toString());
  }

  public void testParseXHtml() throws UnableToCompleteException {
    // Basic cases.
    assertParseXHtmlResult("[L(<b>foo</b>)]", "<b>foo</b>");
    assertParseXHtmlResult(
        "[L(<span>foo<b>), P((TEXT,null,null),0), L(</b></span>)]",
        "<span>foo<b>{0}</b></span>");
    assertParseXHtmlResult(
        "[L(<span>foo<b>), P((TEXT,null,null),1), L(</b>), "
            + "P((TEXT,null,null),0), L(</span>)]",
        "<span>foo<b>{1}</b>{0}</span>");

    // Check that tags and attributes are lower-cased, but inner text is not.
    assertParseXHtmlResult("[L(<b id=\"bAr\">fOo</b>)]",
                           "<B Id=\"bAr\">fOo</B>");

    // Verify correct handling/escaping of HTML metacharacters and
    // CDATA sections.
    assertParseXHtmlResult(
        "[L(<span>foo&amp;bar<b>), P((TEXT,null,null),1), "
            + "L(</b>foo-cdata &lt;baz&gt;), P((TEXT,null,null),0), "
            + "L(</span>)]",
        "<span>foo&amp;bar<b>{1}</b><![CDATA[foo-cdata <baz>]]>{0}</span>");

    // Check correct handling of ATTRIBUTE vs ATTRIBUTE_START context.
    assertParseXHtmlResult(
        "[L(<a href=\"), P((ATTRIBUTE_START,a,href),0), "
            + "L(\">), P((TEXT,null,null),1), L(</a>)]",
        "<a href=\"{0}\">{1}</a>");
    assertParseXHtmlResult(
        "[L(<a href=\"http://), P((ATTRIBUTE,a,href),0), "
            + "L(\">), P((TEXT,null,null),1), L(</a>)]",
        "<a href=\"http://{0}\">{1}</a>");
    assertParseXHtmlResult(
        "[L(<a href=\"), P((ATTRIBUTE_START,a,href),0), "
            + "L(/), P((ATTRIBUTE,a,href),1), "
            + "L(\">), P((TEXT,null,null),2), L(</a>)]",
        "<a href=\"{0}/{1}\">{2}</a>");

    // Verify correct escaping in attributes.
    assertParseXHtmlResult(
        "[L(<a href=\"http://...&amp;), "
            + "P((ATTRIBUTE,a,href),0), "
            + "L(=), P((ATTRIBUTE,a,href),1), "
            + "L(\">), P((TEXT,null,null),2), L(</a>)]",
        "<a href=\"http://...&amp;{0}={1}\">{2}</a>");
  }
}
