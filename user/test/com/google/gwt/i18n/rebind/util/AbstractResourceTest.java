/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.i18n.rebind.util;

import com.google.gwt.i18n.client.ColorsAndShapes;
import com.google.gwt.i18n.client.ColorsAndShapesAndConcepts;
import com.google.gwt.i18n.client.gen.Colors;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * TODO: document me.
 */
public class AbstractResourceTest extends TestCase {
  static Locale pigLatin = new Locale("piglatin");
  public static final String UNICODE = "Îñţérñåţîöñåļîžåţîöñ";

  public void testBundle() {
    // simple test
    String s = Colors.class.getName();
    AbstractResource resource = ResourceFactory.getBundle(s, null);
    assertNotNull(resource);
    AbstractResource pigLatinResource = ResourceFactory.getBundle(s, pigLatin);
    assertEquals(pigLatin, pigLatinResource.getLocale());
    assertNotNull(pigLatinResource);
    assertEquals("ueblay", pigLatinResource.getString("blue"));
    assertEquals("ĝréý", pigLatinResource.getString("grey"));
  }

  public void testInheritence() {
    ResourceFactory.clearCache();
    AbstractResource resource = ResourceFactory.getBundle(
      ColorsAndShapes.class, pigLatin);
    assertEquals(pigLatin, resource.getLocale());
    assertEquals("ueblay", resource.getString("blue"));
    assertEquals("ĝréý", resource.getString("grey"));
  }

  public void testByteStreamBehavior() throws UnsupportedEncodingException {
    ByteArrayOutputStream s = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(s, "UTF-8");
    PrintWriter t = new PrintWriter(writer, true);
    t.print("ĝréý");
    t.close();
    assertEquals("ĝréý", s.toString("UTF-8"));
    assertEquals("ĝréý", (new String(s.toString("UTF-8").toCharArray())));
  }

  public void testDoubleInherits() {
    AbstractResource resource = ResourceFactory.getBundle(
      ColorsAndShapesAndConcepts.class, new Locale("piglatin", "UK"));
    String s = resource.getString("internationalization");
    assertEquals("Îñţérñåţîöñåļîžåţîöñ", s);
    assertTrue(resource.keySet().size() > 5);
  }

  public void testCharArrayBehavior() {
    CharArrayWriter s = new CharArrayWriter();
    PrintWriter t = new PrintWriter(s, true);
    t.print(UNICODE);
    t.close();
    assertEquals(UNICODE, s.toString());
  }
}
