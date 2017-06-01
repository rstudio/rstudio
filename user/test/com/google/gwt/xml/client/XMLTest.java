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
package com.google.gwt.xml.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.xml.client.impl.DOMParseException;
import com.google.gwt.xml.client.impl.XMLParserImplSafari;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class poorly tests all the methods in the GWT XML parser.
 */
public class XMLTest extends GWTTestCase {
  private static List<Node> asList(Node[] nodes) {
    return Arrays.asList(nodes);
  }

  private static void assertAttributeMapEquals(NamedNodeMap listA,
      NamedNodeMap listB) {
    // This is only for checking attribute maps.
    assertEquals(listA.getLength(), listB.getLength());
    for (int i = 0, n = listA.getLength(); i < n; ++i) {
      final Node itemA = listA.item(i);
      final Node itemB = listB.getNamedItem(itemA.getNodeName());
      assertNotNull(itemB);
      assertEquals(itemA.getNodeType(), itemB.getNodeType());
      assertEquals(itemA.getNodeValue(), itemB.getNodeValue());
    }
  }

  private static void assertCharacterDataEquals(CharacterData charDataA,
      CharacterData charDataB) {
    assertEquals(charDataA.getData(), charDataB.getData());
  }

  private static void assertDocumentEquals(Document docA, Document docB) {
    assertNodeListEquals(docA.getChildNodes(), docB.getChildNodes());
  }

  private static void assertElementEquals(Element elemA, Element elemB) {
    assertEquals(elemA.getNodeName(), elemB.getNodeName());
    assertAttributeMapEquals(elemA.getAttributes(), elemB.getAttributes());
    assertNodeListEquals(elemA.getChildNodes(), elemB.getChildNodes());
  }

  private static void assertNodeEquals(Node nodeA, Node nodeB) {
    assertNotNull(nodeA);
    assertNotNull(nodeB);
    final int typeA = nodeA.getNodeType();
    final int typeB = nodeB.getNodeType();
    assertEquals(typeA, typeB);
    switch (typeA) {
      case Node.ELEMENT_NODE:
        assertElementEquals((Element) nodeA, (Element) nodeB);
        break;
      case Node.COMMENT_NODE:
      case Node.CDATA_SECTION_NODE:
      case Node.TEXT_NODE:
        assertCharacterDataEquals((CharacterData) nodeA, (CharacterData) nodeB);
        break;
      case Node.PROCESSING_INSTRUCTION_NODE:
        assertProcessingInstructionEquals((ProcessingInstruction) nodeA,
            (ProcessingInstruction) nodeB);
        break;
      default:
        fail("Unexpected node type: " + nodeA.toString());
        break;
    }
  }

  private static void assertNodeListEquals(NodeList listA, NodeList listB) {
    final int sizeA = listA.getLength();
    final int sizeB = listB.getLength();
    assertEquals(sizeA, sizeB);
    for (int i = 0, n = sizeA; i < n; ++i) {
      assertNodeEquals(listA.item(i), listB.item(i));
    }
  }

  private static void assertProcessingInstructionEquals(
      ProcessingInstruction piA, ProcessingInstruction piB) {
    assertEquals(piA.getData(), piB.getData());
  }

  private static Document createTestDocument() {
    Document d = XMLParser.createDocument();
    Element top = d.createElement("doc");
    top.setAttribute("fluffy", "true");
    top.setAttribute("numAttributes", "2");
    d.appendChild(top);
    ProcessingInstruction commentBefore = d.createProcessingInstruction(
        "target", "some data");
    d.insertBefore(commentBefore, top);
    Comment commentAfter = d.createComment("after the element");
    d.insertBefore(commentAfter, null);
    for (int i = 0; i < 3; i++) {
      Element e = d.createElement("e" + i);
      e.setAttribute("id", "e" + i + "Id");
      top.appendChild(e);
    }
    Element deep = d.createElement("deep");
    top.getFirstChild().appendChild(deep);
    deep.setAttribute("depth", "1 foot");
    Element deep2 = d.createElement("deep");
    deep2.setAttribute("depth", "2 feet");
    top.getFirstChild().getFirstChild().appendChild(deep2);

    top.appendChild(d.createTextNode("0123456789"));
    top.appendChild(d.createTextNode("abcdefghij"));
    top.appendChild(d.createElement("e4"));
    top.appendChild(d.createCDATASection("klmnopqrst"));
    return d;
  }
  
