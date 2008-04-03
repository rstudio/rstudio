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
 * Tests the {@link Node} class.
 */
public class NodeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * nodeType
   */
  public void testNodeType() {
    Document doc = Document.get();
    DivElement div = doc.createDivElement();
    Text txt0 = doc.createTextNode("foo");

    assertEquals(Node.DOCUMENT_NODE, doc.getNodeType());
    assertEquals(Node.ELEMENT_NODE, div.getNodeType());
    assertEquals(Node.TEXT_NODE, txt0.getNodeType());
  }

  /**
   * nodeName, nodeValue
   */
  public void testNodeNameAndValue() {
    Document doc = Document.get();
    DivElement div = doc.createDivElement();
    Text txt0 = doc.createTextNode("foo");

    assertEquals("div", div.getNodeName().toLowerCase());

    assertEquals("foo", txt0.getNodeValue());
    txt0.setNodeValue("bar");
    assertEquals("bar", txt0.getNodeValue());
  }

  /**
   * setAttribute, getAttribute, hasAttributes, hasAttribute
   */
  public void testAttributes() {
    Document doc = Document.get();
    DivElement div = doc.createDivElement();

    div.setAttribute("id", "myId");
    assertEquals("myId", div.getAttribute("id"));
  }

  /**
   * getParentNode, firstChild, lastChild, nextSibling, previousSibling
   */
  public void testParentAndSiblings() {
    Document doc = Document.get();
    BodyElement body = doc.getBody();

    // <div>foo<button/>bar</div>
    DivElement div = doc.createDivElement();
    Text txt0 = doc.createTextNode("foo");
    ButtonElement btn0 = doc.createButtonElement();
    Text txt1 = doc.createTextNode("bar");

    body.appendChild(div);
    div.appendChild(txt0);
    div.appendChild(btn0);
    div.appendChild(txt1);

    assertEquals(div, btn0.getParentNode());

    assertEquals(txt0, div.getFirstChild());
    assertEquals(txt1, div.getLastChild());
    assertEquals(btn0, txt0.getNextSibling());
    assertEquals(btn0, txt1.getPreviousSibling());
    assertEquals(null, txt0.getPreviousSibling());
    assertEquals(null, txt1.getNextSibling());
  }

  /**
   * ownerDocument
   */
  public void testOwnerDocument() {
    Document doc = Document.get();
    BodyElement body = doc.getBody();

    // <div>foo<button/>bar</div>
    DivElement div = doc.createDivElement();
    Text txt0 = doc.createTextNode("foo");
    ButtonElement btn0 = doc.createButtonElement();
    Text txt1 = doc.createTextNode("bar");

    body.appendChild(div);
    div.appendChild(txt0);
    div.appendChild(btn0);
    div.appendChild(txt1);

    // ownerDocument
    assertEquals(doc, div.getOwnerDocument());
    assertEquals(doc, txt0.getOwnerDocument());
  }

  /**
   * childNodes, hasChildNodes
   */
  public void testChildNodeList() {
    Document doc = Document.get();
    BodyElement body = doc.getBody();

    // <div>foo<button/>bar</div>
    DivElement div = doc.createDivElement();
    Text txt0 = doc.createTextNode("foo");
    ButtonElement btn0 = doc.createButtonElement();
    Text txt1 = doc.createTextNode("bar");

    body.appendChild(div);
    div.appendChild(txt0);
    div.appendChild(btn0);
    div.appendChild(txt1);

    NodeList<Node> children = div.getChildNodes();
    assertEquals(3, children.getLength());
    assertEquals(txt0, children.getItem(0));
    assertEquals(btn0, children.getItem(1));
    assertEquals(txt1, children.getItem(2));

    assertFalse(txt0.hasChildNodes());
    assertTrue(div.hasChildNodes());
  }

  /**
   * appendChild, insertBefore, removeChild, replaceChild
   */
  public void testAppendRemoveReplace() {
    Document doc = Document.get();
    BodyElement body = doc.getBody();

    // <div>foo<button/>bar</div>
    DivElement div = doc.createDivElement();
    Text txt0 = doc.createTextNode("foo");
    ButtonElement btn0 = doc.createButtonElement();
    Text txt1 = doc.createTextNode("bar");

    body.appendChild(div);
    div.appendChild(txt0);
    div.appendChild(btn0);
    div.appendChild(txt1);

    // appendChild, insertBefore
    ButtonElement btn1 = doc.createButtonElement();

    // <div>foo<button/>bar<button/></div>
    div.appendChild(btn1);
    assertEquals(btn1, div.getLastChild());

    // <div>foo<button/><button/>bar</div>
    div.insertBefore(btn1, txt1);
    assertEquals(4, div.getChildNodes().getLength());
    assertEquals(btn1, div.getChildNodes().getItem(2));

    // removeChild
    // <div>foo<button/>bar</div> (back to original)
    div.removeChild(btn1);
    assertEquals(3, div.getChildNodes().getLength());

    // replaceChild
    div.replaceChild(btn1, btn0);
    assertEquals(btn1, txt0.getNextSibling());
    assertEquals(btn1, txt1.getPreviousSibling());
  }
}
