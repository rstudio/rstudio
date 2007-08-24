/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;

/**
 * This class implements the NamedNodeMap interface.
 */
class NamedNodeMapImpl extends NodeListImpl implements NamedNodeMap {

  protected NamedNodeMapImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * Gets the number of nodes in this object.
   * 
   * @return the number of nodes in this object.
   * @see com.google.gwt.xml.client.impl.NodeListImpl#getLength()
   */
  @Override
  public int getLength() {
    return super.getLength();
  }

  /**
   * This method gets the item at the index position.
   * 
   * @param name - the name of the item 
   * @return the item retrieved from the name
   */
  public Node getNamedItem(String name) {
    return NodeImpl.build(XMLParserImpl.getNamedItem(this.getJsObject(), name));
  }

  @Override
  public Node item(int index) {
    return super.item(index);
  }

  /**
   * This function delegates to the native method <code>removeNamedItem</code>
   * in XMLParserImpl.
   */
  public Node removeNamedItem(String name) {
    try {
      return NodeImpl.build(XMLParserImpl.removeNamedItem(this.getJsObject(),
        name));
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>setNamedItem</code> in
   * XMLParserImpl.
   */
  public Node setNamedItem(Node arg) {
    try {
      return NodeImpl.build(XMLParserImpl.setNamedItem(this.getJsObject(),
        ((DOMItem) arg).getJsObject()));
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }
}
