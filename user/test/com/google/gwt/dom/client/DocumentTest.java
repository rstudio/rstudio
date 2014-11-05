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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Locale;

/**
 * Tests the {@link Document} class.
 */
public class DocumentTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  // create*Element
  public void testElementCreators() {
    Document doc = Document.get();

    assertEquals("a", doc.createAnchorElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("area", doc.createAreaElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("base", doc.createBaseElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("blockquote",
        doc.createBlockQuoteElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("br", doc.createBRElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("caption",
        doc.createCaptionElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("col", doc.createColElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("colgroup",
        doc.createColGroupElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("del", doc.createDelElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("div", doc.createDivElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("dl", doc.createDLElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("fieldset",
        doc.createFieldSetElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("form", doc.createFormElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("frame", doc.createFrameElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("frameset",
        doc.createFrameSetElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("head", doc.createHeadElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("h1", doc.createHElement(1).getTagName().toLowerCase(Locale.ROOT));
    assertEquals("hr", doc.createHRElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("iframe", doc.createIFrameElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("img", doc.createImageElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("ins", doc.createInsElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("label", doc.createLabelElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("legend", doc.createLegendElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("li", doc.createLIElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("link", doc.createLinkElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("map", doc.createMapElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("meta", doc.createMetaElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("object", doc.createObjectElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("ol", doc.createOLElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("optgroup",
        doc.createOptGroupElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("option", doc.createOptionElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("param", doc.createParamElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("p", doc.createPElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("pre", doc.createPreElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("q", doc.createQElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("script", doc.createScriptElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("select", doc.createSelectElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("select",
        doc.createSelectElement(false).getTagName().toLowerCase(Locale.ROOT));
    assertEquals("span", doc.createSpanElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("style", doc.createStyleElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("table", doc.createTableElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("tbody", doc.createTBodyElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("td", doc.createTDElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("textarea",
        doc.createTextAreaElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("tfoot", doc.createTFootElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("thead", doc.createTHeadElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("th", doc.createTHElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("title", doc.createTitleElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("tr", doc.createTRElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("ul", doc.createULElement().getTagName().toLowerCase(Locale.ROOT));

    assertEquals("button",
        doc.createPushButtonElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("button",
        doc.createResetButtonElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("button",
        doc.createSubmitButtonElement().getTagName().toLowerCase(Locale.ROOT));

    assertEquals("button",
        doc.createPushButtonElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("reset",
        doc.createResetButtonElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("submit",
        doc.createSubmitButtonElement().getType().toLowerCase(Locale.ROOT));

    assertEquals("input",
        doc.createCheckInputElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("input",
        doc.createFileInputElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("input",
        doc.createHiddenInputElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("input",
        doc.createImageInputElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("input",
        doc.createPasswordInputElement().getTagName().toLowerCase(Locale.ROOT));
    assertEquals("input",
        doc.createRadioInputElement("foo").getTagName().toLowerCase(Locale.ROOT));
    assertEquals("input",
        doc.createTextInputElement().getTagName().toLowerCase(Locale.ROOT));

    assertEquals("button",
        doc.createButtonInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("checkbox",
        doc.createCheckInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("file", doc.createFileInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("hidden",
        doc.createHiddenInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("image", doc.createImageInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("password",
        doc.createPasswordInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("radio",
        doc.createRadioInputElement("foo").getType().toLowerCase(Locale.ROOT));
    assertEquals("reset", doc.createResetInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("submit",
        doc.createSubmitInputElement().getType().toLowerCase(Locale.ROOT));
    assertEquals("text", doc.createTextInputElement().getType().toLowerCase(Locale.ROOT));
  }

  /**
   * getElementById, getElementsByTagName.
   */
  public void testGetElements() {
    Document doc = Document.get();

    DivElement div = doc.createDivElement();
    doc.getBody().appendChild(div);

    div.setInnerHTML("<span><button id='foo'>foo</button><span><button>bar</button></span></span>");

    NodeList<Element> nodes = doc.getElementsByTagName("button");
    assertEquals(2, nodes.getLength());
    assertEquals("foo", nodes.getItem(0).getInnerText());
    assertEquals("bar", nodes.getItem(1).getInnerText());

    Element foo = doc.getElementById("foo");
    assertEquals("foo", foo.getId());
  }

  /**
   * domain, referrer, title, url.
   */
  public void testProperties() {
    Document doc = Document.get();

    assertTrue(doc.getURL().startsWith("http"));
    // TODO: referrer
    // TODO: domain

    doc.setTitle("myTitle");
    assertEquals("myTitle", doc.getTitle());
  }

  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testScrollLeft() {
    Document doc = Document.get();
    DivElement element = doc.createDivElement();
    element.setInnerHTML("<div style='width: 5000px; height: 5000px'></div>");
    doc.getBody().appendChild(element);

    doc.setScrollLeft(15);
    assertEquals(15, doc.getScrollLeft());
  }

  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testScrollTop() {
    Document doc = Document.get();
    DivElement element = doc.createDivElement();
    element.setInnerHTML("<div style='width: 5000px; height: 5000px'></div>");
    doc.getBody().appendChild(element);

    doc.setScrollTop(15);
    assertEquals(15, doc.getScrollTop());
  }
}
