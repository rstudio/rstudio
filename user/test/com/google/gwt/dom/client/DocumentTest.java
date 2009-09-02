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

import com.google.gwt.junit.client.GWTTestCase;

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

    assertEquals("a", doc.createAnchorElement().getTagName().toLowerCase());
    assertEquals("area", doc.createAreaElement().getTagName().toLowerCase());
    assertEquals("base", doc.createBaseElement().getTagName().toLowerCase());
    assertEquals("blockquote",
        doc.createBlockQuoteElement().getTagName().toLowerCase());
    assertEquals("br", doc.createBRElement().getTagName().toLowerCase());
    assertEquals("caption",
        doc.createCaptionElement().getTagName().toLowerCase());
    assertEquals("col", doc.createColElement().getTagName().toLowerCase());
    assertEquals("colgroup",
        doc.createColGroupElement().getTagName().toLowerCase());
    assertEquals("del", doc.createDelElement().getTagName().toLowerCase());
    assertEquals("div", doc.createDivElement().getTagName().toLowerCase());
    assertEquals("dl", doc.createDLElement().getTagName().toLowerCase());
    assertEquals("fieldset",
        doc.createFieldSetElement().getTagName().toLowerCase());
    assertEquals("form", doc.createFormElement().getTagName().toLowerCase());
    assertEquals("frame", doc.createFrameElement().getTagName().toLowerCase());
    assertEquals("frameset",
        doc.createFrameSetElement().getTagName().toLowerCase());
    assertEquals("head", doc.createHeadElement().getTagName().toLowerCase());
    assertEquals("h1", doc.createHElement(1).getTagName().toLowerCase());
    assertEquals("hr", doc.createHRElement().getTagName().toLowerCase());
    assertEquals("iframe", doc.createIFrameElement().getTagName().toLowerCase());
    assertEquals("img", doc.createImageElement().getTagName().toLowerCase());
    assertEquals("ins", doc.createInsElement().getTagName().toLowerCase());
    assertEquals("label", doc.createLabelElement().getTagName().toLowerCase());
    assertEquals("legend", doc.createLegendElement().getTagName().toLowerCase());
    assertEquals("li", doc.createLIElement().getTagName().toLowerCase());
    assertEquals("link", doc.createLinkElement().getTagName().toLowerCase());
    assertEquals("map", doc.createMapElement().getTagName().toLowerCase());
    assertEquals("meta", doc.createMetaElement().getTagName().toLowerCase());
    assertEquals("object", doc.createObjectElement().getTagName().toLowerCase());
    assertEquals("ol", doc.createOLElement().getTagName().toLowerCase());
    assertEquals("optgroup",
        doc.createOptGroupElement().getTagName().toLowerCase());
    assertEquals("option", doc.createOptionElement().getTagName().toLowerCase());
    assertEquals("param", doc.createParamElement().getTagName().toLowerCase());
    assertEquals("p", doc.createPElement().getTagName().toLowerCase());
    assertEquals("pre", doc.createPreElement().getTagName().toLowerCase());
    assertEquals("q", doc.createQElement().getTagName().toLowerCase());
    assertEquals("script", doc.createScriptElement().getTagName().toLowerCase());
    assertEquals("select", doc.createSelectElement().getTagName().toLowerCase());
    assertEquals("select",
        doc.createSelectElement(false).getTagName().toLowerCase());
    assertEquals("span", doc.createSpanElement().getTagName().toLowerCase());
    assertEquals("style", doc.createStyleElement().getTagName().toLowerCase());
    assertEquals("table", doc.createTableElement().getTagName().toLowerCase());
    assertEquals("tbody", doc.createTBodyElement().getTagName().toLowerCase());
    assertEquals("td", doc.createTDElement().getTagName().toLowerCase());
    assertEquals("textarea",
        doc.createTextAreaElement().getTagName().toLowerCase());
    assertEquals("tfoot", doc.createTFootElement().getTagName().toLowerCase());
    assertEquals("thead", doc.createTHeadElement().getTagName().toLowerCase());
    assertEquals("th", doc.createTHElement().getTagName().toLowerCase());
    assertEquals("title", doc.createTitleElement().getTagName().toLowerCase());
    assertEquals("tr", doc.createTRElement().getTagName().toLowerCase());
    assertEquals("ul", doc.createULElement().getTagName().toLowerCase());

    assertEquals("button",
        doc.createPushButtonElement().getTagName().toLowerCase());
    assertEquals("button",
        doc.createResetButtonElement().getTagName().toLowerCase());
    assertEquals("button",
        doc.createSubmitButtonElement().getTagName().toLowerCase());

    assertEquals("button",
        doc.createPushButtonElement().getType().toLowerCase());
    assertEquals("reset",
        doc.createResetButtonElement().getType().toLowerCase());
    assertEquals("submit",
        doc.createSubmitButtonElement().getType().toLowerCase());

    assertEquals("input",
        doc.createCheckInputElement().getTagName().toLowerCase());
    assertEquals("input",
        doc.createFileInputElement().getTagName().toLowerCase());
    assertEquals("input",
        doc.createHiddenInputElement().getTagName().toLowerCase());
    assertEquals("input",
        doc.createImageInputElement().getTagName().toLowerCase());
    assertEquals("input",
        doc.createPasswordInputElement().getTagName().toLowerCase());
    assertEquals("input",
        doc.createRadioInputElement("foo").getTagName().toLowerCase());
    assertEquals("input",
        doc.createTextInputElement().getTagName().toLowerCase());

    assertEquals("button",
        doc.createButtonInputElement().getType().toLowerCase());
    assertEquals("checkbox",
        doc.createCheckInputElement().getType().toLowerCase());
    assertEquals("file", doc.createFileInputElement().getType().toLowerCase());
    assertEquals("hidden",
        doc.createHiddenInputElement().getType().toLowerCase());
    assertEquals("image", doc.createImageInputElement().getType().toLowerCase());
    assertEquals("password",
        doc.createPasswordInputElement().getType().toLowerCase());
    assertEquals("radio",
        doc.createRadioInputElement("foo").getType().toLowerCase());
    assertEquals("reset", doc.createResetInputElement().getType().toLowerCase());
    assertEquals("submit",
        doc.createSubmitInputElement().getType().toLowerCase());
    assertEquals("text", doc.createTextInputElement().getType().toLowerCase());
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
}
