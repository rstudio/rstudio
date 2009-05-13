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

/**
 * The Text interface represents textual content.
 */
public class Text extends Node {

  /**
   * Assert that the given {@link Node} is of type {@link Node#TEXT_NODE} and
   * automatically typecast it.
   */
  public static Text as(Node node) {
    assert node.getNodeType() == Node.TEXT_NODE;
    return (Text) node;
  }

  protected Text() {
  }

  /**
   * Deletes data at the given [offset, length] range.
   */
  public final native void deleteData(int offset, int length) /*-{
    this.deleteData(offset, length);
  }-*/;

  /**
   * The character data of this text node.
   */
  public final native String getData() /*-{
    return this.data;
  }-*/;

  /**
   * The number of characters available through the data property.
   */
  public final native int getLength() /*-{
    return this.length;
  }-*/;

  /**
   * Inserts character data at the given offset.
   */
  public final native void insertData(int offset, String data) /*-{
    this.insertData(offset, data);
  }-*/;

  /**
   * Replaces data at the given [offset, length] range with the given string.
   */
  public final native void replaceData(int offset, int length, String data) /*-{
    this.replaceData(offset, length, data);
  }-*/;

  /**
   * The character data of this text node.
   */
  public final native void setData(String data) /*-{
    this.data = data;
  }-*/;

  /**
   * Splits the data in this node into two separate text nodes. The text
   * before the split offset is kept in this node, and a new sibling node is
   * created to contain the text after the offset.
   */
  public final native Text splitText(int offset) /*-{
    return this.splitText(offset);
  }-*/;
}
