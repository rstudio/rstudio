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
package com.google.gwt.junit;

import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import junit.framework.TestCase;

/**
 * Tests of FakeSafeHtmlTemplatesMaker.
 */
public class FakeSafeHtmlTemplatesMakerTest extends TestCase {
  interface MyTemplates extends SafeHtmlTemplates {

    @Template("<span class=\"someclass\">Some message here</span>")
    SafeHtml mySimpleTemplate();

    @Template("<span class=\"{3}\">{0}: <a href=\"{1}\">{2}</a></span>")
    SafeHtml messageWithLink(SafeHtml message, String url, String linkText,
        String style);
  }

  public void testSimple() {
    MyTemplates templates = FakeSafeHtmlTemplatesMaker.create(MyTemplates.class);
    assertEquals("mySimpleTemplate", templates.mySimpleTemplate().asString());
  }

  public void testArgs() {
    MyTemplates templates = FakeSafeHtmlTemplatesMaker.create(MyTemplates.class);
    SafeHtml message = SafeHtmlUtils.fromString("message");
    assertEquals("messageWithLink[message, url, link, style]",
        templates.messageWithLink(message, "url", "link", "style").asString());
  }
}
