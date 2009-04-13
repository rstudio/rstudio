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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Element tests (many stolen from DOMTest).
 */
public class ElementTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * [get|set|remove]Attribute.
   */
  public void testElementAttribute() {
    DivElement div = Document.get().createDivElement();
    div.setAttribute("class", "testClass");
    String cssClass = div.getAttribute("class");
    assertEquals("testClass", cssClass);
    div.removeAttribute("class");
    cssClass = div.getAttribute("class");
    assertEquals("", cssClass);
  }

  /**
   * Ensure that the return type of an attribute is always a string. IE should
   * not return a numeric attribute based on the element property. See issue
   * 3238.
   */
  public void testElementAttributeNumeric() {
    DivElement div = Document.get().createDivElement();
    Document.get().getBody().appendChild(div);
    div.setInnerText("Hello World");
    div.getAttribute("offsetWidth").length();
    div.getAttribute("offsetWidth").trim().length();
    Document.get().getBody().removeChild(div);
  }

  /**
   * getAbsolute[Left|Top].
   */
  public void testGetAbsolutePosition() {
    final int border = 8;
    final int margin = 9;
    final int padding = 10;

    final int top = 15;
    final int left = 14;

    Document doc = Document.get();
    final DivElement elem = doc.createDivElement();
    doc.getBody().appendChild(elem);

    elem.getStyle().setProperty("position", "absolute");
    elem.getStyle().setProperty("border", border + "px solid #000");
    elem.getStyle().setProperty("padding", padding + "px");
    elem.getStyle().setProperty("margin", margin + "px");

    elem.getStyle().setPropertyPx("top", top - doc.getBodyOffsetLeft());
    elem.getStyle().setPropertyPx("left", left - doc.getBodyOffsetTop());

    delayTestFinish(1000);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        assertEquals(top + margin, elem.getAbsoluteTop());
        assertEquals(left + margin, elem.getAbsoluteLeft());
        finishTest();
      }
    });
  }

  /**
   * scroll[Left|Top], scrollIntoView.
   */
  public void testGetAbsolutePositionWhenScrolled() {
    final DivElement outer = Document.get().createDivElement();
    final DivElement inner = Document.get().createDivElement();

    outer.getStyle().setProperty("position", "absolute");
    outer.getStyle().setProperty("top", "0px");
    outer.getStyle().setProperty("left", "0px");
    outer.getStyle().setProperty("overflow", "auto");
    outer.getStyle().setProperty("width", "200px");
    outer.getStyle().setProperty("height", "200px");

    inner.getStyle().setProperty("marginTop", "800px");
    inner.getStyle().setProperty("marginLeft", "800px");

    outer.appendChild(inner);
    Document.get().getBody().appendChild(outer);
    inner.setInnerText(":-)");
    inner.scrollIntoView();

    // Ensure that we are scrolled.
    assertTrue(outer.getScrollTop() > 0);
    assertTrue(outer.getScrollLeft() > 0);

    outer.setScrollLeft(0);
    outer.setScrollTop(0);

    // Ensure that we are no longer scrolled.
    assertEquals(outer.getScrollTop(), 0);
    assertEquals(outer.getScrollLeft(), 0);
  }

  /**
   * Tests that scrollLeft behaves as expected in RTL mode.
   */
  public void testScrollLeftInRtl() {
    final DivElement outer = Document.get().createDivElement();
    final DivElement inner = Document.get().createDivElement();

    outer.getStyle().setProperty("position", "absolute");
    outer.getStyle().setProperty("top", "0px");
    outer.getStyle().setProperty("left", "0px");
    outer.getStyle().setProperty("overflow", "auto");
    outer.getStyle().setProperty("width", "200px");
    outer.getStyle().setProperty("height", "200px");

    // Force scrolling on the outer div, because WebKit doesn't do this
    // correctly in RTL mode.
    outer.getStyle().setProperty("overflow", "scroll");

    inner.getStyle().setProperty("marginTop", "800px");
    inner.getStyle().setProperty("marginRight", "800px");

    outer.appendChild(inner);
    Document.get().getBody().appendChild(outer);
    inner.setInnerText(":-)");
    outer.setDir("rtl");

    // The important thing is that setting and retrieving scrollLeft values in
    // RTL mode works only for negative numbers, and that they round-trip
    // correctly.
    outer.setScrollLeft(-32);
    assertEquals(-32, outer.getScrollLeft());

    outer.setScrollLeft(32);
    assertEquals(0, outer.getScrollLeft());
  }

  /**
   * getParentElement.
   */
  public void testGetParent() {
    Element element = Document.get().getBody();
    int i = 0;
    while (i < 10 && element != null) {
      element = element.getParentElement();
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
   * firstChildElement, nextSiblingElement.
   */
  public void testChildElements() {
    Document doc = Document.get();
    DivElement parent = doc.createDivElement();
    DivElement div0 = doc.createDivElement();
    DivElement div1 = doc.createDivElement();

    parent.appendChild(doc.createTextNode("foo"));
    parent.appendChild(div0);
    parent.appendChild(doc.createTextNode("bar"));
    parent.appendChild(div1);

    Element fc = parent.getFirstChildElement();
    Element ns = fc.getNextSiblingElement();
    assertEquals(div0, fc);
    assertEquals(div1, ns);
  }

  /**
   * isOrHasChild.
   */
  public void testIsOrHasChild() {
    DivElement div = Document.get().createDivElement();
    DivElement childDiv = Document.get().createDivElement();

    assertFalse(div.isOrHasChild(childDiv));
    assertTrue(div.isOrHasChild(div));

    div.appendChild(childDiv);
    assertTrue(div.isOrHasChild(childDiv));
    assertFalse(childDiv.isOrHasChild(div));

    Document.get().getBody().appendChild(div);
    assertTrue(div.isOrHasChild(childDiv));
    assertTrue(div.isOrHasChild(div));
    assertFalse(childDiv.isOrHasChild(div));
  }

  /**
   * innerText.
   */
  public void testSetInnerText() {
    Document doc = Document.get();

    TableElement tableElem = doc.createTableElement();
    TableRowElement trElem = doc.createTRElement();
    TableCellElement tdElem = doc.createTDElement();
    tdElem.setInnerText("Some Table Heading Data");

    // Add a <em> element as a child to the td element
    Element emElem = doc.createElement("em");
    emElem.setInnerText("Some emphasized text");
    tdElem.appendChild(emElem);

    trElem.appendChild(tdElem);
    tableElem.appendChild(trElem);
    doc.getBody().appendChild(tableElem);
    tdElem.setInnerText(null);

    // Once we set the inner text on an element to null, all of the element's
    // child nodes should be deleted, including any text nodes, for all
    // supported browsers.
    assertTrue(tdElem.getChildNodes().getLength() == 0);
  }

  /**
   * innerHTML.
   */
  public void testSetInnerHTML() {
    DivElement div = Document.get().createDivElement();
    div.setInnerHTML("<button><img src='foo.gif'></button>");

    Element button = div.getFirstChildElement();
    Element img = button.getFirstChildElement();

    assertEquals("button", button.getTagName().toLowerCase());
    assertEquals("img", img.getTagName().toLowerCase());
    assertTrue(((ImageElement) img).getSrc().endsWith("foo.gif"));
  }

  /**
   * setProperty*, getProperty*.
   */
  public void testProperties() {
    DivElement div = Document.get().createDivElement();

    div.setPropertyString("foo", "bar");
    assertEquals("bar", div.getPropertyString("foo"));

    div.setPropertyInt("foo", 42);
    assertEquals(42, div.getPropertyInt("foo"));

    div.setPropertyBoolean("foo", true);
    div.setPropertyBoolean("bar", false);
    assertEquals(true, div.getPropertyBoolean("foo"));
    assertEquals(false, div.getPropertyBoolean("bar"));
  }

  /**
   * className, id, tagName, title, dir, lang.
   */
  public void testNativeProperties() {
    DivElement div = Document.get().createDivElement();

    assertEquals("div", div.getTagName().toLowerCase());

    div.setClassName("myClass");
    assertEquals(div.getClassName(), "myClass");

    div.setId("myId");
    assertEquals(div.getId(), "myId");

    div.setTitle("myTitle");
    assertEquals(div.getTitle(), "myTitle");

    div.setDir("rtl");
    assertEquals(div.getDir(), "rtl");

    div.setLang("fr-FR");
    assertEquals(div.getLang(), "fr-FR");
  }

  /**
   * style.
   */
  public void testStyle() {
    DivElement div = Document.get().createDivElement();

    div.getStyle().setProperty("color", "black");
    assertEquals("black", div.getStyle().getProperty("color"));

    div.getStyle().setPropertyPx("width", 42);
    assertEquals("42px", div.getStyle().getProperty("width"));
  }

  /**
   * Test that styles only allow camelCase.
   */
  public void testStyleCamelCase() {
    DivElement div = Document.get().createDivElement();

    // Use a camelCase property
    div.getStyle().setProperty("backgroundColor", "black");
    assertEquals("black", div.getStyle().getProperty("backgroundColor"));
    div.getStyle().setPropertyPx("marginLeft", 10);
    assertEquals("10px", div.getStyle().getProperty("marginLeft"));

    // Use a hyphenated style
    if (Style.class.desiredAssertionStatus()) {
      try {
        div.getStyle().setProperty("background-color", "red");
        fail("Expected assertion error: background-color should be in camelCase");
      } catch (AssertionError e) {
        // expected
      }
      try {
        div.getStyle().setPropertyPx("margin-left", 20);
        fail("Expected assertion error: margin-left should be in camelCase");
      } catch (AssertionError e) {
        // expected
      }
      try {
        div.getStyle().getProperty("margin-right");
        fail("Expected assertion error: margin-right should be in camelCase");
      } catch (AssertionError e) {
        // expected
      }
    }
  }

  /**
   * offset[Left|Top|Width|Height], offsetParent.
   */
  public void testOffsets() {
    DivElement outer = Document.get().createDivElement();
    DivElement middle = Document.get().createDivElement();
    DivElement inner = Document.get().createDivElement();

    Document.get().getBody().appendChild(outer);
    outer.appendChild(middle);
    middle.appendChild(inner);

    outer.getStyle().setProperty("position", "absolute");
    inner.getStyle().setProperty("position", "relative");
    inner.getStyle().setPropertyPx("left", 19);
    inner.getStyle().setPropertyPx("top", 23);
    inner.getStyle().setPropertyPx("width", 29);
    inner.getStyle().setPropertyPx("height", 31);

    assertEquals(outer, inner.getOffsetParent());
    assertEquals(19, inner.getOffsetLeft());
    assertEquals(23, inner.getOffsetTop());
    assertEquals(29, inner.getOffsetWidth());
    assertEquals(31, inner.getOffsetHeight());
  }

  /**
   * getElementsByTagName.
   */
  public void testGetElementsByTagName() {
    DivElement div = Document.get().createDivElement();
    div.setInnerHTML("<span><button>foo</button><span><button>bar</button></span></span>");

    NodeList<Element> nodes = div.getElementsByTagName("button");
    assertEquals(2, nodes.getLength());
    assertEquals("foo", nodes.getItem(0).getInnerText());
    assertEquals("bar", nodes.getItem(1).getInnerText());
  }

  /**
   * Tests HeadingElement.as() (it has slightly more complex assertion logic
   * than most).
   */
  public void testHeadingElementAs() {
    DivElement placeHolder = Document.get().createDivElement();

    for (int i = 0; i < 6; ++i) {
      placeHolder.setInnerHTML("<H" + (i + 1) + "/>");
      assertNotNull(HeadingElement.as(placeHolder.getFirstChildElement()));
    }

    if (!GWT.isScript()) {
      Element notHeading = Document.get().createDivElement();
      try {
        HeadingElement.as(notHeading);
        fail("Expected assertion failure");
      } catch (AssertionError e) {
        // this *should* happen.
      }
    }
  }
}