  /**
   * Returns the module name for GWT unit test running.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.xml.XML";
  }

  public void testAttr() {
    Document d = createTestDocument();
    Element de = d.getDocumentElement();
    de.setAttribute("created", "true");
    assertEquals("true", de.getAttribute("created"));
    de.setAttribute("set", "toAValue");
    assertEquals("toAValue", de.getAttribute("set"));
    assertTrue(de.getAttributeNode("set").getSpecified());
    assertEquals(de.getAttributeNode("unset"), null);
  }

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testCreate() {
    Document d = XMLParser.createDocument();
    CDATASection createCDATA = d.createCDATASection("sampl<<< >>e data");
    Comment createComment = d.createComment("a sample comment");
    DocumentFragment createDocumentFragment = d.createDocumentFragment();
    Element elementWithChildren = d.createElement("elementWithChildren");
    ProcessingInstruction createProcessingInstruction = d.createProcessingInstruction(
        "target", "processing instruction data");
    Text createTextNode = d.createTextNode("sample text node");
    List<Node> canHaveChildren = asList(new Node[] {
        createDocumentFragment, elementWithChildren});
    List<Node> canBeChildren = asList(new Node[] {
        createCDATA, createComment, elementWithChildren,
        createProcessingInstruction, createTextNode});

    for (int i = 0; i < canHaveChildren.size(); i++) {
      Node parent = canHaveChildren.get(i);
      Node cloneParent = parent.cloneNode(false);
      if (canBeChildren.contains(parent)) {
        d.appendChild(cloneParent);
      }
      for (int j = 0; j < canBeChildren.size(); j++) {
        Node child = canBeChildren.get(j);
        cloneParent.appendChild(child.cloneNode(false));
      }
      for (int j = 0; j < canBeChildren.size(); j++) {
        Node clonedChild = cloneParent.getChildNodes().item(j);
        Node hopefullySameChild = canBeChildren.get(j);
        assertEquals(hopefullySameChild.cloneNode(false).toString(),
            clonedChild.toString());
      }
      Node deepClonedNode = parent.cloneNode(true);
      assertEquals(parent.toString(), deepClonedNode.toString());
    }

    // Now check the document.
    XMLParser.removeWhitespace(d);
    assertDocumentEquals(XMLParser.parse("<elementWithChildren>"
        + "<![CDATA[sampl<<< >>e data]]>" + "<!--a sample comment-->"
        + "<elementWithChildren/>" + "<?target processing instruction data?>"
        + "sample text node" + "</elementWithChildren>"), d);
  }

  public void testDocument() {
    Document d = createTestDocument();
    NodeList e1Nodes = d.getElementsByTagName("e1");
    assertEquals(1, e1Nodes.getLength());
    Node e1Node = e1Nodes.item(0);
    assertEquals("e1", ((Element) e1Node).getTagName());

    // It used to be that XML nodes without a DTD would never be returned by getElementById().
    // In DOM 4, an Element's "id" attribute is always used as its id. [1]
    // This seems to be the behavior starting with Chrome 11 and Firefox 32.
    // [1] http://www.w3.org/TR/dom/#concept-id

    Element e1NodeDirect = d.getElementById("e1Id");
    // Allow both the old and new behavior.
    if (e1NodeDirect != null) {
      // Check toString() first for a better error message.
      assertEquals("getElementId returned unexpected element for XML node",
          e1Node.toString(), e1NodeDirect.toString());
      // Not the same for Firefox 32. TODO: investigate.
      // assertSame("getElementId returned unexpected element for XML node",
      //    e1Node, e1NodeDirect);
    }

    Document alienDoc = XMLParser.createDocument();
    Node alienNode11 = alienDoc.importNode(e1Node, true);
    alienDoc.appendChild(alienNode11);
    assertNotSame(e1Node, alienNode11);
    assertEquals(e1Node.toString(), alienNode11.toString());
  }

  public void testElement() {
    Document d = createTestDocument();
    Element top = d.getDocumentElement();
    NodeList el = top.getElementsByTagName("e1");
    assertEquals(1, el.getLength());
    NodeList deepNodes = top.getElementsByTagName("deep");
    assertEquals(2, deepNodes.getLength());
    Element d1 = (Element) deepNodes.item(0);
    assertTrue(d1.hasAttribute("depth"));
    assertFalse(d1.hasAttribute("height"));
    d1.removeAttribute("depth");
    assertFalse(d1.hasAttribute("depth"));
    Element d2 = (Element) deepNodes.item(1);
    assertTrue(d2.hasAttribute("depth"));
    Attr depthAttr = d2.getAttributeNode("depth");
    assertNotNull(depthAttr);
    d2.removeAttribute("depth");
    assertFalse(d2.hasAttribute("depth"));
  }

  public void testForIssue733() {
    // TODO (knorton):
    // http://code.google.com/p/google-web-toolkit/issues/detail?id=2346
    // Fixing issue #733 has been deferred for Safari2. See the bug URL for more
    // details. This should be enabled as soon as that bug is fixed.
    if (XMLParserImplSafari.isSafari2LevelWebKit()) {
      return;
    }

    final Document document = XMLParser.createDocument();
    final Element element = document.createElement("foo");
    document.appendChild(element);
    element.setAttribute("bar", "<");
    final String xmlAsString = document.toString();
    try {
      XMLParser.parse(xmlAsString);
    } catch (DOMParseException e) {
      fail(xmlAsString + " is invalid XML.");
    }
  }

  public void testNamedNodeMap() {
    Document d = createTestDocument();
    NamedNodeMap m = d.getDocumentElement().getAttributes();
    assertEquals("true", ((Attr) m.getNamedItem("fluffy")).getValue());
    assertEquals(2, m.getLength());
  }

  public void testNavigation() {
    Document d = createTestDocument();
    Element documentElement = d.getDocumentElement();
    assertEquals("getPreviousSibling", documentElement.getPreviousSibling(),
        d.getChildNodes().item(0));
    assertEquals("getNextSibling", documentElement.getNextSibling(),
        d.getChildNodes().item(2));
    assertEquals("getDocumentElement", documentElement, d.getChildNodes().item(
        1));
    assertEquals("getTagName", "doc", documentElement.getTagName());
    NodeList documentElementChildNodes = documentElement.getChildNodes();
    int deChildNodesLength = documentElementChildNodes.getLength();
    assertEquals("getFirstChild", documentElement.getFirstChild(),
        documentElementChildNodes.item(0));
    assertEquals("getLastChild", documentElement.getLastChild(),
        documentElementChildNodes.item(deChildNodesLength - 1));
    assertEquals("getNextSibling2",
        documentElement.getFirstChild().getNextSibling(),
        documentElementChildNodes.item(1));

    assertEquals("getPreviousSibling2",
        documentElement.getLastChild().getPreviousSibling(),
        documentElementChildNodes.item(deChildNodesLength - 2));
  }

  public void testNode() {
    Document ns = XMLParser.parse("<x:doc xmlns:x=\"http://x\"/>");
    String xUrl = "http://x";
    assertEquals(xUrl, ns.getFirstChild().getNamespaceURI());
    Element top = ns.getDocumentElement();
    Text xxx = ns.createTextNode("xxx");
    top.appendChild(xxx);
    Text yyy = ns.createTextNode("yyy");
    top.appendChild(yyy);
    assertEquals("xxx", xxx.getNodeValue());
    assertEquals(xxx.getParentNode(), top);
    xxx.setNodeValue("x");
    assertEquals("x", xxx.getData());
    assertEquals(xxx.getOwnerDocument(), ns);
    top.removeChild(xxx);
    assertEquals(1, top.getChildNodes().getLength());
    Comment commentNode = ns.createComment("comment ccc");
    top.replaceChild(commentNode, yyy);
    assertEquals(top.getFirstChild(), commentNode);
    assertEquals(1, top.getChildNodes().getLength());
  }

  /**
   * At one point, this test was failing on one Safari configuration in
   * Production Mode in the 1.5 release branch.
   */
  @DoNotRunWith({Platform.HtmlUnitUnknown})
  public void testParse() {
    Document docA = XMLParser.parse("<!--hello-->   <a spam=\"ham\">\n  <?pi hello ?>dfgdfg  <b/>\t</a>");

    Document docB = XMLParser.createDocument();
    docB.appendChild(docB.createComment("hello"));
    final Element eleB = docB.createElement("a");
    docB.appendChild(eleB);
    eleB.setAttribute("spam", "ham");
    eleB.appendChild(docB.createTextNode("\n  "));
    eleB.appendChild(docB.createProcessingInstruction("pi", "hello "));
    eleB.appendChild(docB.createTextNode("dfgdfg  "));
    eleB.appendChild(docB.createElement("b"));
    eleB.appendChild(docB.createTextNode("\t"));

    assertDocumentEquals(docA, docB);

    try {
      XMLParser.parse("<<<");
      fail();
    } catch (DOMParseException e) {
    }
  }

