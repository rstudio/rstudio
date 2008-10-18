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

/*
 * Implementation notes: Safari does not support mutable attributes, so no
 * mechanism for creating Attr objects has been supplied. IE does not support
 * any of the xxxNS operations, so they have been omitted as well. IE does not
 * use importNode to copy nodes from one document into another.
 */
/**
 * <code>Document</code> objects represent XML documents. Each
 * <code>Document</code> can contain exactly one <code>Element</code> node,
 * and any number of other node types.
 */
public interface Document extends Node {
  /**
   * This method creates a new <code>CDATASection</code>.
   * 
   * @param data the data of the new <code>CDATASection</code>
   * @return the newly created <code>CDATASection</code>
   */
  CDATASection createCDATASection(String data);

  /**
   * This method creates a new <code>Comment</code>.
   * 
   * @param data the data of the new <code>Comment</code>
   * @return the newly created <code>Comment</code>
   */
  Comment createComment(String data);

  /**
   * This method creates a new <code>DocumentFragment</code>.
   * 
   * @return the newly created <code>DocumentFragment</code>
   */
  DocumentFragment createDocumentFragment();

  /**
   * This method creates a new <code>Element</code>.
   * 
   * @param tagName the tag name of the new <code>Element</code>
   * @return the newly created <code>Element</code>
   */
  Element createElement(String tagName);

  /**
   * This method creates a new <code>ProcessingInstruction</code>.
   * 
   * @param target the target of the new <code>ProcessingInstruction</code>
   * @param data the data of the new <code>ProcessingInstruction</code>
   * @return the newly created <code>ProcessingInstruction</code>
   */
  ProcessingInstruction createProcessingInstruction(String target, String data);

  /**
   * This method creates a new <code>Text</code>.
   * 
   * @param data the data of the new <code>Text</code>
   * @return the newly created <code>Text</code>
   */
  Text createTextNode(String data);

  /**
   * This method retrieves the document element. Each document has at most one
   * <code>Element</code> as its direct child, and this node is returned if it
   * exists. <code>null</code> is returned otherwise.
   * 
   * @return the document element of this <code>Document</code>
   */
  Element getDocumentElement();

  /**
   * This method retrieves the unique descendent elements which has an id of
   * <code>elementId</code>. Note the attribute which is used as an ID must
   * be supplied in the DTD of the document. It is not sufficient to give the
   * <code>Element</code> to be retrieved an attribute named 'id'.
   * 
   * @return the <code>Element</code> which has an id of
   *         <code>elementId</code> and belongs to this <code>Document</code>
   */
  Element getElementById(String elementId);

  /**
   * This method retrieves any descendent elements which have a tag name of
   * <code>tagname</code>.
   * 
   * @return the <code>NodeList</code> of elements which has a tag name of
   *         <code>tagname</code> and belong to this <code>Document</code>
   */
  NodeList getElementsByTagName(String tagname);

  /**
   * This method imports a node into the current <code>Document</code>.
   * 
   * @param deep whether to recurse to children
   * @return the node <code>Node</code> imported
   */
  Node importNode(Node importedNode, boolean deep);
}