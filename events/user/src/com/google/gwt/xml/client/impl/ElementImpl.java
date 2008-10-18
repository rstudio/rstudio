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
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;

/**
 * This method implements the Element interface.
 */
class ElementImpl extends NodeImpl implements Element {

  protected ElementImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>getAttribute</code> in
   * XMLParserImpl.
   */
  public String getAttribute(String tagName) {
    return XMLParserImpl.getAttribute(this.getJsObject(), tagName);
  }

  /**
   * This function delegates to the native method <code>getAttributeNode</code>
   * in XMLParserImpl.
   */
  public Attr getAttributeNode(String tagName) {
    return (Attr) NodeImpl.build(XMLParserImpl.getAttributeNode(
        this.getJsObject(), tagName));
  }

  /**
   * This function delegates to the native method
   * <code>getElementsByTagName</code> in XMLParserImpl.
   */
  public NodeList getElementsByTagName(String tagName) {
    return new NodeListImpl(XMLParserImpl.getElementsByTagName(
        this.getJsObject(), tagName));
  }

  /**
   * This function delegates to the native method <code>getTagName</code> in
   * XMLParserImpl.
   */
  public String getTagName() {
    return XMLParserImpl.getTagName(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>hasAttribute</code> in
   * XMLParserImpl.
   */
  public boolean hasAttribute(String tagName) {
    return getAttribute(tagName) != null;
  }

  /**
   * This function delegates to the native method <code>removeAttribute</code>
   * in XMLParserImpl.
   */
  public void removeAttribute(String name) throws DOMNodeException {
    try {
      XMLParserImpl.removeAttribute(this.getJsObject(), name);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>setAttribute</code> in
   * XMLParserImpl.
   */
  public void setAttribute(String name, String value) throws DOMNodeException {
    try {
      XMLParserImpl.setAttribute(this.getJsObject(), name, value);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }
}
