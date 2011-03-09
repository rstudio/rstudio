/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.uibinder.rebind.Tokenator;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * HtmlTemplate unit tests.
 *
 */
public class HtmlTemplateTest extends TestCase {
  public void testHappy() {
    Tokenator t = new Tokenator();    
    HtmlTemplates templates = new HtmlTemplates();
    
    HtmlTemplate h = new HtmlTemplate("<DialogBox id=\'" 
        + t.nextToken("\" + domId0 + \"")
        + "\'>this is a dialog box</DialogBox>", t, templates);
    
    String[] expectedTemplates = {
        "@Template(\"<DialogBox id=\'{0}\'>this is a dialog box</DialogBox>\")",
        "SafeHtml html1(String arg0);",
        " ",};
    
    Iterator<String> j = h.getTemplate().iterator();
    for (String et : expectedTemplates) {
      assertEquals(et, j.next());
    }
    assertFalse(j.hasNext());

    String expectedCall = "template.html1(domId0).asString()";

    assertEquals(h.writeTemplateCall(), expectedCall);
  }
  
  public void testNullHtml() {
    HtmlTemplates templates = new HtmlTemplates();
    Tokenator t = new Tokenator();
    
    try {
      new HtmlTemplate(null, t, templates);
      fail("Expected empty html to generate error");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().equals("Template html cannot be null"));
    }
  }
  
  public void testNullTokenator() {
    HtmlTemplates templates = new HtmlTemplates();
    
    try {
      new HtmlTemplate("<p>this is a static string</p>", null, 
          templates);
      fail("Expected empty tokenator to generate error");
      } catch (IllegalArgumentException e) {
        assertTrue(e.getMessage().equals("Template tokenator cannot be null"));
      }
  }

  public void testNullHtmlTemplates() {
    Tokenator t = new Tokenator();
    
    try {
      new HtmlTemplate("<p>this is a static string</p>", t, null);
      fail("Expected empty html to generate error");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().equals("HtmlTemplates container cannot be null"));
    }
  }

}
