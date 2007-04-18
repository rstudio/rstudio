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
import com.google.gwt.user.client.Element;

/**
 * TODO: document me.
 */
public class DOMTest extends GWTTestCase {

  /**
   * Helper method to return the denormalized child count of a DOM Element. For
   * example, child nodes which have a nodeType of Text are included in the
   * count, whereas <code>DOM.getChildCount(Element parent)</code> only counts
   * the child nodes which have a nodeType of Element.
   * 
   * @param elem the DOM element to check the child count for
   * @return The number of child nodes
   */
  public static native int getDenormalizedChildCount(Element elem) /*-{
    return (elem.childNodes.length);
  }-*/;

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test DOM.get/set/removeElementAttribute() methods.
   */
  public void testElementAttribute() {
    Element div = DOM.createDiv();
    DOM.setElementAttribute(div, "class", "testClass");
    String cssClass = DOM.getElementAttribute(div, "class");
    assertEquals("testClass", cssClass);
    DOM.removeElementAttribute(div, "class");
    cssClass = DOM.getElementAttribute(div, "class");
    assertNull(cssClass);
  }
  
  public void testGetParent() {
    Element element = RootPanel.get().getElement();
    int i = 0;
    while (i < 10 && element != null) {
      element = DOM.getParent(element);
      i++;
    }
    // If we got here we looped "forever" or passed, as no exception was thrown.
    if (i == 10) {
      fail("Cyclic parent structure detected.");
    }
    // If we get here, we pass, because we encountered no errors going to the
    // top of the parent hierarchy.
  }

  public void testSetInnerText() {
    Element tableElem = DOM.createTable();

    Element trElem = DOM.createTR();

    Element tdElem = DOM.createTD();
    DOM.setInnerText(tdElem, "Some Table Heading Data");

    // Add a <em> element as a child to the td element
    Element emElem = DOM.createElement("em");
    DOM.setInnerText(emElem, "Some emphasized text");
    DOM.appendChild(tdElem, emElem);

    DOM.appendChild(trElem, tdElem);

    DOM.appendChild(tableElem, trElem);

    DOM.appendChild(RootPanel.getBodyElement(), tableElem);

    DOM.setInnerText(tdElem, null);

    // Once we set the inner text on an element to null, all of the element's
    // child nodes
    // should be deleted, including any text nodes, for all supported browsers.
    assertTrue(getDenormalizedChildCount(tdElem) == 0);
  }

  public void testToString() {
    Button b = new Button("abcdef");
    assertTrue(b.toString().indexOf("abcdef") != -1);
    assertTrue(b.toString().toLowerCase().indexOf("button") != -1);
  }
}
