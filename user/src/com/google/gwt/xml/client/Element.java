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
 * Implementation notes: Internet Explorer does not support any of the namespace
 * methods, so xxxNS is not supported for all xxx. Safari does not support
 * Attribute node modification; use <code>setAttribute</code> instead.
 * 
 */

/**
 * This interface represents XML DOM elements, which are the basic building
 * block of XML. An example follows:
 * 
 * <pre>
 *   <sample my_attribute="one">
 *      Some text<child/> more text
 *   </sample>
 * </pre>
 */
public interface Element extends Node {
  /**
   * This method retrieves the attribute which has a name of <code>name</code>.
   * 
   * @param name the name of the attribute to get the value of
   * @return the value of the attribute specified by <code>name</code>
   */
  String getAttribute(String name);

  /**
   * This method retrieves the attribute node which has a name of
   * <code>name</code>. This <code>Attr</code> will have the same value as
   * would be gotten with <code>getAttribute</code>.
   * 
   * @param name the name of the <code>Attr</code> to get
   * @return the attribute node of this <code>Element</code>which has a name
   *         of <code>name</code>
   */
  Attr getAttributeNode(String name);

  /**
   * This method retrieves the elements by tag name which has a name of
   * <code>name</code>.
   * 
   * @param name the name of the <code>Element</code> to get
   * @return the elements by tag name of this <code>Element</code> which has a
   *         name of <code>name</code>
   */
  NodeList getElementsByTagName(String name);

  /**
   * This method retrieves the tag name.
   * 
   * @return the tag name of this <code>Element</code>
   */
  String getTagName();

  /**
   * This method determines whether this <code>Element</code> has an attribute
   * with the supplied name.
   * 
   * @param name the name of the attribute
   * @return <code>true</code> if this <code>Element</code> has an attribute
   *         that name.
   */
  boolean hasAttribute(String name);

  /**
   * This method removes the attribute which has the specified name.
   * 
   * @param name the name of the attribute to remove
   */
  void removeAttribute(String name);

  /**
   * This method sets the attribute specified by <code>name</code> to
   * <code>value</code>.
   * 
   * @param name the name of the attribute to set
   * @param value the new value this attribute is to have
   */
  void setAttribute(String name, String value);

}