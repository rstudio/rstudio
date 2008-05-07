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

import com.google.gwt.dom.client.Node;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

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
    assertEquals("a", DOM.getElementAttribute(
        DOM.getParent(labelA.getElement()), "id"));
    assertEquals("b", DOM.getElementAttribute(
        DOM.getParent(labelB.getElement()), "id"));
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
    assertTrue(DOM.isOrHasChild(sp.getElement(), p.getElement()));
  }

  public void testAttachDetachOrder() {
    HTMLPanel p = new HTMLPanel("<div id='w00t'></div>");
    HasWidgetsTester.testAll(p, new Adder());
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

    assertTrue(DOM.isOrHasChild(fp.getElement(), hp.getElement()));
    assertTrue(DOM.getNextSibling(ba.getElement()) == hp.getElement());
    assertTrue(DOM.getNextSibling(hp.getElement()) == bb.getElement());
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

  public void testGetElementById() {
    HTMLPanel hp = new HTMLPanel("foo<div id='toFind'>bar</div>baz");

    // Get the element twice, once before and once after attachment.
    Element elem0 = hp.getElementById("toFind");
    RootPanel.get().add(hp);
    Element elem1 = hp.getElementById("toFind");

    // Make sure we got the same element in both cases.
    assertEquals(elem0, elem1);
  }
}
