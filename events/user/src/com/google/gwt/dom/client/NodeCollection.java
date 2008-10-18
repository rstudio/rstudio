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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * An ElementCollection is a list of nodes. An individual node may be accessed by
 * either ordinal index or the node's name or id attributes.
 * 
 * Note: Collections in the HTML DOM are assumed to be live meaning that they
 * are automatically updated when the underlying document is changed.
 *
 * @param <T> the type of contained node
 */
public class NodeCollection<T extends Node> extends JavaScriptObject {

  protected NodeCollection() {
  }

  /**
   * This method retrieves a node specified by ordinal index. Nodes are numbered
   * in tree order (depth-first traversal order).
   * 
   * @param index The index of the node to be fetched. The index origin is 0.
   * @return The element at the corresponding position upon success. A value of
   *         null is returned if the index is out of range.
   */
  public final native T getItem(int index) /*-{
    return this[index];
  }-*/;

  /**
   * This attribute specifies the length or size of the list.
   */
  public final native int getLength() /*-{
    return this.length;
  }-*/;

  /**
   * This method retrieves a Node using a name. With [HTML 4.01] documents, it
   * first searches for a Node with a matching id attribute. If it doesn't find
   * one, it then searches for a Node with a matching name attribute, but only
   * on those elements that are allowed a name attribute. With [XHTML 1.0]
   * documents, this method only searches for Nodes with a matching id
   * attribute. This method is case insensitive in HTML documents and case
   * sensitive in XHTML documents.
   * 
   * @param name The name of the Node to be fetched.
   * @return The element with a name or id attribute whose value corresponds to
   *         the specified string. Upon failure (e.g., no element with this name
   *         exists), returns null.
   */
  public final native T getNamedItem(String name) /*-{
    return this[name];
  }-*/;
}
