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
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

/**
 * Tests for {@link HtmlTemplateParser}.
 */
public final class HtmlTemplateParserTest extends TestCase {

  private TreeLogger logger;

  @Override
  public void setUp() {
    logger = new PrintWriterTreeLogger();
  }

  /*
   * We use the string representation of ParsedHtmlTemplate to express expected
   * results. The unit test for ParsedHtmlTemplate establishes that this string
   * representation accurately represents the structure of the parsed template.
   */
  private void assertParseTemplateResult(String expected, String template)
      throws UnableToCompleteException {
    HtmlTemplateParser parser = new HtmlTemplateParser(logger);
    parser.parseTemplate(template);
    assertEquals(expected, parser.getParsedTemplate().toString());
  }

  public void testParseTemplate_noMarkup() throws UnableToCompleteException {
    assertParseTemplateResult("[]", "");
    assertParseTemplateResult("[L(foo)]", "foo");
    assertParseTemplateResult(
        "[L(foo), P((TEXT,null,null),0), L(bar)]",
        "foo{0}bar");
    assertParseTemplateResult(
        "[L(foo), P((TEXT,null,null),0), "
            + "P((TEXT,null,null),1), L(bar)]",
        "foo{0}{1}bar");
    assertParseTemplateResult(
        "[L(foo), P((TEXT,null,null),0), L(b), "
            + "P((TEXT,null,null),1), L(bar)]",
        "foo{0}b{1}bar");
    assertParseTemplateResult(
        "[L(foo), P((TEXT,null,null),0), L(baz), "
            + "P((TEXT,null,null),1), L(bar)]",
        "foo{0}baz{1}bar");
    assertParseTemplateResult(
        "[P((TEXT,null,null),0), L(foo), P((TEXT,null,null),0), "
            + "P((TEXT,null,null),1), L(bar)]",
        "{0}foo{0}{1}bar");
    assertParseTemplateResult(
        "[P((TEXT,null,null),0), L(foo), P((TEXT,null,null),0), "
            + "P((TEXT,null,null),1), L(b)]",
        "{0}foo{0}{1}b");
    assertParseTemplateResult(
        "[L(foo), P((TEXT,null,null),0), P((TEXT,null,null),1), "
            + "L(bar), P((TEXT,null,null),2)]",
        "foo{0}{1}bar{2}");
    assertParseTemplateResult(
        "[L(f), P((TEXT,null,null),2), P((TEXT,null,null),1), "
            + "L(bar), P((TEXT,null,null),2)]",
        "f{2}{1}bar{2}");

    // Test degenerate cases with curly braces that don't match a parameter
    // pattern; these are treated as regular string literals.
    assertParseTemplateResult("[L(foo{)]", "foo{");
    assertParseTemplateResult("[L(}foo)]", "}foo");
    assertParseTemplateResult("[L(foo{text})]", "foo{text}");
  }

