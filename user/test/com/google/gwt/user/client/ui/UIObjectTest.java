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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Tests UIObject. Currently, focuses on style name behaviors.
 */
public class UIObjectTest extends GWTTestCase {
  static class MyObject extends UIObject {
    public Element subElement;

    MyObject() {
      setElement(DOM.createDiv());
      subElement = DOM.createDiv();
    }

    @Override
    protected void onEnsureDebugId(String baseID) {
      super.onEnsureDebugId(baseID);
      ensureDebugId(subElement, baseID, "subElem");
    }
  }

  /**
   * Verify that an element has the specified debug id.
   * 
   * @param debugID the debug ID
   * @param elem the {@link Element} that should have the id
   */
  public static void assertDebugId(String debugID, Element elem) {
    debugID = UIObject.DEBUG_ID_PREFIX + debugID;
    assertEquals(debugID, DOM.getElementProperty(elem, "id"));
  }

  /**
   * Verify that the contents of an element match the expected contents. This
   * method is useful to test debug IDs of private, inaccessible members of a
   * Widget. Note that this method requires that the Widget is added to the
   * {@link RootPanel} and should be called from a
   * {@link com.google.gwt.user.client.DeferredCommand} to give the browser
   * enough time to register the ID.
   * 
   * @param debugID the debug ID of the element
   * @param contents the contents expected in the inner HTML
   */
  public static void assertDebugIdContents(String debugID, String contents) {
    debugID = UIObject.DEBUG_ID_PREFIX + debugID;
    Element elem = DOM.getElementById(debugID);
    assertEquals(contents, DOM.getInnerHTML(elem));
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testAccidentalPrimary() {
    MyObject o = new MyObject();
    o.addStyleName("accidentalPrimary");
    assertEquals("accidentalPrimary", o.getStylePrimaryName());
  }

  public void testAddAndRemoveEmptyStyleName() {
    MyObject o = new MyObject();

    o.setStylePrimaryName("primary");
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

    assertEquals("primary", o.getStylePrimaryName());
  }

  public void testDebugId() {
    // Test basic set
    MyObject o = new MyObject();
    Element oElem = o.getElement();
    o.ensureDebugId("test1");
    assertDebugId("test1", oElem);
    assertDebugId("test1-subElem", o.subElement);

    // Test override with new ID
    o.ensureDebugId("test2");
    assertDebugId("test2", oElem);
    assertDebugId("test2-subElem", o.subElement);

    // Test setting actual id
    DOM.setElementProperty(oElem, "id", "mytest");
    assertEquals("mytest", DOM.getElementProperty(oElem, "id"));
    assertDebugId("test2-subElem", o.subElement);

    // Test overriding with debug ID succeeds if ID present
    assertEquals("mytest", DOM.getElementProperty(oElem, "id"));
    o.ensureDebugId("test3");
    assertDebugId("test3", oElem);
    assertDebugId("test3-subElem", o.subElement);
  }

  public void testNormal() {
    // Test the basic set/get case.
    MyObject o = new MyObject();
    o.setStylePrimaryName("primaryStyle");

    // Note: getStyleName() explicitly returns the className attribute, so it
    // doesn't guarantee that there aren't leading or trailing spaces.
    assertEquals("primaryStyle", o.getStyleName());
    doDependentAndSecondaryStyleTest(o);
    assertEquals("primaryStyle", o.getStyleName());
  }

  public void testSetEmptyPrimaryStyleName() {
    MyObject o = new MyObject();
    try {
      o.setStylePrimaryName("");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }

    try {
      o.setStylePrimaryName(" ");
      fail();
    } catch (IllegalArgumentException e) {
      // This *should* throw.
    }
  }

  public void testSetStyleNameNormalization() {
    MyObject o = new MyObject();

    o.setStylePrimaryName(" one ");
    o.addStyleName("  two  ");
    o.addStyleName("\tthree\t");

    assertEquals("one two three", o.getStyleName());
  }

  public void testSetStylePrimaryName() {
    MyObject o = new MyObject();
    o.setStylePrimaryName("gwt");
    o.addStyleDependentName("dependent");
    o.addStyleName("i-heart-gwt");
    o.addStyleName("i-gwt-heart");

    assertTrue(containsClass(o, "gwt"));
    assertTrue(containsClass(o, "gwt-dependent"));
    assertTrue(containsClass(o, "i-heart-gwt"));
    assertTrue(containsClass(o, "i-gwt-heart"));

    o.setStylePrimaryName("awt");

    assertPrimaryStyleNameEquals(o, "awt");
    assertTrue(containsClass(o, "awt-dependent"));
    assertFalse(containsClass(o, "gwt-dependent"));
    assertTrue(containsClass(o, "i-heart-gwt"));
    assertTrue(containsClass(o, "i-gwt-heart"));
    assertFalse(containsClass(o, "i-heart-awt"));
    assertFalse(containsClass(o, "i-awt-heart"));
  }

  private void assertPrimaryStyleNameEquals(UIObject o, String className) {
    String attr = DOM.getElementProperty(o.getElement(), "className");
    assertTrue(attr.indexOf(className) == 0);
    assertTrue(attr.length() == className.length()
        || attr.charAt(className.length()) == ' ');
  }

  private boolean containsClass(UIObject o, String className) {
    String[] classes = DOM.getElementProperty(o.getElement(), "className").split(
        "\\s+");
    for (int i = 0; i < classes.length; i++) {
      if (className.equals(classes[i])) {
        return true;
      }
    }
    return false;
  }

  // doStuff() should leave MyObject's style in the same state it started in.
  private void doDependentAndSecondaryStyleTest(MyObject o) {
    // Test that the primary style remains the first class, and that the
    // dependent style shows up.
    o.addStyleDependentName("dependent");
    assertTrue(containsClass(o, o.getStylePrimaryName() + "-dependent"));

    String oldPrimaryStyle = o.getStylePrimaryName();

    // Test that replacing the primary style name works (and doesn't munge up
    // the secondary style).
    o.addStyleName("secondaryStyle");
    o.setStylePrimaryName("newPrimaryStyle");

    assertEquals("newPrimaryStyle", o.getStylePrimaryName());
    assertPrimaryStyleNameEquals(o, "newPrimaryStyle");
    assertTrue(containsClass(o, "newPrimaryStyle-dependent"));
    assertTrue(containsClass(o, "secondaryStyle"));
    assertFalse(containsClass(o, oldPrimaryStyle));
    assertFalse(containsClass(o, oldPrimaryStyle + "-dependent"));

    // Clean up & return.
    o.setStylePrimaryName(oldPrimaryStyle);
    o.removeStyleDependentName("dependent");
    o.removeStyleName("secondaryStyle");
    assertFalse(containsClass(o, o.getStylePrimaryName() + "-dependent"));
    assertFalse(containsClass(o, "secondaryStyle"));
  }
}
