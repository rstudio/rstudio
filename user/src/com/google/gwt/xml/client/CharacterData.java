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
 * Implementation notes: Opera has a length limit of 32k on any <code>CharacterData</code>
 * nodes.
 */

/**
 * This interface describes <code>CharacterData</code> XML nodes. These can be
 * either <code>Text</code>, <code>CDATASection</code> or
 * <code>Comment</code> nodes.
 */
public interface CharacterData extends Node {
  /**
   * This method appends <code>data</code> to the data in this
   * <code>CharacterData</code>.
   * 
   * @param appendedData the data to be appended to the end
   */
  void appendData(String appendedData);

  /**
   * This method deletes data, starting at <code>offset</code>, and deleting
   * <code>count</code> characters.
   * 
   * @param offset how far from the beginning to start deleting
   * @param count how many characters to delete
   */
  void deleteData(int offset, int count);

  /**
   * This method retrieves the data.
   * 
   * @return the data of this <code>CharacterData</code>
   */
  String getData();

  /**
   * This method retrieves the length of the data.
   * 
   * @return the length of the data contained in this <code>CharacterData</code>
   */
  int getLength();

  /**
   * This method inserts data at the specified offset.
   * 
   * @param offset how far from the beginning to start inserting
   * @param insertedData the data to be inserted
   */
  void insertData(int offset, String insertedData);

  /**
   * This method replaces the substring of data indicated by <code>offset</code>
   * and <code>count</code> with <code>replacementData</code>.
   * 
   * @param offset how far from the beginning to start the replacement
   * @param replacementData the data that will replace the deleted data
   * @param count how many characters to delete before inserting
   *          <code>replacementData</code>
   */
  void replaceData(int offset, int count, String replacementData);

  /**
   * This method sets the data to <code>data</code>.
   * 
   * @param data the new data
   */
  void setData(String data);

  /**
   * This method gets a substring of the character data.
   * 
   * @param offset the place to start the substring
   * @param count how many characters to return
   * @return the specified substring
   */
  String substringData(int offset, int count);

}