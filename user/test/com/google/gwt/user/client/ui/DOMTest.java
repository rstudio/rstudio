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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

/**
 * Tests standard DOM operations in the {@link DOM} class.
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

  /**
   * Tests {@link DOM#getAbsoluteLeft(Element)} and
   * {@link DOM#getAbsoluteTop(Element)}.
   */
  public void testGetAbsolutePosition() {
    final int border = 8;
    final int margin = 9;
    final int padding = 10;

    final int top = 15;
    final int left = 14;

    final Element elem = DOM.createDiv();
    DOM.appendChild(RootPanel.getBodyElement(), elem);

    DOM.setStyleAttribute(elem, "position", "absolute");
    DOM.setStyleAttribute(elem, "border", border + "px solid #000");
    DOM.setStyleAttribute(elem, "padding", padding + "px");
    DOM.setStyleAttribute(elem, "margin", margin + "px");

    DOM.setStyleAttribute(elem, "top", top + "px");
    DOM.setStyleAttribute(elem, "left", left + "px");

    delayTestFinish(1000);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        assertEquals(top + margin, DOM.getAbsoluteTop(elem));
        assertEquals(left + margin, DOM.getAbsoluteLeft(elem));
        finishTest();
      }
    });
  }

  /**
   * Tests {@link DOM#getAbsoluteTop(Element)} and
   * {@link DOM#getAbsoluteLeft(Element)} for consistency when the element
   * contains children and has scrollbars. See issue #1093 for more details.
   * 
   */
  public void testGetAbsolutePositionWhenScrolled() {
    final Element outer = DOM.createDiv();
    final Element inner = DOM.createDiv();

    DOM.setStyleAttribute(outer, "position", "absolute");
    DOM.setStyleAttribute(outer, "top", "0px");
    DOM.setStyleAttribute(outer, "left", "0px");
    DOM.setStyleAttribute(outer, "overflow", "auto");
    DOM.setStyleAttribute(outer, "width", "200px");
    DOM.setStyleAttribute(outer, "height", "200px");
   
    DOM.setStyleAttribute(inner, "marginTop", "800px");
    DOM.setStyleAttribute(inner, "marginLeft", "800px");

    DOM.appendChild(outer, inner);
    DOM.appendChild(RootPanel.getBodyElement(), outer);
    DOM.setInnerText(inner, ":-)");
    DOM.scrollIntoView(inner);

    // Ensure that we are scrolled.
    assertTrue(DOM.getElementPropertyInt(outer, "scrollTop") > 0);
    assertTrue(DOM.getElementPropertyInt(outer, "scrollLeft") > 0);

    assertEquals(0, DOM.getAbsoluteTop(outer));
    assertEquals(0, DOM.getAbsoluteLeft(outer));
  }

  /**
   * Tests the ability to do a parent-ward walk in the DOM.
   */
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

  /**
   * Tests {@link DOM#insertChild(Element, Element, int)}.
   *
   */
  public void testInsertChild() {
    Element parent = RootPanel.get().getElement();
    Element div = DOM.createDiv();
    DOM.insertChild(parent, div, Integer.MAX_VALUE);
    Element child = DOM.getChild(RootPanel.get().getElement(),
        DOM.getChildCount(parent) - 1);
    assertEquals(div, child);
  }

  /**
   * Tests that {@link DOM#isOrHasChild(Element, Element)} works consistently
   * across browsers.
   */
  public void testIsOrHasChild() {
    Element div = DOM.createDiv();
    Element childDiv = DOM.createDiv();
    assertFalse(DOM.isOrHasChild(div, childDiv));
    DOM.appendChild(div, childDiv);
    assertTrue(DOM.isOrHasChild(div, childDiv));
    assertFalse(DOM.isOrHasChild(childDiv, div));
  }

  /**
   * Tests that {@link DOM#setInnerText(Element, String)} works consistently
   * across browsers.
   */
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

  /**
   * Tests {@link DOM#toString(Element)} against likely failure points.
   */
  public void testToString() {
    Button b = new Button("abcdef");
    assertTrue(b.toString().indexOf("abcdef") != -1);
    assertTrue(b.toString().toLowerCase().indexOf("button") != -1);

    // Test <img src="http://.../logo.gif" />
    Element image = DOM.createImg();
    String imageUrl = "http://www.google.com/images/logo.gif";
    DOM.setImgSrc(image, imageUrl);
    String imageToString = DOM.toString(image).trim().toLowerCase();
    assertTrue(imageToString.startsWith("<img"));
    assertTrue(imageToString.indexOf(imageUrl) != -1);

    // Test <input name="flinks" />
    Element input = DOM.createInputText();
    DOM.setElementProperty(input, "name", "flinks");
    final String inputToString = DOM.toString(input).trim().toLowerCase();
    assertTrue(inputToString.startsWith("<input"));

    // Test <select><option>....</select>
    Element select = DOM.createSelect();
    for (int i = 0; i < 10; i++) {
      final Element option = DOM.createElement("option");
      DOM.appendChild(select, option);
      DOM.setInnerText(option, "item #" + i);
    }
    String selectToString = DOM.toString(select).trim().toLowerCase();
    assertTrue(selectToString.startsWith("<select"));
    for (int i = 0; i < 10; i++) {
      assertTrue(selectToString.indexOf("item #" + i) != -1);
    }

    // Test <meta name="robots" />
    Element meta = DOM.createElement("meta");
    DOM.setElementProperty(meta, "name", "robots");
    String metaToString = DOM.toString(meta).trim().toLowerCase();
    assertTrue(metaToString.startsWith("<meta"));
  }
}
