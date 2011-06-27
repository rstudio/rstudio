/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.resource.ResourceOracle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Uses SAX events to construct a DOM Document. Each node in the Document will
 * have a {@link XMLElement.Location} object attached to that Node's user data
 * with the {@value XMLElement#LOCATION_KEY} key.
 */
class W3cDocumentBuilder extends DefaultHandler2 {
  private final Document document;
  private final Stack<Node> eltStack = new Stack<Node>();
  private Locator locator;
  private final TreeLogger logger;
  private final GwtResourceEntityResolver resolver;

  public W3cDocumentBuilder(TreeLogger logger, String pathBase,
      ResourceOracle resourceOracle) throws ParserConfigurationException {
    this.logger = logger;
    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    eltStack.push(document);
    resolver = new GwtResourceEntityResolver(logger, resourceOracle, pathBase);
  }

  /**
   * Appends to the existing Text node, if possible.
   */
  @Override
  public void characters(char[] ch, int start, int length) {
    Node current = eltStack.peek();
    if (current.getChildNodes().getLength() == 1
        && current.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
      Text text = (Text) current.getChildNodes().item(0);
      text.appendData(new String(ch, start, length));
    } else {
      Text text = document.createTextNode(new String(ch, start, length));
      eltStack.peek().appendChild(text);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    Node elt = eltStack.pop();
    assert elt.getLocalName().equals(localName);
  }

  @Override
  public void error(SAXParseException exception) {
    logger.log(TreeLogger.ERROR, exception.getMessage());
    logger.log(TreeLogger.DEBUG, "SAXParseException", exception);
  }

  @Override
  public void fatalError(SAXParseException exception) {
    /*
     * Fatal errors seem to be no scarier than error errors, and simply happen
     * due to badly formed XML.
     */
    logger.log(TreeLogger.ERROR, exception.getMessage());
    logger.log(TreeLogger.DEBUG, "SAXParseException", exception);
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public InputSource resolveEntity(String name, String publicId,
      String baseURI, String systemId) {
    return resolver.resolveEntity(publicId, systemId);
  }

  /**
   * This is the whole reason for this mess. We want to know where a given
   * element comes from.
   */
  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  @Override
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) {
    Element elt = document.createElementNS(uri, qName);
    eltStack.peek().appendChild(elt);
    eltStack.push(elt);

    for (int i = 0, j = attributes.getLength(); i < j; i++) {
      elt.setAttributeNS(attributes.getURI(i), attributes.getQName(i),
          attributes.getValue(i));
    }

    XMLElement.Location location = new XMLElement.Location(
        locator.getSystemId(), locator.getLineNumber());
    elt.setUserData(XMLElement.LOCATION_KEY, location, null);
  }

  @Override
  public void warning(SAXParseException exception) {
    logger.log(TreeLogger.WARN, exception.getMessage());
    logger.log(TreeLogger.DEBUG, "SAXParseException", exception);
  }
}