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

import com.google.gwt.i18n.client.ColorsAndShapes;
import com.google.gwt.i18n.client.ColorsAndShapesAndConcepts;
import com.google.gwt.i18n.client.gen.Colors;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * TODO: document me.
 */
public class AbstractResourceTest extends TestCase {
  public static final String UNICODE = "Îñţérñåţîöñåļîžåţîöñ";
  private static final String LOCALE_NAME_PIGLATIN = "piglatin";
  private static final String LOCALE_NAME_PIGLATIN_UK = "piglatin_UK";

  public void testBundle() {
    // simple test
    String s = Colors.class.getName();
    ResourceList resourceList = ResourceFactory.getBundle(s, null, true);
    assertNotNull(resourceList);
    ResourceList pigLatinResourceList =
        ResourceFactory.getBundle(s, LOCALE_NAME_PIGLATIN, true);
    assertNotNull(pigLatinResourceList);
    assertEquals("ueblay", pigLatinResourceList.getString("blue"));
    assertEquals("ĝréý", pigLatinResourceList.getString("grey"));
  }

  public void testInheritence() {
    ResourceFactory.clearCache();
    ResourceList resourceList = ResourceFactory.getBundle(
      ColorsAndShapes.class, LOCALE_NAME_PIGLATIN, true);
    assertEquals("ueblay", resourceList.getString("blue"));
    assertEquals("ĝréý", resourceList.getString("grey"));
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
    ResourceList resourceList = ResourceFactory.getBundle(
      ColorsAndShapesAndConcepts.class, LOCALE_NAME_PIGLATIN_UK, true);
    String s = resourceList.getString("internationalization");
    assertEquals("Îñţérñåţîöñåļîžåţîöñ", s);
    assertTrue(resourceList.keySet().size() > 5);
  }

  public void testCharArrayBehavior() {
    CharArrayWriter s = new CharArrayWriter();
    PrintWriter t = new PrintWriter(s, true);
    t.print(UNICODE);
    t.close();
    assertEquals(UNICODE, s.toString());
  }
}
