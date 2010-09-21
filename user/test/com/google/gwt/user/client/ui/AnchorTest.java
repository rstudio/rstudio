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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;

/**
 * Tests for {@link Anchor}.
 */
public class AnchorTest extends GWTTestCase {
  private static final class TestClickHandler implements ClickHandler {
    private int clicks = 0;
    private Object lastSender;

    public void onClick(ClickEvent event) {
      clicks++;
      lastSender = event.getSource();
    }

    public int getClicks() {
      return clicks;
    }

    public Object getLastSender() {
      return lastSender;
    }
  }

  private static final String html = "<b>hello</b><i>world</i>";
  private static final String TEST_URL0 = "http://www.google.com/";
  private static final String TEST_URL1 = "http://code.google.com/";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testProperties() {
    Anchor anchor = new Anchor("foo", TEST_URL0);
    assertEquals("foo", anchor.getText());
    assertEquals("foo", anchor.getHTML());
    assertEquals(TEST_URL0, anchor.getHref());

    anchor.setText("bar");
    assertEquals("bar", anchor.getText());

    anchor.setHTML("baz");
    assertEquals("baz", anchor.getHTML());

    anchor.setHref(TEST_URL1);
    assertEquals(TEST_URL1, anchor.getHref());

    anchor.setDirection(HasDirection.Direction.RTL);
    assertEquals(HasDirection.Direction.RTL, anchor.getDirection());

    anchor.setWordWrap(true);
    assertEquals(true, anchor.getWordWrap());

    anchor.setTabIndex(42);
    assertEquals(42, anchor.getTabIndex());
  }

  @Deprecated
  private static final class TestClickListener implements ClickListener {
    private int clicks = 0;
    private Widget lastSender;

    public void onClick(Widget sender) {
      clicks++;
      lastSender = sender;
    }

    public int getClicks() {
      return clicks;
    }

    public Widget getLastSender() {
      return lastSender;
    }
  }

  public void testNoAttributes() {
    Anchor anchor = new Anchor();

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals(0, DOM.getChildCount(anchor.getElement()));

    final String[] attrs = new String[] {
        "href", "name", "id", "rel", "ref", "target"};
    for (String attribute : attrs) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  public void testSafeHtmlConstructors() {
    String href = "http://example.com/example.png";
    String target = "_blank";
    Anchor anchor1 = new Anchor(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html.toLowerCase(), anchor1.getHTML().toLowerCase());
    
    Anchor anchor2 = new Anchor(
        SafeHtmlUtils.fromSafeConstant(html), href, target);
    
    assertEquals(html, anchor2.getHTML().toLowerCase());
    assertEquals(href, anchor2.getHref());
    assertEquals(target, anchor2.getTarget());
  }

  public void testSetSafeHtml() {
    Anchor anchor = new Anchor("hello");
    anchor.setHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, anchor.getHTML().toLowerCase());
  }

