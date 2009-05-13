/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Text node tests.
 */
public class TextTest extends GWTTestCase {

  private static final String FOLDIN_LEFT = "fold";
  private static final String FOLDIN_RIGHT = "in";
  private static final String FOLDIN_BIG = "fold mad magazine in";
  private static final String FOLDIN_SMALL = "foldin";
  private static final String FOLDIN_HYPHENATED = "fold-in";
  private static final String FOLDIN_MIDDLE = " mad magazine ";

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * Test that getting and setting data works.
   */
  public void testDataRoundTrip() {
    Text text = Document.get().createTextNode("initial");
    assertEquals("initial", text.getData());

    text.setData("replaced");
    assertEquals("replaced", text.getData());
  }

  /**
   * Test {@link Text#getLength()}.
   */
  public void testLength() {
    Text text = Document.get().createTextNode("initial");
    assertEquals("initial".length(), text.getLength());
  }

  /**
   * Test that deleting, inserting, and replacing data works.
   */
  public void testInsertAndDeleteData() {
    Text text = Document.get().createTextNode(FOLDIN_BIG);

    text.deleteData(FOLDIN_LEFT.length(), FOLDIN_MIDDLE.length());
    assertEquals(FOLDIN_SMALL, text.getData());

    text.insertData(FOLDIN_LEFT.length(), FOLDIN_MIDDLE);
    assertEquals(FOLDIN_BIG, text.getData());

    text.replaceData(FOLDIN_LEFT.length(), FOLDIN_MIDDLE.length(), "-");
    assertEquals(FOLDIN_HYPHENATED, text.getData());
  }

  /**
   * Test {@link Text#splitText(int)}.
   */
  public void testSplitText() {
    Document doc = Document.get();
    DivElement div = doc.createDivElement();
    Text leftNode = doc.createTextNode(FOLDIN_SMALL);
    div.appendChild(leftNode);

    Text rightNode = leftNode.splitText(FOLDIN_LEFT.length());
    assertEquals(FOLDIN_LEFT, leftNode.getData());
    assertEquals(FOLDIN_RIGHT, rightNode.getData());
    assertEquals(div, leftNode.getParentNode());
    assertEquals(div, rightNode.getParentNode());
    assertEquals(leftNode, div.getFirstChild());
    assertEquals(rightNode, leftNode.getNextSibling());
  }
}
