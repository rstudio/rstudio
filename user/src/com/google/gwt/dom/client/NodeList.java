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
 * The NodeList interface provides the abstraction of an ordered collection of
 * nodes, without defining or constraining how this collection is implemented.
 * NodeList objects in the DOM are live.
 * 
 * The items in the NodeList are accessible via an integral index, starting from
 * 0.
 * 
 * @param <T> the type of contained node
 */
public class NodeList<T extends Node> extends JavaScriptObject {

  protected NodeList() {
  }

  /**
   * Returns the indexth item in the collection. If index is greater than or
   * equal to the number of nodes in the list, this returns null.
   * 
   * @param index Index into the collection
   * @return the node at the indexth position in the NodeList, or null if that
   *         is not a valid index.
   */
  public final native T getItem(int index) /*-{
    return this[index];
  }-*/;

  /**
   * The number of nodes in the list. The range of valid child node indices is 0
   * to length-1 inclusive.
   */
  public final native int getLength() /*-{
    return this.length;
  }-*/;
}
