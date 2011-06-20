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
import com.google.gwt.user.client.Window;
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

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testAttr() {
    Document d = createTestDocument();
    Element de = d.getDocumentElement();
    de.setAttribute("created", "true");
    assertEquals("true", de.getAttribute("created"));
    de.setAttribute("set", "toAValue");
    assertEquals(de.getAttribute("set"), "toAValue");
    assertTrue(de.getAttributeNode("set").getSpecified());
    assertEquals(de.getAttributeNode("unset"), null);
  }

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testCreate() {
    Document d = XMLParser.createDocument();
    CDATASection createCDATA;
    if (XMLParser.supportsCDATASection()) {
      createCDATA = d.createCDATASection("sampl<<< >>e data");
    } else {
      createCDATA = d.createCDATASection("sample data");
    }
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
    if (XMLParser.supportsCDATASection()) {
      assertDocumentEquals(XMLParser.parse("<elementWithChildren>"
          + "<![CDATA[sampl<<< >>e data]]>" + "<!--a sample comment-->"
          + "<elementWithChildren/>" + "<?target processing instruction data?>"
          + "sample text node" + "</elementWithChildren>"), d);
    } else {
      assertDocumentEquals(XMLParser.parse("<elementWithChildren>"
          + "sample data" + "<!--a sample comment-->"
          + "<elementWithChildren/>" + "<?target processing instruction data?>"
          + "sample text node" + "</elementWithChildren>"), d);
    }
  }

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testDocument() {
    Document d = createTestDocument();
    NodeList e1Nodes = d.getElementsByTagName("e1");
    assertEquals(e1Nodes.getLength(), 1);
    Node e1Node = e1Nodes.item(0);
    assertEquals(((Element) e1Node).getTagName(), "e1");

    // we didn't define a dtd, so no id for us
    Element e1NodeDirect = d.getElementById("e1Id");
    // Chrome 11 and up fail to implement this behavior
    if (!Window.Navigator.getUserAgent().matches(".*Chrome/(1[1-9]|[2-9][0-9])\\..*")) {
      assertNull(e1NodeDirect);
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
    assertEquals(el.getLength(), 1);
    NodeList deepNodes = top.getElementsByTagName("deep");
    assertEquals(deepNodes.getLength(), 2);
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
    assertEquals(((Attr) m.getNamedItem("fluffy")).getValue(), "true");
    assertEquals(m.getLength(), 2);
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
    assertEquals("getTagName", documentElement.getTagName(), "doc");
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
    assertEquals(xxx.getNodeValue(), "xxx");
    assertEquals(xxx.getParentNode(), top);
    xxx.setNodeValue("x");
    assertEquals(xxx.getData(), "x");
    assertEquals(xxx.getOwnerDocument(), ns);
    top.removeChild(xxx);
    assertEquals(top.getChildNodes().getLength(), 1);
    Comment commentNode = ns.createComment("comment ccc");
    top.replaceChild(commentNode, yyy);
    assertEquals(top.getFirstChild(), commentNode);
    assertEquals(top.getChildNodes().getLength(), 1);
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
    assertEquals(d.getDocumentElement().getNodeName(), "bk:book");
    assertEquals(d.getDocumentElement().getPrefix(), "bk");
    assertEquals(d.getElementsByTagName("book").getLength(), 1);
    assertEquals(d.getElementsByTagName("book").item(0), d.getDocumentElement());
  }

  public void testProcessingInstruction() {
    Document d = createTestDocument();
    ProcessingInstruction pi = (ProcessingInstruction) d.getChildNodes().item(0);
    assertEquals(pi.getTarget(), "target");
    assertEquals(pi.getData(), "some data");
    pi.setData("other data");
    assertEquals(pi.getData(), "other data");
  }

  @DoNotRunWith({Platform.HtmlUnitBug})
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
    assertEquals("t.length", t.getData().length(), 5);
    assertEquals("leftT.length", rightT.getData().length(), 5);
    assertEquals("t data", t.getData(), "01234");
    assertEquals("LeftT data", rightT.getData(), "56789");
    CDATASection cd = (CDATASection) d.getDocumentElement().getChildNodes().item(
        7);
    Text rightCD = cd.splitText(5);
    assertEquals("cd and leftCd parent equality", cd.getParentNode(),
        rightCD.getParentNode());
    assertEquals("leftCD.getPreviousSibling", rightCD.getPreviousSibling(), cd);
    assertEquals("cd length", cd.getData().length(), 5);
    assertEquals("leftCD.length", rightCD.getData().length(), 5);
    assertEquals("cd data", cd.getData(), "klmno");
    assertEquals("leftCD data", rightCD.getData(), "pqrst");
    d.getDocumentElement().normalize();
    assertEquals("normalized t", d.getDocumentElement().getChildNodes().item(
        3).toString(), "0123456789abcdefghij");
  }
}