  public void testParseTemplate_withMarkup() throws UnableToCompleteException {
    // Basic cases.
    assertParseTemplateResult("[L(<b>foo</b>)]", "<b>foo</b>");
    assertParseTemplateResult(
        "[L(<span>foo<b>), P((TEXT,null,null),0), L(</b></span>)]",
        "<span>foo<b>{0}</b></span>");
    assertParseTemplateResult(
        ("[L(<span>foo<b>), P((TEXT,null,null),1), L(</b>), "
            + "P((TEXT,null,null),0), L(</span>)]"),
        "<span>foo<b>{1}</b>{0}</span>");

    // Check that case is not modified.
    assertParseTemplateResult(
        "[L(<B Id=\"bAr\">fOo</B>)]", "<B Id=\"bAr\">fOo</B>");

    // Verify correct handling/escaping of HTML metacharacters and
    // CDATA sections.
    assertParseTemplateResult(
        ("[L(<span>foo&amp;bar<b>), P((TEXT,null,null),1), "
            + "L(</b><![CDATA[foo-cdata <baz>]]>), P((TEXT,null,null),0), "
            + "L(</span>)]"),
        "<span>foo&amp;bar<b>{1}</b><![CDATA[foo-cdata <baz>]]>{0}</span>");

    // Check correct handling of ATTRIBUTE_VALUE vs URL_ATTRIBUTE_START and
    // URL_ATTRIBUTE_ENTIRE context.
    assertParseTemplateResult(("[L(<a href=\"), P((URL_ATTRIBUTE_ENTIRE,a,href),0), "
        + "L(\">), P((TEXT,null,null),1), L(</a>)]"),
        "<a href=\"{0}\">{1}</a>");
    // Single quotes work too:
    assertParseTemplateResult(("[L(<a href='), P((URL_ATTRIBUTE_ENTIRE,a,href),0), "
        + "L('>), P((TEXT,null,null),1), L(</a>)]"),
        "<a href='{0}'>{1}</a>");
    assertParseTemplateResult(
        ("[L(<a href=\"http://), P((ATTRIBUTE_VALUE,a,href),0), "
            + "L(\">), P((TEXT,null,null),1), L(</a>)]"),
        "<a href=\"http://{0}\">{1}</a>");
    assertParseTemplateResult(("[L(<a href=\"), P((URL_ATTRIBUTE_START,a,href),0), "
        + "L(/), P((ATTRIBUTE_VALUE,a,href),1), "
        + "L(\">), P((TEXT,null,null),2), L(</a>)]"),
        "<a href=\"{0}/{1}\">{2}</a>");

    // Verify correct escaping in attributes.
    assertParseTemplateResult(
        ("[L(<a href=\"http://...&amp;), " + "P((ATTRIBUTE_VALUE,a,href),0), "
            + "L(=), P((ATTRIBUTE_VALUE,a,href),1), "
            + "L(\">), P((TEXT,null,null),2), L(</a>)]"),
        "<a href=\"http://...&amp;{0}={1}\">{2}</a>");

    // Test correct detection of CSS context.
    assertParseTemplateResult(
        "[L(<div class=\"), P((ATTRIBUTE_VALUE,div,class),0), L(\" style=\"), "
            + "P((CSS_ATTRIBUTE_START,div,style),2), L(\">Hello ), "
            + "P((TEXT,null,null),1)]",
        "<div class=\"{0}\" style=\"{2}\">Hello {1}");
    assertParseTemplateResult(
        "[L(<div class=\"), P((ATTRIBUTE_VALUE,div,class),0), L(\" style=\"color:green; ), "
            + "P((CSS_ATTRIBUTE,div,style),2), L(\">Hello ), "
            + "P((TEXT,null,null),1)]",
        "<div class=\"{0}\" style=\"color:green; {2}\">Hello {1}");
    assertParseTemplateResult(
        "[L(<div>), P((TEXT,null,null),0), L(<style>foo ), "
            + "P((CSS,null,null),1), L(</style>)]",
        "<div>{0}<style>foo {1}</style>");

    // Test that javascript contexts without variables are allowed
    assertParseTemplateResult(
        "[L(<div onClick=alert() class=\"), "
            + "P((ATTRIBUTE_VALUE,div,class),0), L(\">)]",
        "<div onClick=alert() class=\"{0}\">");
  }

  private void assertParsingTemplateEndingInNonInnerHtmlContextFails(
      String template) {
    assertParseFails(
        "Template does not end in inner-HTML context: ", template, template);
  }

  public void testParseTemplate_endingInNonInnerHtmlContextFails() {
    assertParsingTemplateEndingInNonInnerHtmlContextFails("<div class=");
    assertParsingTemplateEndingInNonInnerHtmlContextFails("<div class=\"");
    assertParsingTemplateEndingInNonInnerHtmlContextFails("<div class=\"{0}");
    assertParsingTemplateEndingInNonInnerHtmlContextFails("<div class=\"{0}\"");
    assertParsingTemplateEndingInNonInnerHtmlContextFails(
        "<div class=\"{0}\" foo");
    assertParsingTemplateEndingInNonInnerHtmlContextFails(
        "<div class=\"{0}\" foo=bar");
    assertParsingTemplateEndingInNonInnerHtmlContextFails(
        "<div class=\"{0}\" foo=bar>{1}<a");
    assertParsingTemplateEndingInNonInnerHtmlContextFails(
        "<div class=\"{0}\" foo=bar>{1}<a href=");

    // Check that parseTemplate doesn't walk off the end of the string when
    // extracting lookAhead: We should be getting an error that the template
    // ends in non-inner-HTML context, and not an IndexOutOfBoundsException.
    assertParsingTemplateEndingInNonInnerHtmlContextFails("<a href='{0}'");
    assertParsingTemplateEndingInNonInnerHtmlContextFails("<a href='{0}");
  }

