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
package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

/**
 * This class wraps the native Node object.
 */
class NodeImpl extends DOMItem implements Node {

  /**
   * This method creates a new node of the correct type.
   * 
   * @param node - the supplied DOM JavaScript object
   * @return a Node object that corresponds to the DOM object
   */
  static Node build(JavaScriptObject node) {
    if (node == null) {
      return null;
    }
    short nodeType = XMLParserImpl.getNodeType(node);
    switch (nodeType) {
      case Node.ATTRIBUTE_NODE:
        return new AttrImpl(node);
      case Node.CDATA_SECTION_NODE:
        return new CDATASectionImpl(node);
      case Node.COMMENT_NODE:
        return new CommentImpl(node);
      case Node.DOCUMENT_FRAGMENT_NODE:
        return new DocumentFragmentImpl(node);
      case Node.DOCUMENT_NODE:
        return new DocumentImpl(node);
      case Node.ELEMENT_NODE:
        return new ElementImpl(node);
      case Node.PROCESSING_INSTRUCTION_NODE:
        return new ProcessingInstructionImpl(node);
      case Node.TEXT_NODE:
        return new TextImpl(node);
      default:
        return new NodeImpl(node);
    }
  }

  /**
   * creates a new NodeImpl from the supplied JavaScriptObject.
   * 
   * @param jso - the DOM node JavaScriptObject
   */
  protected NodeImpl(JavaScriptObject jso) {
    super(jso);
  }

  /**
   * This function delegates to the native method <code>appendChild</code> in
   * XMLParserImpl.
   */
  public Node appendChild(Node newChild) {
    try {
      final JavaScriptObject newChildJs = ((DOMItem) newChild).getJsObject();
      final JavaScriptObject appendChildResults = XMLParserImpl.appendChild(
          this.getJsObject(), newChildJs);
      return NodeImpl.build(appendChildResults);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>cloneNode</code> in
   * XMLParserImpl.
   */
  public Node cloneNode(boolean deep) {
    return NodeImpl.build(XMLParserImpl.cloneNode(this.getJsObject(), deep));
  }

  public NamedNodeMap getAttributes() {
    return new NamedNodeMapImpl(XMLParserImpl.getAttributes(this.getJsObject()));
  }

  public NodeList getChildNodes() {
    return new NodeListImpl(XMLParserImpl.getChildNodes(this.getJsObject()));
  }

  public Node getFirstChild() {
    return getChildNodes().item(0);
  }

  public Node getLastChild() {
    return getChildNodes().item(getChildNodes().getLength() - 1);
  }

  /**
   * This function delegates to the native method <code>getNamespaceURI</code>
   * in XMLParserImpl.
   */
  public String getNamespaceURI() {
    return XMLParserImpl.getNamespaceURI(this.getJsObject());
  }

  public Node getNextSibling() {
    return NodeImpl.build(XMLParserImpl.getNextSibling(this.getJsObject()));
  }

  public String getNodeName() {
    return XMLParserImpl.getNodeName(this.getJsObject());
  }

  public short getNodeType() {
    return XMLParserImpl.getNodeType(this.getJsObject());
  }

  public String getNodeValue() {
    return XMLParserImpl.getNodeValue(this.getJsObject());
  }

  public Document getOwnerDocument() {
    return (Document) NodeImpl.build(XMLParserImpl.getOwnerDocument(this.getJsObject()));
  }

  public Node getParentNode() {
    return NodeImpl.build(XMLParserImpl.getParentNode(this.getJsObject()));
  }

  /**
   * This function delegates to the native method <code>getPrefix</code> in
   * XMLParserImpl.
   */
  public String getPrefix() {
    return XMLParserImpl.getPrefix(this.getJsObject());
  }

  public Node getPreviousSibling() {
    return NodeImpl.build(XMLParserImpl.getPreviousSibling(this.getJsObject()));
  }

  /**
   * This function delegates to the native method <code>hasAttributes</code>
   * in XMLParserImpl.
   */
  public boolean hasAttributes() {
    return XMLParserImpl.hasAttributes(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>hasChildNodes</code>
   * in XMLParserImpl.
   */
  public boolean hasChildNodes() {
    return XMLParserImpl.hasChildNodes(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>insertBefore</code> in
   * XMLParserImpl.
   */
  public Node insertBefore(Node newChild, Node refChild) {
    try {
      final JavaScriptObject newChildJs = ((DOMItem) newChild).getJsObject();
      final JavaScriptObject refChildJs;
      if (refChild != null) {
        refChildJs = ((DOMItem) refChild).getJsObject();
      } else {
        refChildJs = null;
      }
      final JavaScriptObject insertBeforeResults = XMLParserImpl.insertBefore(
          this.getJsObject(), newChildJs, refChildJs);
      return NodeImpl.build(insertBeforeResults);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>normalize</code> in
   * XMLParserImpl.
   */
  public void normalize() {
    XMLParserImpl.normalize(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>removeChild</code> in
   * XMLParserImpl.
   */
  public Node removeChild(Node oldChild) {
    try {
      final JavaScriptObject oldChildJs = ((DOMItem) oldChild).getJsObject();
      final JavaScriptObject removeChildResults = XMLParserImpl.removeChild(
          this.getJsObject(), oldChildJs);
      return NodeImpl.build(removeChildResults);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>replaceChild</code> in
   * XMLParserImpl.
   */
  public Node replaceChild(Node newChild, Node oldChild) {
    try {
      final JavaScriptObject newChildJs = ((DOMItem) newChild).getJsObject();
      final JavaScriptObject oldChildJs = ((DOMItem) oldChild).getJsObject();
      final JavaScriptObject replaceChildResults = XMLParserImpl.replaceChild(
          this.getJsObject(), newChildJs, oldChildJs);
      return NodeImpl.build(replaceChildResults);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>setNodeValue</code> in
   * XMLParserImpl.
   */
  public void setNodeValue(String nodeValue) {
    try {
      XMLParserImpl.setNodeValue(this.getJsObject(), nodeValue);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }
 
  @Override
  public String toString() {
    return XMLParserImpl.getInstance().toStringImpl(this);
  }
}
