/*
 * Copyright 2012 Google Inc.
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

package com.google.gwt.user.server.rpc.impl;

import junit.framework.TestCase;

/**
 * Tests {@link ServerSerializationStreamWriter}.
 */
public class ServerSerializationStreamWriterTest extends TestCase {

  public void testEscapeString() {
    // Ensure that when using escapeString, a large string is not split into
    // separate nodes like escapeStringSplitNodes does.
    int nodeLength = 0xFFFF * 2;
    StringBuilder firstNodeBuilder = new StringBuilder(nodeLength);
    for (int i = 0; i < nodeLength; i++) {
      firstNodeBuilder.append('1');
    }

    String escaped = ServerSerializationStreamWriter.escapeString(
        firstNodeBuilder.toString());

    assertEquals("\"" + firstNodeBuilder.toString() + "\"", escaped);
  }

  public void testEscapeStringSplitNodes() {
    String escaped = ServerSerializationStreamWriter.escapeStringSplitNodes("test");
    assertEquals("\"test\"", escaped);
  }

  public void testEscapeStringSplitNodes_unicodeEscape() {
    String escaped = ServerSerializationStreamWriter.escapeStringSplitNodes(
        "测试"  // Unicode characters
        + "\"" // JS quote char
        + "\\" // JS escape char
        + '\u2011' // Unicode non-breaking hyphen char
        + (char) 0x8); // Combining spacing mark
    assertEquals(
        "\""
        + "测试" // Unicode characters
        + "\\\"" // JS quote char
        + "\\\\" // JS escape char
        + "\\u2011" // Unicode non-breaking hyphen char
        + "\\b" // Combining spacing mark
        + "\"",
        escaped);
  }

  public void testEscapeStringSplitNodes_over64KB() {
    // String node length limit is 64KB.
    int firstNodeLength = 0xFFFF;
    StringBuilder firstNodeBuilder = new StringBuilder(firstNodeLength);
    for (int i = 0; i < firstNodeLength - 5; i++) {
      firstNodeBuilder.append('1');
    }

    int secondNodeLength = 0xFF;
    StringBuilder secondNodeBuilder = new StringBuilder(secondNodeLength);
    for (int i = 0; i < secondNodeLength; i++) {
      secondNodeBuilder.append('2');
    }

    String escaped = ServerSerializationStreamWriter.escapeStringSplitNodes(
        firstNodeBuilder.toString() + secondNodeBuilder.toString());

    assertEquals(
        "\"" + firstNodeBuilder.toString() + "\"+\"" + secondNodeBuilder.toString() + "\"",
        escaped);
  }

  public void testEscapeStringSplitNodes_over64KBEscaped() {
    // Fill the entire 64KB string, but leave 6 characters for an escaped unicode character added
    // below.
    int firstNodeLength = 0xFFFF;
    StringBuilder firstNodeBuilder = new StringBuilder(firstNodeLength);
    for (int i = 0; i < firstNodeLength - 6; i++) {
      firstNodeBuilder.append('y');
    }
    String firstNodeNoUnicode = firstNodeBuilder.toString();

    // Add a unicode character on the boundary, this should be the last character added to the first
    // node.
    firstNodeBuilder.append('\u2011');

    int secondNodeLength = 0xFF;
    StringBuilder secondNodeBuilder = new StringBuilder(secondNodeLength);
    for (int i = 0; i < secondNodeLength; i++) {
      secondNodeBuilder.append('z');
    }
    String secondNode = secondNodeBuilder.toString();

    String escaped = ServerSerializationStreamWriter.escapeStringSplitNodes(
        firstNodeBuilder.toString() + secondNode);

    assertEquals(
        "\"" + firstNodeNoUnicode + "\\u2011" // first node (including escaped unicode character)
        + "\"+\""
        + secondNode + "\"",  // second node
        escaped);
  }

}
