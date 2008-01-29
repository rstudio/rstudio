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
 * This interface describes text nodes, as might occur between tags.  These may
 * also be <code>CDATASection</code> nodes.  
 */
public interface Text extends CharacterData  {
  /**
   * Splits the node into two text nodes.  The current node is truncated to 
   * <code>offset</code>, and the new node is inserted as the next sibling.  
   * The new node created is also returned.
   * 
   * @param offset how far from the beginning to start splitting
   * @return new <code>Text</code> node containing the data after <code>offset</code>
   */  
  Text splitText(int offset);

}