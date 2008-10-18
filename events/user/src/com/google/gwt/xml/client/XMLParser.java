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

import com.google.gwt.xml.client.impl.XMLParserImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the client interface to XML parsing.
 */
public class XMLParser {

  private static final XMLParserImpl impl = XMLParserImpl.getInstance();

  /**
   * This method creates a new document, to be manipulated by the DOM API.
   * 
   * @return the newly created document
   */
  public static Document createDocument() {
    return impl.createDocument();
  }

  /**
   * This method parses a new document from the supplied string, throwing a
   * <code>DOMParseException</code> if the parse fails.
   * 
   * @param contents the String to be parsed into a <code>Document</code>
   * @return the newly created <code>Document</code>
   */
  public static Document parse(String contents) {
    return impl.parse(contents);
  }

  /**
   * This method removes all <code>Text</code> nodes which are made up of only
   * white space.
   * 
   * @param n the node which is to have all of its whitespace descendents
   *          removed.
   */
  public static void removeWhitespace(Node n) {
    removeWhitespaceInner(n, null);
  }

  /**
   * This method determines whether the browser supports {@link CDATASection} 
   * as distinct entities from <code>Text</code> nodes.
   * 
   * @return true if the browser supports {@link CDATASection}, otherwise
   *         <code>false</code>.
   */
  public static boolean supportsCDATASection() {
    return impl.supportsCDATASection();
  }

  /*
   * The inner recursive method for removeWhitespace
   */
  private static void removeWhitespaceInner(Node n, Node parent) {
    // This n is removed from the parent if n is a whitespace node
    if (parent != null && n instanceof Text && (!(n instanceof CDATASection))) {
      Text t = (Text) n;
      if (t.getData().matches("[ \t\n]*")) {
        parent.removeChild(t);
      }
    }
    if (n.hasChildNodes()) {
      int length = n.getChildNodes().getLength();
      List<Node> toBeProcessed = new ArrayList<Node>();
      // We collect all the nodes to iterate as the child nodes will change 
      // upon removal
      for (int i = 0; i < length; i++) {
        toBeProcessed.add(n.getChildNodes().item(i));
      }
      // This changes the child nodes, but the iterator of nodes never changes
      // meaning that this is safe
      for (Node childNode : toBeProcessed) {
        removeWhitespaceInner(childNode, n);
      }
    }
  }

  /**
   * Not instantiable.
   */
  private XMLParser() {
  }
}
