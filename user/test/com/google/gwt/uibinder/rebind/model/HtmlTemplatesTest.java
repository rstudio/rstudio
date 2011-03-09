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

import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.Tokenator;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * HtmlTemplates unit tests.
 *
 */
public class HtmlTemplatesTest extends TestCase {
  public void testHappy() throws IllegalArgumentException {
    Tokenator t = new Tokenator();    
    HtmlTemplates h = new HtmlTemplates();
   
    String call = h.addSafeHtmlTemplate("<DialogBox id=\'" 
        + t.nextToken("\" + domId0 + \"")
        + "\'>this is a dialog box</DialogBox>", t);
    
    // Check the generated template
    String[] expectedTemplates = {
        "@Template(\"<DialogBox id=\'{0}\'>this is a dialog box</DialogBox>\")",
        "SafeHtml html1(String arg0);",
        " ",};
        
    List<String> templates = new ArrayList<String>();
    for (HtmlTemplate ht : h.getTemplates()) {
      templates.addAll(ht.getTemplate());
    }
        
    Iterator<String> i = templates.iterator();
    for (String et : expectedTemplates) {
        assertEquals(et, i.next());
    }
    assertFalse(i.hasNext());
    
    // Check the returned template function call
    String expectedCall = "template.html1(domId0).asString()";
    assertEquals(call, expectedCall);
    
    StringWriter s = new StringWriter();
    IndentedWriter n = new IndentedWriter(new PrintWriter(s));
    
    // Confirm that IndentedWriter writes the correct template.
    h.writeTemplates(n);
    
    assertEquals("@Template(\"<DialogBox id=\'{0}\'>this is a dialog "
        + "box</DialogBox>\")\nSafeHtml html1(String arg0);\n \n", s.toString());
  }
  
  public void testNullHtml() {
    HtmlTemplates h = new HtmlTemplates();
    Tokenator t = new Tokenator();
    
    try {
    h.addSafeHtmlTemplate(null, t);
    fail();
    } catch (IllegalArgumentException e) {
      assertTrue("Expected empty html to generate error", 
        e.getMessage().equals("Template html cannot be null"));
    }

    assertTrue(h.isEmpty());
  } 
  
  public void testNullTokenator() throws IllegalArgumentException {
    HtmlTemplates h = new HtmlTemplates();
    
    try {
    h.addSafeHtmlTemplate("<p>this is a static string</p>", null);
    fail();
    } catch (IllegalArgumentException e) {
      assertTrue("Expected empty tokenator to generate error", 
          e.getMessage().equals("Template tokenator cannot be null"));
    }

    assertTrue(h.isEmpty());
  } 
}
