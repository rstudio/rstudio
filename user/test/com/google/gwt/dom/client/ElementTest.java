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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Element tests (many stolen from DOMTest).
 */
public class ElementTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  public void testAddRemoveReplaceClassName() {
    DivElement div = Document.get().createDivElement();

    div.setClassName("foo");
    assertEquals("foo", div.getClassName());

    div.addClassName("bar");
    assertEquals("foo bar", div.getClassName());

    div.addClassName("baz");
    assertEquals("foo bar baz", div.getClassName());

    div.replaceClassName("bar", "tintin");
    assertTrue(div.getClassName().contains("tintin"));
    assertFalse(div.getClassName().contains("bar"));
  }

  /**
   * firstChildElement, nextSiblingElement, previousSiblingElement.
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
    Element ps = ns.getPreviousSiblingElement();
    assertEquals(div0, fc);
    assertEquals(div1, ns);
    assertEquals(div0, ps);

    assertNull(fc.getPreviousSiblingElement());
    assertNull(ns.getNextSiblingElement());
  }

  /**
   * Test round-trip of the 'disabled' property.
   */
  public void testDisabled() {
    ButtonElement button = Document.get().createPushButtonElement();
    assertFalse(button.isDisabled());
    button.setDisabled(true);
    assertTrue(button.isDisabled());

    InputElement input = Document.get().createTextInputElement();
    assertFalse(input.isDisabled());
    input.setDisabled(true);
    assertTrue(input.isDisabled());
    
    SelectElement select = Document.get().createSelectElement();
    assertFalse(select.isDisabled());
    select.setDisabled(true);
    assertTrue(select.isDisabled());
    
    OptGroupElement optgroup = Document.get().createOptGroupElement();
    assertFalse(optgroup.isDisabled());
    optgroup.setDisabled(true);
    assertTrue(optgroup.isDisabled());
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

  public void testEmptyClassNameAssertion() {
    DivElement div = Document.get().createDivElement();

    if (getClass().desiredAssertionStatus()) {
      div.setClassName("primary");
      try {
        div.addClassName("");
        fail();
      } catch (AssertionError e) {
        // This *should* throw.
      }

      try {
        div.addClassName(" ");
        fail();
      } catch (AssertionError e) {
        // This *should* throw.
      }

      try {
        div.addClassName(null);
        fail();
      } catch (AssertionError e) {
        // This *should* throw.
      }

      try {
        div.removeClassName("");
        fail();
      } catch (AssertionError e) {
        // This *should* throw.
      }

      try {
        div.removeClassName(" ");
        fail();
      } catch (AssertionError e) {
        // This *should* throw.
      }

      try {
        div.removeClassName(null);
        fail();
      } catch (AssertionError e) {
        // This *should* throw.
      }

      assertEquals("primary", div.getClassName());
    }
  }

  /**
   * getAbsolute[Left|Top|Right|Bottom].
   */
  public void testGetAbsolutePosition() {
    final int border = 8;
    final int margin = 9;
    final int padding = 10;

    final int top = 15;
    final int left = 14;
    final int width = 128;
    final int height = 64;

    final Document doc = Document.get();
    final DivElement elem = doc.createDivElement();
    doc.getBody().appendChild(elem);

    elem.getStyle().setProperty("position", "absolute");
    elem.getStyle().setProperty("border", border + "px solid #000");
    elem.getStyle().setProperty("padding", padding + "px");
    elem.getStyle().setProperty("margin", margin + "px");

    elem.getStyle().setPropertyPx("top", top - doc.getBodyOffsetLeft());
    elem.getStyle().setPropertyPx("left", left - doc.getBodyOffsetTop());
    elem.getStyle().setPropertyPx("width", width);
    elem.getStyle().setPropertyPx("height", height);

    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        int absLeft = left + margin;
        int absTop = top + margin;
        int interiorDecorations = (border * 2) + (padding * 2);

        assertEquals(absLeft, elem.getAbsoluteLeft());
        assertEquals(absTop, elem.getAbsoluteTop());

        if (isIE6or7() && !doc.isCSS1Compat()) {
          // In IE/quirk, the interior decorations are considered part of the
          // width/height, so there's no need to account for them here.
          assertEquals(absLeft + width, elem.getAbsoluteRight());
          assertEquals(absTop + height, elem.getAbsoluteBottom());
        } else {
          assertEquals(absLeft + width + interiorDecorations,
              elem.getAbsoluteRight());
          assertEquals(absTop + height + interiorDecorations,
              elem.getAbsoluteBottom());
        }
      }
    });
  }

  /**
   * scroll[Left|Top], getAbsolute[Left|Top].
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testGetAbsolutePositionWhenBodyScrolled() {
    Document doc = Document.get();
    BodyElement body = doc.getBody();

    DivElement div = doc.createDivElement();
    body.appendChild(div);

    div.setInnerText("foo");
    div.getStyle().setPosition(Position.ABSOLUTE);
    div.getStyle().setLeft(1000, Unit.PX);
    div.getStyle().setTop(1000, Unit.PX);

    DivElement fixedDiv = doc.createDivElement();
    body.appendChild(fixedDiv);
    fixedDiv.setInnerText("foo");
    fixedDiv.getStyle().setPosition(Position.FIXED);

    // Get the absolute position of the element when the body is unscrolled.
    int absLeft = div.getAbsoluteLeft();
    int absTop = div.getAbsoluteTop();

    // Scroll the body as far down and to the right as possible.
    body.setScrollLeft(10000);
    body.setScrollTop(10000);

    // Make sure the absolute position hasn't changed (this has turned out to
    // be a common error in getAbsoluteLeft/Top() implementations).
    //
    // HACK: Firefox 2 has a bug that causes its getBoxObjectFor() to become
    // off-by-one at times when scrolling. It's not clear how to make this go
    // away, and doesn't seem to be worth the trouble to implement
    // getAbsoluteLeft/Top() yet again for FF2.
    assertTrue(Math.abs(absLeft - div.getAbsoluteLeft()) <= 1);
    assertTrue(Math.abs(absTop - div.getAbsoluteTop()) <= 1);

    // Ensure that the 'position:fixed' div's absolute position includes the
    // body's scroll position.
    //
    // Don't do this on IE6/7, which doesn't support position:fixed.
    if (!isIE6or7()) {
      assertTrue(fixedDiv.getAbsoluteLeft() >= body.getScrollLeft());
      assertTrue(fixedDiv.getAbsoluteTop() >= body.getScrollTop());
    }
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

  public void testHasAttribute() {
    DivElement div = Document.get().createDivElement();

    // Assert that a raw element doesn't incorrectly report that it has any
    // unspecified built-in attributes (this is a problem on IE<8 if you're not
    // careful in implementing hasAttribute()).
    assertFalse(div.hasAttribute("class"));
    assertFalse(div.hasAttribute("style"));
    assertFalse(div.hasAttribute("title"));
    assertFalse(div.hasAttribute("id"));

    // Ensure that setting HTML-defined attributes is properly reported by
    // hasAttribute().
    div.setId("foo");
    assertTrue(div.hasAttribute("id"));

    // Ensure that setting *custom* attributes is properly reported by
    // hasAttribute().
    assertFalse(div.hasAttribute("foo"));
    div.setAttribute("foo", "bar");
    assertTrue(div.hasAttribute("foo"));

    // Ensure that a null attribute argument always returns null.
    assertFalse(div.hasAttribute(null));
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

    if (getClass().desiredAssertionStatus()) {
      Element notHeading = Document.get().createDivElement();
      try {
        HeadingElement.as(notHeading);
        fail("Expected assertion failure");
      } catch (AssertionError e) {
        // this *should* happen.
      }
    }
  }

  /**
   * Tests Element.is() and Element.as().
   */
  public void testIsAndAs() {
    assertFalse(Element.is(Document.get()));

    Node div = Document.get().createDivElement();
    assertTrue(Element.is(div));
    assertEquals("div", Element.as(div).getTagName().toLowerCase());

    // Element.is(null) is allowed and should return false.
    assertFalse(Element.is(null));
  }

  /**
   * Document.createElement('ns:tag'), getTagName().
   */
  public void testNamespaces() {
    Element elem = Document.get().createElement("myns:elem");
    assertEquals("myns:elem", elem.getTagName().toLowerCase());
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

    Object obj = new Object();
    div.setPropertyObject("baz", obj);
    assertEquals(obj, div.getPropertyObject("baz"));

    JavaScriptObject jso = createTrivialJSO();
    div.setPropertyJSO("tintin", jso);
    assertEquals(jso, div.getPropertyJSO("tintin"));
  }

  /**
   * scroll[Left|Top], scrollIntoView.
   */
  @DoNotRunWith({Platform.HtmlUnitLayout})
  public void testScrollIntoView() {
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
   * Failed in all modes due to HtmlUnit bug:
   * https://sourceforge.net/tracker/?func=detail&aid=2941255&group_id=47038&atid=448266
   */
  @DoNotRunWith({Platform.HtmlUnitLayout})
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

    inner.getStyle().setProperty("position", "absolute");
    inner.getStyle().setProperty("top", "0px");
    inner.getStyle().setProperty("left", "0px");
    inner.getStyle().setProperty("right", "0px");
    inner.getStyle().setProperty("marginTop", "800px");
    inner.getStyle().setProperty("marginRight", "800px");

    outer.appendChild(inner);
    Document.get().getBody().appendChild(outer);
    outer.setDir("rtl");

    // FF2 does not render scroll bars reliably in RTL, so we set a large
    // content to force the scroll bars.
    String content = "ssssssssssssssssssssssssssssssssssssssssssssssssssss"
        + "sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss"
        + "sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss";
    inner.setInnerText(content);

    // The important thing is that setting and retrieving scrollLeft values in
    // RTL mode works only for negative numbers, and that they round-trip
    // correctly.
    outer.setScrollLeft(-32);
    assertEquals(-32, outer.getScrollLeft());

    outer.setScrollLeft(32);
    assertEquals(0, outer.getScrollLeft());
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

  private native JavaScriptObject createTrivialJSO() /*-{
    return {};
  }-*/;

  // Stolen from UserAgentPropertyGenerator
  private native boolean isIE6or7() /*-{
    var ua = navigator.userAgent.toLowerCase();
    if (ua.indexOf("msie") != -1) {
      if ($doc.documentMode >= 8) {
        return false;
      }
      return true;
    }
    return false;
  }-*/;
}