  private void assertTemplateVariableInUnquotedAttributeFails(
      String template, String failAtPrefix) {
    assertParseFails("Template variable in unquoted attribute value: ",
        template, failAtPrefix);
  }

  public void testParseTemplate_templateVariableInUnquotedAttributeFails() {
    assertTemplateVariableInUnquotedAttributeFails(
        "<div class={0}>", "<div class={0}");
    assertTemplateVariableInUnquotedAttributeFails(
        "<div style=blah class={0}>", "<div style=blah class={0}");
    assertTemplateVariableInUnquotedAttributeFails(
        "<div style=blah class=blah{0}>", "<div style=blah class=blah{0}");
    assertTemplateVariableInUnquotedAttributeFails(
        "<div style=blah class=\"{0}\" foo={1}>bar</div><a href={3}>",
        "<div style=blah class=\"{0}\" foo={1}");
    assertTemplateVariableInUnquotedAttributeFails(
        "<div style=blah class=\"{0}\" foo=\"{1}\">bar</div><a href={3}>",
        "<div style=blah class=\"{0}\" foo=\"{1}\">bar</div><a href={3}");
    assertTemplateVariableInUnquotedAttributeFails(
        "<div style=blah class=\"{0}\"foo=\"{1}\">bar</div><a href=http://{3}>",
        "<div style=blah class=\"{0}\"foo=\"{1}\">bar</div><a href=http://{3}");
  }

  private void assertTemplateVariableInJsContextFails(
      String template, String failAtPrefix) {
    assertParseFails(
        "Template variables in javascript context are not supported: ",
        template, failAtPrefix);
  }

  public void testParseTemplate_templateVariableInJsContextFails() {
    assertTemplateVariableInJsContextFails(
        "<div onClick=\"{0}\">", "<div onClick=\"{0}");
    assertTemplateVariableInJsContextFails(
        "<div onClick=\"alert({0})\">", "<div onClick=\"alert({0}");
    assertTemplateVariableInJsContextFails(
        "foo<script language=blah>{0};", "foo<script language=blah>{0}");
    assertTemplateVariableInJsContextFails(
        "foo<script language=blah>alert({0});",
        "foo<script language=blah>alert({0}");
  }

  private void assertTemplateVariableInCommentContextFails(
      String template, String failAtPrefix) {
    assertParseFails(
        "Template variables inside HTML comments are not supported: ",
        template, failAtPrefix);
  }

  public void testParseTemplate_templateVariableInCommentFails() {
    assertTemplateVariableInCommentContextFails(
        "<!-- Hello {0}-->", "<!-- Hello {0}");
    assertTemplateVariableInCommentContextFails(
        "<!--<script language=blah>alert({0});",
        "<!--<script language=blah>alert({0}");
  }

  private void assertTemplateVariableInAttributeNameFails(
      String template, String failAtPrefix) {
    assertParseFails(
        "Template variables in tags or in attribute names are not supported: ",
        template, failAtPrefix);
  }

  public void testParseTemplate_templateVariableInAttributeNameFails() {
    assertTemplateVariableInAttributeNameFails(
        "<div style=\"{0}\" {1}=\"{2}\">", "<div style=\"{0}\" {1}");
    assertTemplateVariableInAttributeNameFails(
        "<div style=\"{0}\" foo{1}=\"{2}\">", "<div style=\"{0}\" foo{1}");
  }

  public void testParseTemplate_templateVariableInMetaContentFails() {
    assertParseFails("Template variables in content attribute of meta tag are not supported: ",
        "<meta http-equiv=\"{0}\" content=\"{1}\">", "<meta http-equiv=\"{0}\" content=\"{1}");
  }

  private void assertParseFails(
      String expectedError, final String template, final String failAtPrefix) {
    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.expectError(
        expectedError
            + failAtPrefix, null);
    UnitTestTreeLogger logger = loggerBuilder.createLogger();

    HtmlTemplateParser parser = new HtmlTemplateParser(logger);
    try {
      parser.parseTemplate(template);
      fail("Parsing invalid template did not fail."
          + " Parsed representation: " + parser.getParsedTemplate().toString());
    } catch (UnableToCompleteException e) {
      logger.assertCorrectLogEntries();
    }
  }
}
