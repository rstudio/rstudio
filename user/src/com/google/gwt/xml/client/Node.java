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

/**
 * This is the base interface for DOM nodes, as obtained from using
 * <code>XMLParser</code> methods. Methods for iterating over and accessing
 * values from nodes are supplied here.
 */
public interface Node {
  /**
   * The constant 1 denotes DOM nodes of type Element.
   */
  short ELEMENT_NODE = 1;

  /**
   * The constant 2 denotes DOM nodes of type Attribute.
   */
  short ATTRIBUTE_NODE = 2;

  /**
   * The constant 3 denotes DOM nodes of type Text.
   */
  short TEXT_NODE = 3;

  /**
   * The constant 4 denotes DOM nodes of type CdataSection.
   */
  short CDATA_SECTION_NODE = 4;

  /**
   * The constant 5 denotes DOM nodes of type EntityReference.
   */
  short ENTITY_REFERENCE_NODE = 5;

  /**
   * The constant 6 denotes DOM nodes of type Entity.
   */
  short ENTITY_NODE = 6;

  /**
   * The constant 7 denotes DOM nodes of type ProcessingInstruction.
   */
  short PROCESSING_INSTRUCTION_NODE = 7;

  /**
   * The constant 8 denotes DOM nodes of type Comment.
   */
  short COMMENT_NODE = 8;

  /**
   * The constant 9 denotes DOM nodes of type Document.
   */
  short DOCUMENT_NODE = 9;

  /**
   * The constant 10 denotes DOM nodes of type DocumentType.
   */
  short DOCUMENT_TYPE_NODE = 10;

  /**
   * The constant 11 denotes DOM nodes of type DocumentFragment.
   */
  short DOCUMENT_FRAGMENT_NODE = 11;

  /**
   * The constant 12 denotes DOM nodes of type Notation.
   */
  short NOTATION_NODE = 12;

  /**
   * This method appends child <code>newChild</code>.
   * 
   * @param newChild the <code>Node</code> to be added
   * @return the child <code>Node</code> appended
   */
  Node appendChild(Node newChild);

  /**
   * This method copies this <code>Node</code>.
   * 
   * @param deep whether to recurse to children
   * @return <code>Node</code> cloned
   */
  Node cloneNode(boolean deep);

  /**
   * This method retrieves the attributes.
   * 
   * @return the attributes of this <code>Node</code>
   */
  NamedNodeMap getAttributes();

  /**
   * This method retrieves the child nodes.
   * 
   * @return the child nodes of this <code>Node</code>
   */
  NodeList getChildNodes();

  /**
   * This method retrieves the first child.
   * 
   * @return the first child of this <code>Node</code>
   */
  Node getFirstChild();

  /**
   * This method retrieves the last child.
   * 
   * @return the last child of this <code>Node</code>
   */
  Node getLastChild();

  /**
   * This method retrieves the namespace URI.
   * 
   * @return the namespace URI of this <code>Node</code>
   */
  String getNamespaceURI();

  /**
   * This method retrieves the next sibling.
   * 
   * @return the next sibling of this <code>Node</code>
   */
  Node getNextSibling();

  /**
   * This method retrieves the name.
   * 
   * @return the name of this <code>Node</code>
   */
  String getNodeName();

  /**
   * This method retrieves the type.
   * 
   * @return the type of this <code>Node</code>
   */
  short getNodeType();

  /**
   * This method retrieves the value.
   * 
   * @return the value of this <code>Node</code>
   */
  String getNodeValue();

  /**
   * This method retrieves the owner document.
   * 
   * @return the owner document of this <code>Node</code>
   */
  Document getOwnerDocument();

  /**
   * This method retrieves the parent.
   * 
   * @return the parent of this <code>Node</code>
   */
  Node getParentNode();

  /**
   * This method retrieves the prefix.
   * 
   * @return the prefix of this <code>Node</code>
   */
  String getPrefix();

  /**
   * This method retrieves the previous sibling.
   * 
   * @return the previous sibling of this <code>Node</code>
   */
  Node getPreviousSibling();

  /**
   * This method determines whether this <code>Node</code> has any attributes.
   * 
   * @return <code>true</code> if this <code>Node</code> has any attributes
   */
  boolean hasAttributes();

  /**
   * This method determines whether this <code>Node</code> has any child
   * nodes.
   * 
   * @return <code>true</code> if this <code>Node</code> has any child nodes
   */
  boolean hasChildNodes();

  /**
   * This method inserts before <code>newChild</code>.
   * 
   * @param newChild the <code>Node</code> to be added
   * @param refChild the <code>Node</code> which determines the position to
   *          insert
   * @return the before <code>Node</code> inserted
   */
  Node insertBefore(Node newChild, Node refChild);

  /**
   * This method may collapse adjacent text nodes into one text node, depending
   * on the implementation. Safari 4.0 and Chrome will also merge CDATA nodes
   * into text nodes, even though they support CDATA nodes as distinct nodes.
   */
  void normalize();

  /**
   * This method removes child <code>oldChild</code>.
   * 
   * @param oldChild the <code>Node</code> to be removed
   * @return the child <code>Node</code> removed
   */
  Node removeChild(Node oldChild);

  /**
   * This method replaces the child <code>oldChild</code> with
   * <code>newChild</code>.
   * 
   * @param newChild the <code>Node</code> to be added
   * @param oldChild the <code>Node</code> to be removed
   * @return the child <code>Node</code> replaced
   */
  Node replaceChild(Node newChild, Node oldChild);

  /**
   * This method sets the value to <code>nodeValue</code>.
   * 
   * @param nodeValue the new value
   */
  void setNodeValue(String nodeValue);

}