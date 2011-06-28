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
package com.google.gwt.dom.builder.shared;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test for implementations of {@link ElementBuilderImpl}.
 */
public abstract class GwtElementBuilderImplTestBase extends GWTTestCase {

  private ElementBuilderFactory factory;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.builder.DomBuilder";
  }

  /**
   * Attributes on an element.
   * 
   * <pre>
   * <div id="myId" title="myTitle">hello world</div>
   * </pre>
   */
  public void testAttributes() {
    Element div =
        factory.createDivBuilder().id("myId").title("myTitle").text("hello world").finish();
    assertTrue("div".equalsIgnoreCase(div.getTagName()));
    assertEquals("hello world", div.getInnerText());
    assertEquals("myId", div.getId());
    assertEquals("myTitle", div.getTitle());
  }

  public void testFinishTwice() {
    DivBuilder builder = factory.createDivBuilder();
    assertNotNull(builder.finish());

    try {
      builder.finish();
      fail("Expected IllegalStateException: cannot call finish() twice");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  /**
   * Children nested at multiple levels.
   * 
   * <pre>
   * <div id="root">
   *   <div id="child">
   *     <div id="grandchild">grandchild</div>
   *   </div>
   * </div>
   * </pre>
   */
  public void testNestedChildElements() {
    DivBuilder builder = factory.createDivBuilder().id("root");
    builder.startDiv().id("child").startDiv().id("grandchild").text("grandchild");
    Element div = builder.finish();
    assertTrue("div".equalsIgnoreCase(div.getTagName()));
    assertEquals("root", div.getId());
    assertEquals(1, div.getChildCount());

    Element child = div.getFirstChildElement();
    assertTrue("div".equalsIgnoreCase(child.getTagName()));
    assertEquals("child", child.getId());
    assertEquals(1, child.getChildCount());

    Element grandchild = child.getFirstChildElement();
    assertTrue("div".equalsIgnoreCase(grandchild.getTagName()));
    assertEquals("grandchild", grandchild.getId());
    assertNull(grandchild.getFirstChildElement());
  }

  /**
   * Multiple children beneath root.
   * 
   * <pre>
   * <div>
   *   <div>div0</div>
   *   <div>div1</div>
   *   <div>div2</div>
   * </div>
   * </pre>
   */
  public void testNestedSiblingElements() {
    DivBuilder builder = factory.createDivBuilder();
    for (int i = 0; i < 3; i++) {
      builder.startDiv().text("div" + i).endDiv();
    }
    Element div = builder.finish();
    assertTrue("div".equalsIgnoreCase(div.getTagName()));
    assertEquals(3, div.getChildCount());

    Element child = div.getFirstChildElement();
    assertEquals("div0", child.getInnerText());
    child = child.getNextSiblingElement();
    assertEquals("div1", child.getInnerText());
    child = child.getNextSiblingElement();
    assertEquals("div2", child.getInnerText());
  }

  /**
   * Single element with text.
   * 
   * <pre>
   * <div>hello world</div>.
   * </pre>
   */
  public void testSingleElement() {
    Element div = factory.createDivBuilder().text("hello world").finish();
    assertTrue("div".equalsIgnoreCase(div.getTagName()));
    assertEquals("hello world", div.getInnerText());
  }

  /**
   * Element with style properties.
   * 
   * <pre>
   * <div style="color:red;position:absolute;">hello world</div>.
   * </pre>
   */
  public void testStyleProperties() {
    Element div =
        factory.createDivBuilder().style().trustedColor("red").position(Position.ABSOLUTE)
            .endStyle().text("hello world").finish();
    assertTrue("div".equalsIgnoreCase(div.getTagName()));
    assertEquals("hello world", div.getInnerText());
    assertEquals("red", div.getStyle().getColor());
    assertEquals("absolute", div.getStyle().getPosition());
  }

  public void testTrustedStart() {
    {
      DivBuilder div = factory.createDivBuilder();
      div.trustedStart("lowercase");
      div.trustedStart("UPPERCASE");
      div.trustedStart("camelCase");
      div.trustedStart("containsNumber0");
      div.trustedStart("a");
    }

    // Empty tagName.
    try {
      DivBuilder div = factory.createDivBuilder();
      div.trustedStart("");
      fail("Expected IllegalArgumentException: Empty string is not a valid tag name");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      DivBuilder div = factory.createDivBuilder();
      div.trustedStart("<containsbracket");
      fail("Expected IllegalArgumentException: TagName cannot contain brackets (<)");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  /**
   * Get the element builder factory used to create the implementation.
   */
  protected abstract ElementBuilderFactory getElementBuilderFactory();

  @Override
  protected void gwtSetUp() throws Exception {
    factory = getElementBuilderFactory();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    factory = null;
  }
}
