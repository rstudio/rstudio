/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.client.impl;

import junit.framework.TestCase;

import javax.validation.Path.Node;

/**
 * Tests for {@link NodeImpl}.
 */
public class NodeImplTest extends TestCase {

  public void testFoo() throws Exception {
    assertNode(NodeImpl.createNode("foo"), "foo", false, null, null);
  }

  public void testFoo_iterable() throws Exception {
    assertNode(NodeImpl.createIterableNode("foo"), "foo", true, null, null);
  }

  public void testFoo1() throws Exception {
    assertNode(NodeImpl.createIndexedNode("foo", 1), "foo", true, null,
        Integer.valueOf(1));
  }

  public void testFooBar() throws Exception {
    assertNode(NodeImpl.createKeyedNode("foo", "bar"), "foo", true, "bar", null);
  }

  public void testRoot() throws Exception {
    assertNode(NodeImpl.ROOT_NODE, null, false, null, null);
  }

  protected void assertNode(Node node, String expectedName,
      boolean expectedInIterator, Object expectedKey, Integer expectedIndex) {
    assertEquals(node + " name", expectedName, node.getName());
    assertEquals(node + " isInIterator", expectedInIterator,
        node.isInIterable());
    assertEquals(node + " key", expectedKey, node.getKey());
    assertEquals(node + " index", expectedIndex, node.getIndex());
  }
}