  public void testPrefix() {
    Document d = XMLParser.parse("<?xml version=\"1.0\"?>\r\n"
        + "<!-- both namespace prefixes are available throughout -->\r\n"
        + "<bk:book xmlns:bk=\'urn:loc.gov:books\'\r\n"
        + "         xmlns:isbn=\'urn:ISBN:0-395-36341-6\'>\r\n"
        + "    <bk:title>Cheaper by the Dozen</bk:title>\r\n"
        + "    <isbn:number>1568491379</isbn:number>\r\n" + "</bk:book>");
    assertEquals("bk:book", d.getDocumentElement().getNodeName());
    assertEquals("bk", d.getDocumentElement().getPrefix());
    assertEquals(1, d.getElementsByTagName("book").getLength());
    assertEquals(d.getElementsByTagName("book").item(0), d.getDocumentElement());
  }

  public void testProcessingInstruction() {
    Document d = createTestDocument();
    ProcessingInstruction pi = (ProcessingInstruction) d.getChildNodes().item(0);
    assertEquals("target", pi.getTarget());
    assertEquals("some data", pi.getData());
    pi.setData("other data");
    assertEquals("other data", pi.getData());
  }

  public void testText() {
    Document d = createTestDocument();
    List<Node> textLikeNodes = Arrays.asList(new Node[] {
        d.createTextNode(""), d.createCDATASection(""), d.createComment("")});
    StringBuffer b = new StringBuffer();
    for (char i = 32; i < 30000; i++) {
      b.append(i);
    }
    for (Iterator<Node> iter = textLikeNodes.iterator(); iter.hasNext();) {
      CharacterData textLike = (CharacterData) iter.next();
      textLike.setData(b.toString());
      // CHECKSTYLE_OFF
      assertEquals("initialLength type:" + textLike.getNodeType(), 30000 - 32,
          textLike.getLength());
      assertEquals("initialEquals", textLike.getData(), b.toString());
      // CHECKSTYLE_ON
    }
    for (int i = 32; i < 29900; i += 100) {
      for (Iterator<Node> iter = textLikeNodes.iterator(); iter.hasNext();) {
        CharacterData textLike = (CharacterData) iter.next();
        // CHECKSTYLE_OFF
        assertEquals("substring type:" + textLike.getNodeType() + " count: "
            + i, b.substring(i, i + 100), textLike.substringData(i, 100));
        // CHECKSTYLE_ON
      }
    }
    for (Iterator<Node> iter = textLikeNodes.iterator(); iter.hasNext();) {
      StringBuffer bTemp = new StringBuffer(b.toString());
      CharacterData textLike = (CharacterData) iter.next();
      textLike.deleteData(100, 100);
      bTemp.delete(100, 200);
      // CHECKSTYLE_OFF
      assertEquals("deleteLength type:" + textLike.getNodeType(),
          bTemp.length(), textLike.getData().length());
      assertEquals("deleteEquals type:" + textLike.getNodeType(),
          bTemp.toString(), textLike.getData());
      // CHECKSTYLE_ON
      bTemp.setLength(0);
    }
    for (Iterator<Node> iter = textLikeNodes.iterator(); iter.hasNext();) {
      StringBuffer bTemp = new StringBuffer(b.toString());
      CharacterData textLike = (CharacterData) iter.next();
      textLike.setData(bTemp.toString());
      textLike.replaceData(50, 100, " ");
      bTemp.replace(50, 150, " ");
      // CHECKSTYLE_OFF
      assertEquals("replaceLength type:" + textLike.getNodeType(),
          bTemp.length(), textLike.getData().length());
      assertEquals("replaceEquals type:" + textLike.getNodeType(),
          bTemp.toString(), textLike.getData());
      // CHECKSTYLE_ON
      bTemp.setLength(0);
    }
    for (Iterator<Node> iter = textLikeNodes.iterator(); iter.hasNext();) {
      CharacterData textLike = (CharacterData) iter.next();
      textLike.appendData("!!!");
      assertTrue("endswith!!!", textLike.getData().endsWith("!!!"));
      textLike.insertData(0, "!");
      textLike.insertData(1, "@@");
      assertTrue("startsWith !@@", textLike.getData().startsWith("!@@"));
    }
    Text t = (Text) d.getDocumentElement().getChildNodes().item(3);
    Text rightT = t.splitText(5);
    assertEquals("t and leftT parent equality", t.getParentNode(),
        rightT.getParentNode());
    assertEquals("leftT.getPreviousSibling", rightT.getPreviousSibling(), t);
    assertEquals("t.length", 5, t.getData().length());
    assertEquals("leftT.length", 5, rightT.getData().length());
    assertEquals("t data", "01234", t.getData());
    assertEquals("LeftT data", "56789", rightT.getData());
    CDATASection cd = (CDATASection) d.getDocumentElement().getChildNodes().item(
        7);
    Text rightCD = cd.splitText(5);
    assertEquals("cd and leftCd parent equality", cd.getParentNode(),
        rightCD.getParentNode());
    assertEquals("leftCD.getPreviousSibling", rightCD.getPreviousSibling(), cd);
    assertEquals("cd length", 5, cd.getData().length());
    assertEquals("leftCD.length", 5, rightCD.getData().length());
    assertEquals("cd data", "klmno", cd.getData());
    assertEquals("leftCD data", "pqrst", rightCD.getData());
    d.getDocumentElement().normalize();
    assertEquals(
        "normalized t",
        "0123456789abcdefghij",
        d.getDocumentElement().getChildNodes().item(3).toString());
  }
}