  public void testScriptAnchor() {
    Anchor anchor = new Anchor("Foo");

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals("Foo", anchor.getText());
    assertAttributeHasValue("javascript:;", anchor.getElement(), "href");

    for (String attribute : new String[] {"name", "id", "rel", "ref", "target"}) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  public void testScriptAnchorWithHTML() {
    Anchor anchor = new Anchor("<span>Foo</span>", true);

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals("SPAN", DOM.getChild(anchor.getElement(), 0).getTagName());
    assertAttributeHasValue("javascript:;", anchor.getElement(), "href");

    for (String attribute : new String[] {"name", "id", "rel", "ref", "target"}) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  public void testEvents() {
    Anchor anchor = new Anchor("Trigger obscure JavaScript things");

    Panel p = getTestPanel();
    p.add(anchor);

    TestClickListener testListener = new TestClickListener();
    anchor.addClickListener(testListener);

    TestClickHandler handler = new TestClickHandler();
    anchor.addClickHandler(handler);

    assertEquals(0, testListener.getClicks());
    assertEquals(0, handler.getClicks());
    triggerEvent(anchor.getElement(), "click", false, "MouseEvents");
    assertEquals(1, testListener.getClicks());
    assertEquals(1, handler.getClicks());
    assertEquals(anchor, testListener.getLastSender());
    assertEquals(anchor, handler.getLastSender());
  }

  public void testLink() {
    Anchor anchor = new Anchor("Click me!", "http://nowhere.org/");

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals("Click me!", anchor.getText());
    assertAttributeHasValue("http://nowhere.org/", anchor.getElement(), "href");

    for (String attribute : new String[] {"name", "id", "rel", "ref", "target"}) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  public void testLinkWithHTML() {
    Anchor anchor = new Anchor("<span>Foo</span>", true,
        "http://still.nowhere.org/");

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals("SPAN", DOM.getChild(anchor.getElement(), 0).getTagName());
    assertTrue("<span>Foo</span>".equalsIgnoreCase(anchor.getHTML()));

    assertAttributeHasValue("http://still.nowhere.org/", anchor.getElement(),
        "href");

    for (String attribute : new String[] {"name", "id", "rel", "ref", "target"}) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  public void testLinkWithTarget() {
    Anchor anchor = new Anchor("Click me!",
        "http://and.now.a.word.from.our.sponsor.org/", "popup");

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals("Click me!", anchor.getText());
    assertAttributeHasValue("http://and.now.a.word.from.our.sponsor.org/",
        anchor.getElement(), "href");
    assertAttributeHasValue("popup", anchor.getElement(), "target");

    for (String attribute : new String[] {"name", "id", "rel", "ref"}) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  public void testLinkWithHTMLAndTarget() {
    Anchor anchor = new Anchor("<span>Foo</span>", true,
        "http://more.ads.com/", "_blank");

    Panel p = getTestPanel();
    p.add(anchor);

    assertEquals(1, DOM.getChildCount(p.getElement()));
    assertEquals("A", DOM.getChild(p.getElement(), 0).getTagName());
    assertEquals("SPAN", DOM.getChild(anchor.getElement(), 0).getTagName());
    assertTrue("<span>Foo</span>".equalsIgnoreCase(anchor.getHTML()));

    assertAttributeHasValue("http://more.ads.com/", anchor.getElement(), "href");
    assertAttributeHasValue("_blank", anchor.getElement(), "target");

    for (String attribute : new String[] {"name", "id", "rel", "ref"}) {
      assertAttributeNotPresent(attribute, anchor.getElement());
    }
  }

  /**
   * Tests that the getters interact with the generic setter correctly.
   */
  public void testGetterRoundtrip() {
    Anchor anchor = new Anchor();
    Panel p = getTestPanel();
    p.add(anchor);

    anchor.getElement().setAttribute("href",
        "http://yet.another.made.up.url.org/");
    assertEquals("http://yet.another.made.up.url.org/", anchor.getHref());

    anchor.getElement().setAttribute("target", "_blank");
    assertEquals("_blank", anchor.getTarget());

    anchor.getElement().setAttribute("name", "Marty");
    assertEquals("Marty", anchor.getName());

    anchor.getElement().setAttribute("tabIndex", "23");
    assertEquals(23, anchor.getTabIndex());
  }

  /**
   * Tests that the setters interact with the generic getter correctly.
   */
  public void testSetterRoundtrip() {
    Anchor anchor = new Anchor();
    Panel p = getTestPanel();
    p.add(anchor);

    anchor.setHref("http://duh.no.more.ideas.net/");
    assertEquals("http://duh.no.more.ideas.net/",
        anchor.getElement().getAttribute("href"));

    anchor.setTarget("_top");
    assertEquals("_top", anchor.getElement().getAttribute("target"));

    anchor.setName("Hieronymous");
    assertEquals("Hieronymous", anchor.getElement().getAttribute("name"));

    anchor.setName("Hieronymous");
    assertEquals("Hieronymous", anchor.getElement().getAttribute("name"));

    anchor.setTabIndex(42);
    assertEquals(42, anchor.getElement().getPropertyInt("tabIndex"));
  }

  /**
   * Constructs a simple panel for testing.
   * 
   * @return Panel, attached to the root panel.
   */
  private Panel getTestPanel() {
    Panel p = new FlowPanel();
    RootPanel.get().add(p);
    return p;
  }

  /**
   * Asserts that a given attribute is not present on an element.
   * 
   * @param element The element to check.
   * @param attribute The attribute to check.
   */
  private static void assertAttributeNotPresent(String attribute,
      Element element) {
    String value = element.getPropertyString(attribute);
    assertTrue(attribute + " not present", (value == null)
        || (value.equals("")));
  }

  /**
   * Asserts that a given attribute has the expected valued.
   * 
   * @param expected Expected value for the attribute.
   * @param element Element to check on.
   * @param attribute Attribute to check.
   */
  private static void assertAttributeHasValue(String expected, Element element,
      String attribute) {
    assertEquals("Attribute " + attribute + " has value '" + expected + "'",
        expected, element.getPropertyString(attribute));
  }

  /**
   * Triggers events in a cross-browser way.
   * 
   * TODO: Refactor this and other utility methods into a Utility class or
   * common base.
   * 
   * @param element the element on which to trigger the event
   * @param eventType the type of event to trigger
   * @param canBubble true if the event can bubble
   * @param eventClass the class of event
   */
  public native void triggerEvent(Element element, String eventType,
      boolean canBubble, String eventClass) /*-{
    // TODO: This is convenient for now, but could be a lot simpler if we added
    // a GWTtier API for event triggering.
    canBubble = (typeof(canBubble) == undefined) ? true : canBubble;
    if (element.fireEvent) {
      var evt = element.ownerDocument.createEventObject();
      element.fireEvent('on' + eventType, evt);
    } else {
      var evt = document.createEvent(eventClass);
      evt.initEvent(eventType, canBubble, true);
      element.dispatchEvent(evt);
    }
  }-*/;
}
