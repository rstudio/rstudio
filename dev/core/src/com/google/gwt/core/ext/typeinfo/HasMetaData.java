/*
 * Copyright 2006 Google Inc.
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

/*
 * IMPLEMENTATION NOTES
 * 
 * This is a useful way to unify various forms of metadata so that clients don't
 * have to be brittle with respect to Java language versions. For example, this
 * mechanism exposes the tag "gwt.typeArgs" in a way that is independent of
 * whether a doc comment was used or (in the future) a concrete instantiation of
 * a generic type was used. The same idea could be useful for to exposing
 * attributes as metadata.
 */
package com.google.gwt.core.ext.typeinfo;

/**
 * Manages doc comment metadata for an AST item. The structure of the metadata
 * attempts to mirror the way in which tags and values were originally declared.
 * 
 * <p>
 * For example, for the following declaration
 * 
 * <pre>
 * /**
 *  * @myTag value1 value2
 *  * @myTag value3 value4
 *  * ... 
 * </pre>
 * 
 * a call to <code>getMetaData("myTag")</code> would return this array of
 * string arrays
 * 
 * <pre>
 *[0][0] = value1
 *[0][1] = value2
 *[1][0] = value3
 *[1][1] = value4
 * </pre>
 * 
 * </p>
 */
public interface HasMetaData {
  /**
   * Adds additional metadata.
   */
  void addMetaData(String tagName, String[] values);

  /**
   * Gets each list of metadata for the specified tag name.
   */
  String[][] getMetaData(String tagName);

  /**
   * Gets the name of available metadata tags.
   */
  String[] getMetaDataTags();
}
