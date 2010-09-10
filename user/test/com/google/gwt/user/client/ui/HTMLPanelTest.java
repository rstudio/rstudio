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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the HTMLPanel widget.
 */
public class HTMLPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((HTMLPanel) container).add(child, "w00t");
    }
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Tests {@link HTMLPanel#add(Widget, String)}.
   */
  public void testAddToElementWithId() {
    Label labelA = new Label("A"), labelB = new Label("B");
    HTMLPanel p = new HTMLPanel("<div id=\"a\"></div><div id=\"b\"></div>");
    p.add(labelA, "a");
    p.add(labelB, "b");
    // Ensure that both Label's have the correct parent.
    assertEquals("a", labelA.getElement().getParentElement().getId());
    assertEquals("b", labelB.getElement().getParentElement().getId());
  }

  /**
   * Tests {@link HTMLPanel#add(Widget, Element)}.
   */
  public void testAddToElement() {
    Label labelA = new Label("A"), labelB = new Label("B");
    HTMLPanel p = new HTMLPanel("<div class=\"a\"></div><div class=\"b\"></div>");
    Element first = p.getElement().getFirstChildElement();
    Element second = first.getNextSiblingElement();
    
    p.add(labelA, first);
    p.add(labelB, second);
    // Ensure that both Label's have the correct parent.
    assertEquals("a", labelA.getElement().getParentElement().getClassName());
    assertEquals("b", labelB.getElement().getParentElement().getClassName());
  }

  /**
   * This is meant to catch an issue created by a faulty optimization. To
   * optimize add() when the HTMLPanel is unattached, we would originally
   * move its element to a hidden div so that getElementById() would work.
   * Unfortunately, we didn't move it back to its original parent, causing
   * a problem in the case described in this test.
   */
  public void testAddPartiallyAttached() {
    SimplePanel sp = new SimplePanel();
    HTMLPanel p = new HTMLPanel("<div id='foo'></div>");

    // Add the HTMLPanel to another panel before adding the button.
    sp.add(p);

    // Add a button the HTMLPanel, causing the panel's element to be attached
    // to the DOM.
    p.add(new Button("foo"), "foo");

    // If all goes well, the HTMLPanel's element should still be properly
    // connected to the SimplePanel's element.
    assertTrue(sp.getElement().isOrHasChild(p.getElement()));
  }

  /**
   * Tests child attachment order using {@link HasWidgetsTester}.
   */
  public void testAttachDetachOrder() {
    HTMLPanel p = new HTMLPanel("<div id='w00t'></div>");
    HasWidgetsTester.testAll(p, new Adder(), true);
  }

  /**
   * Ensures that attachToDomAndGetElement() puts the HTMLPanel back exactly
   * where it was in the DOM originally.
   */
  public void testAttachDoesntMangleChildOrder() {
    FlowPanel fp = new FlowPanel();

    Button ba = new Button("before");
    Button bb = new Button("after");

    HTMLPanel hp = new HTMLPanel("<div id='foo'></div>");

    fp.add(ba);
    fp.add(hp);
    fp.add(bb);

    hp.add(new Button("foo"), "foo");

    assertTrue(fp.getElement().isOrHasChild(hp.getElement()));
    assertTrue(ba.getElement().getNextSibling() == hp.getElement());
    assertTrue(hp.getElement().getNextSibling() == bb.getElement());
  }

  /**
   * Ensures that addAndReplaceChild() puts the widget in exactly the right place in the DOM.
   */
  public void testAddAndReplaceElement() {
    HTMLPanel hp = new HTMLPanel("<div id='parent'>foo<span id='placeholder'></span>bar</div>");

    Button button = new Button("my button");
    hp.addAndReplaceElement(button, "placeholder");

    assertEquals("parent", button.getElement().getParentElement().getId());

    Node prev = button.getElement().getPreviousSibling();
    assertEquals("foo", prev.getNodeValue());

    Node next = button.getElement().getNextSibling();
    assertEquals("bar", next.getNodeValue());
  }

  /**
   * Ensures that addAndReplaceChild() puts the widget in exactly the right place in the DOM.
   */
  @SuppressWarnings("deprecation")
  public void testAddAndReplaceElementForUserElement() {

    HTMLPanel hp = new HTMLPanel("<div id='parent'>foo<span id='placeholder'></span>bar</div>");

    RootPanel.get().add(hp);
    com.google.gwt.user.client.Element placeholder = hp.getElementById("placeholder");

    Button button = new Button("my button");
    hp.addAndReplaceElement(button, placeholder);

    assertEquals("parent", button.getElement().getParentElement().getId());

    Node prev = button.getElement().getPreviousSibling();
    assertEquals("foo", prev.getNodeValue());

    Node next = button.getElement().getNextSibling();
    assertEquals("bar", next.getNodeValue());
  }

  /**
   * Ensures that addAndReplaceChild() puts the widget in exactly the right place in the DOM.
   */
  public void testAddAndReplaceElementForElement() {
    HTMLPanel hp = new HTMLPanel("<div id='parent'>foo<span id='placeholder'></span>bar</div>");

    RootPanel.get().add(hp);
    Element placeholder = hp.getElementById("placeholder");

    Button button = new Button("my button");
    hp.addAndReplaceElement(button, placeholder);

    assertEquals("parent", button.getElement().getParentElement().getId());

    Node prev = button.getElement().getPreviousSibling();
    assertEquals("foo", prev.getNodeValue());

    Node next = button.getElement().getNextSibling();
    assertEquals("bar", next.getNodeValue());
  }

  /**
   * Tests table root tag.
   */
  public void testCustomRootTagAsTable() {
    HTMLPanel hp = new HTMLPanel("table",
        "<tr><td>Hello <span id='labelHere'></span></td></tr>");
    InlineLabel label = new InlineLabel("World");
    hp.addAndReplaceElement(label, "labelHere");

    Element parent = label.getElement().getParentElement();
    assertEquals("td", parent.getTagName().toLowerCase());

    parent = parent.getParentElement();
    assertEquals("tr", parent.getTagName().toLowerCase());

    while (parent != null && parent != hp.getElement()) {
      parent = parent.getParentElement();
    }

    assertNotNull(parent);
    assertEquals("table", parent.getTagName().toLowerCase());
  }

  /**
   * Ensure that {@link HTMLPanel#getElementById(String)} behaves properly in
   * both attached and unattached states.
   */
  public void testGetElementById() {
    HTMLPanel hp = new HTMLPanel("foo<div id='toFind'>bar</div>baz");

    // Get the element twice, once before and once after attachment.
    Element elem0 = hp.getElementById("toFind");
    RootPanel.get().add(hp);
    Element elem1 = hp.getElementById("toFind");

    // Make sure we got the same element in both cases.
    assertEquals(elem0, elem1);
  }

  /**
   * Tests that the HTMLPanel's element is not moved from its original location
   * when {@link HTMLPanel#add(Widget, String)} is called on it while it is
   * unattached.
   */
  public void testElementIsUnmoved() {
    HTMLPanel unattached = new HTMLPanel("<div id='unattached'></div>");
    HTMLPanel attached = new HTMLPanel("<div id='attached'></div>");

    RootPanel.get().add(attached);

    Element unattachedParentElem = unattached.getElement().getParentElement();
    Element attachedParentElem = attached.getElement().getParentElement();

    unattached.add(new Button("unattached"), "unattached");
    attached.add(new Button("attached"), "attached");

    assertEquals("Unattached's parent element should be unaffected",
        unattachedParentElem, unattached.getElement().getParentElement());
    assertEquals("Unattached's parent element should be unaffected",
        attachedParentElem, attached.getElement().getParentElement());
  }
}

