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
 * This is an immutable object because Safari does not support mutable attribute
 * maps.
 */

/**
 * Represents a string-to-node map, used in <code>getAttributes</code>.
 */
public interface NamedNodeMap {
  /**
   * Returns the number of items in this <code>NamedNodeMap</code>.
   * 
   * @return the number of items in this <code>NamedNodeMap</code>
   */
  int getLength();

  /**
   * This method gets the item having the given name.
   * 
   * @param name - the name used to look up the item
   * @return the item retrieved
   */
  Node getNamedItem(String name);

  /**
   * This method gets the item at the index position.
   * 
   * @param index - the index to retrieve the item from
   * @return the item retrieved
   */
  Node item(int index);
}