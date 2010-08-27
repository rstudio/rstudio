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
package com.google.gwt.safehtml.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

/**
 * Tests the SafeHtmlTemplates compile-time binding mechanism.
 */
public class SafeHtmlTemplatesTest extends GWTTestCase {

  private static final String HTML_MARKUP = "woo <i>whee</i>";

  private static final String GOOD_URL_ESCAPED =
      "http://foo.com/foo&lt;bar&gt;&amp;baz=dootz";
  private static final String GOOD_URL = "http://foo.com/foo<bar>&baz=dootz";
  private static final String BAD_URL = "javascript:evil(1<2)";
  private static final String BAD_URL_ESCAPED = "javascript:evil(1&lt;2)";

  private TestTemplates templates;

  @Override
  public String getModuleName() {
    return "com.google.gwt.safehtml.SafeHtml";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    templates = GWT.create(TestTemplates.class);
  }

  /**
   * A SafeHtmlTemplates interface for testing.
   */
  public interface TestTemplates extends SafeHtmlTemplates {
    @Template("<span><b>{0}</b><span>{1}</span></span>")
    SafeHtml simpleTemplate(String foo, SafeHtml bar);  

    @Template("<span><a href=\"{0}\"><b>{1}</b></a></span>")
    SafeHtml templateWithUriAttribute(String url, SafeHtml html);

    @Template("<div id=\"{0}\">{1}</div>")
    SafeHtml templateWithRegularAttribute(String id, SafeHtml html);

    @Template("<span><img src=\"{0}/{1}\"/></span>")
    SafeHtml templateWithTwoPartUriAttribute(String baseUrl, String urlPart);
  }

  public void testSimpleTemplate() {
    Assert.assertEquals(
        "<span><b>foo&lt;bar</b><span>woo <i>whee</i></span></span>",
        templates.simpleTemplate("foo<bar",
            SafeHtmlUtils.fromSafeConstant(HTML_MARKUP)).asString());
  }

  public void testTemplateWithUriAttribute() {
    Assert.assertEquals(
        "<span><a href=\"" + GOOD_URL_ESCAPED + "\"><b>" + HTML_MARKUP
            + "</b></a></span>",
        templates.templateWithUriAttribute(
            GOOD_URL, SafeHtmlUtils.fromSafeConstant(HTML_MARKUP)).asString());
    Assert.assertEquals(
        "<span><a href=\"#\"><b>" + HTML_MARKUP + "</b></a></span>",
        templates.templateWithUriAttribute(
            BAD_URL, SafeHtmlUtils.fromSafeConstant(HTML_MARKUP)).asString());
  }
  
  public void testTemplateWithRegularAttribute() {
    Assert.assertEquals(
        "<div id=\"" + GOOD_URL_ESCAPED + "\">" + HTML_MARKUP + "</div>",
        templates.templateWithRegularAttribute(
            GOOD_URL, SafeHtmlUtils.fromSafeConstant(HTML_MARKUP)).asString());
    
    // Values inserted into non-URI-valued attributes should not be sanitized,
    // but still escaped.
    Assert.assertEquals(
        "<div id=\"" + BAD_URL_ESCAPED + "\">" + HTML_MARKUP + "</div>",
        templates.templateWithRegularAttribute(
            BAD_URL, SafeHtmlUtils.fromSafeConstant(HTML_MARKUP)).asString());
  }
  
  public void testTemplateWithTwoPartUriAttribute() {
    Assert.assertEquals(
        "<span><img src=\"" + GOOD_URL_ESCAPED + "/x&amp;y\"></img></span>",
        templates.templateWithTwoPartUriAttribute(
            GOOD_URL, "x&y").asString());
    Assert.assertEquals(
        "<span><img src=\"#/x&amp;y\"></img></span>",
        templates.templateWithTwoPartUriAttribute(
            BAD_URL, "x&y").asString());
  }
}
