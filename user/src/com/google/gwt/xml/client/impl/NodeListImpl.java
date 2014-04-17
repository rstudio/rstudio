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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

/**
 * This class implements the NodeList interface using the underlying
 * JavaScriptObject's implementation.
 */
class NodeListImpl extends DOMItem implements NodeList {

  protected NodeListImpl(JavaScriptObject o) {
    super(o);
  }

  public int getLength() {
    return XMLParserImpl.getLength(this.getJsObject());
  }

  /**
   * This method gets the index item.
   * 
   * @param index - the index to be retrieved
   * @return the item at this index
   * @see com.google.gwt.xml.client.NodeList#item(int)
   */
  public Node item(int index) {
    return NodeImpl.build(XMLParserImpl.item(this.getJsObject(), index));
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < getLength(); i++) {
      b.append(item(i).toString());
    }
    return b.toString();
  }
}
