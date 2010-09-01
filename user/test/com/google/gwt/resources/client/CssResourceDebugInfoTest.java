/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.resources.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.CssResource.DebugInfo;
import com.google.gwt.resources.client.CssResource.NotStrict;

import java.util.Map;

/**
 * Tests for {@link CssResource.DebugInfo}.
 */
public class CssResourceDebugInfoTest extends GWTTestCase {
  interface Concatenated extends CssResource {
    String partA();

    String partB();
  }

  interface Resources extends ClientBundle {
    @Source(value = {"concatenatedA.css", "concatenatedB.css"})
    Concatenated concatenated();

    CssResource empty();

    @NotStrict
    @Source("concatenatedA.css")
    CssResource notStrict();

    @NotStrict
    @Source("concatenatedB.css")
    CssResource unused();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.EnableCssResourceDebugging";
  }

  public void testClassMap() {
    Resources r = GWT.create(Resources.class);
    Concatenated css = r.concatenated();
    DebugInfo info = css.getDebugInfo();

    assertEquals(2, info.getSource().length);

    Map<String, String> map = info.getClassMap();
    assertEquals(css.partA(), map.get("partA"));
    assertEquals(css.partB(), map.get("partB"));
    assertNull(map.get("something"));

    CssResource notStrict = r.notStrict();
    info = notStrict.getDebugInfo();
    assertEquals("partA", info.getClassMap().get("partA"));
  }

  /**
   * Test the JavaScriptObject-based obfuscation map. This should be installed
   * by EnableCssResourceDebugging.gwt.xml.
   */
  public void testCssResourceObserver() {
    JavaScriptObject myMap = getMap();
    assertNotNull("No map registered", myMap);

    Resources r = GWT.create(Resources.class);
    Concatenated css = r.concatenated();
    DebugInfo info = css.getDebugInfo();
    String prefix = info.getOwnerType() + "." + info.getMethodName() + ".";
    assertEquals(css.partA(), get(myMap, prefix + "partA"));
    assertEquals(css.partB(), get(myMap, prefix + "partB"));
    assertNull(get(myMap, prefix + "partC"));

    // Test the unobfuscated resource
    assertEquals("partA", get(myMap, info.getOwnerType() + ".notStrict.partA"));

    // Test the otherwise-unused resource
    assertEquals("partB", get(myMap, info.getOwnerType() + ".unused.partB"));
  }

  public void testTrivialDebugInfo() {
    Resources r = GWT.create(Resources.class);
    CssResource empty = r.empty();
    DebugInfo info = empty.getDebugInfo();
    assertNotNull(info);
    assertEquals("empty", info.getMethodName());
    assertEquals(
        "com.google.gwt.resources.client.CssResourceDebugInfoTest$Resources",
        info.getOwnerType());
    assertEquals(1, info.getSource().length);
    assertTrue(info.getSource()[0].endsWith("empty.css"));
  }

  private native String get(JavaScriptObject map, String key) /*-{
    return map[key];
  }-*/;

  private native JavaScriptObject getMap() /*-{
    return $wnd.gwtCssResource[@com.google.gwt.core.client.GWT::getModuleName()()];
  }-*/;
}
