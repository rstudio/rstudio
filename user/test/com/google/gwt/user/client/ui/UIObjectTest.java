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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;

/**
 * Tests UIObject. Currently, focuses on style name behaviors.
 */
public class UIObjectTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  static class MyObject extends UIObject {
    MyObject() {
      setElement(DOM.createDiv());
    }
  }

  public void testEmpty() {
    MyObject o = new MyObject();

    assertEquals("gwt-nostyle", o.getStyleName());
    doStuff(o);
    assertEquals("gwt-nostyle", o.getStyleName());
  }

  public void testNormal() {
    // Test the basic set/get case.
    MyObject o = new MyObject();

    o.setStyleName("baseStyle");

    assertEquals("baseStyle", o.getStyleName());
    doStuff(o);
    assertEquals("baseStyle", o.getStyleName());
  }

  public void testAddStyleBeforeSet() {
    MyObject o = new MyObject();

    // Test that adding a style name before calling setStyleName() causes the
    // gwt-nostyle class to get added.
    o.addStyleName("userStyle");
    assertStartsWithClass(o, "gwt-nostyle");
    assertContainsClass(o, "userStyle");
    o.removeStyleName("userStyle");
    assertDoesNotContainClass(o, "userStyle");

    // getStyleName() should still be "gwt-nostyle".
    assertEquals("gwt-nostyle", o.getStyleName());

    doStuff(o);

    assertStartsWithClass(o, "gwt-nostyle");
    assertEquals("gwt-nostyle", o.getStyleName());
  }

  public void testAddAndRemoveEmptyStyleName() {
    MyObject o = new MyObject();

    o.setStyleName("base");
    try {
      o.addStyleName("");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }

    try {
      o.addStyleName(" ");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }

    try {
      o.removeStyleName("");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }

    try {
      o.removeStyleName(" ");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }

    assertEquals("base", o.getStyleName());
  }

  public void testSetEmptyBaseStyleName() {
    MyObject o = new MyObject();
    try {
      o.setStyleName("");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }

    try {
      o.setStyleName(" ");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }
  }

  public void testRemoveBaseStyleName() {
    MyObject o = new MyObject();
    o.setStyleName("base");

    try {
      o.removeStyleName("base");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }
  }

  // doStuff() should leave MyObject's style in the same state it started in.
  private void doStuff(MyObject o) {
    // Test that the base style remains the first class, and that the dependent
    // style shows up.
    o.addStyleName(o.getStyleName() + "-dependent");
    assertContainsClass(o, o.getStyleName() + "-dependent");

    String oldBaseStyle = o.getStyleName();

    // Test that replacing the base style name works (and doesn't munge up the
    // user style).
    o.addStyleName("userStyle");
    o.setStyleName("newBaseStyle");

    assertEquals("newBaseStyle", o.getStyleName());
    assertStartsWithClass(o, "newBaseStyle");
    assertContainsClass(o, "newBaseStyle-dependent");
    assertContainsClass(o, "userStyle");
    assertDoesNotContainClass(o, oldBaseStyle);
    assertDoesNotContainClass(o, oldBaseStyle + "-dependent");

    // Clean up & return.
    o.setStyleName(oldBaseStyle);
    o.removeStyleName(oldBaseStyle + "-dependent");
    o.removeStyleName("userStyle");
  }

  private void assertContainsClass(UIObject o, String className) {
    String attr = DOM.getElementProperty(o.getElement(), "className");
    assertTrue(attr.indexOf(className) != -1);
  }

  private void assertDoesNotContainClass(UIObject o, String className) {
    String attr = DOM.getElementProperty(o.getElement(), "className");
    assertTrue(attr.indexOf(className) == -1);
  }

  private void assertStartsWithClass(UIObject o, String className) {
    String attr = DOM.getElementProperty(o.getElement(), "className");
    assertTrue(attr.indexOf(className) == 0);
  }
}
