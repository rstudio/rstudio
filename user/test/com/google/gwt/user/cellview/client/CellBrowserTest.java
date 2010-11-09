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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.CellBrowser.BrowserCellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;

/**
 * Tests for {@link CellBrowser}.
 */
public class CellBrowserTest extends AbstractCellTreeTestBase {

  /**
   * A {@link TreeViewModel} used for testing. Every other top level node is a
   * leaf node. The 0th top level node has children.
   */
  protected class MixedTreeViewModel implements TreeViewModel {

    public <T> NodeInfo<?> getNodeInfo(T value) {
      if (value == null) {
        // Get the children of root, which are Integers.
        ListDataProvider<Number> provider = new ListDataProvider<Number>();
        for (int i = 0; i < 10; i++) {
          provider.getList().add(new Integer(i));
        }
        return new DefaultNodeInfo<Number>(provider, new NumberCell());
      } else if (value instanceof Integer && !isLeaf(value)) {
        // Get the children of odd Integers, which are Strings.
        ListDataProvider<String> provider = new ListDataProvider<String>();
        return new DefaultNodeInfo<String>(provider, new TextCell());
      }
      throw new IllegalArgumentException("Unexpected value: " + value);
    }

    public boolean isLeaf(Object value) {
      if (value == null) {
        // Root value is null.
        return false;
      } else if (value instanceof Integer) {
        // Odd integers are leaf nodes
        return ((Integer) value % 2) != 0;
      }
      return false;
    }
  }

  public CellBrowserTest() {
    super(true);
  }

  /**
   * Verify that closing a leaf node sets the focused key to null.
   */
  public void testCloseLeafNode() {
    CellBrowser browser = new CellBrowser(new MixedTreeViewModel(), null);
    TreeNode rootNode = browser.getRootTreeNode();
    assertEquals(1, browser.treeNodes.size());

    // Open a leaf node.
    assertNull(rootNode.setChildOpen(1, true));
    assertEquals(1, browser.treeNodes.size());
    assertEquals(1, browser.treeNodes.get(0).getFocusedKey());
    assertFalse(browser.treeNodes.get(0).isFocusedOpen());

    // Close the leaf node.
    rootNode.setChildOpen(1, false);
    assertEquals(1, browser.treeNodes.size());
    assertFalse(browser.treeNodes.get(0).isFocusedOpen());
  }

  /**
   * Verify that opening a leaf node closes other open nodes.
   */
  public void testOpenLeafNode() {
    CellBrowser browser = new CellBrowser(new MixedTreeViewModel(), null);
    TreeNode rootNode = browser.getRootTreeNode();
    assertEquals(1, browser.treeNodes.size());

    // Child 0 has children.
    TreeNode child0 = rootNode.setChildOpen(0, true);
    assertNotNull(child0);
    assertFalse(child0.isDestroyed());
    assertEquals(2, browser.treeNodes.size());
    assertEquals(0, browser.treeNodes.get(0).getFocusedKey());
    assertTrue(browser.treeNodes.get(0).isFocusedOpen());

    // Child 1 is a leaf.
    TreeNode child1 = rootNode.setChildOpen(1, true);
    assertNull(child1);
    assertTrue(child0.isDestroyed());
    assertEquals(1, browser.treeNodes.size());
    assertEquals(1, browser.treeNodes.get(0).getFocusedKey());
    assertFalse(browser.treeNodes.get(0).isFocusedOpen());

    // Child 2 has children.
    TreeNode child2 = rootNode.setChildOpen(2, true);
    assertNotNull(child2);
    assertFalse(child2.isDestroyed());
    assertEquals(2, browser.treeNodes.size());
    assertEquals(2, browser.treeNodes.get(0).getFocusedKey());
    assertTrue(browser.treeNodes.get(0).isFocusedOpen());
  }

  public void testSetKeyboardSelectionPolicyDisabled() {
    CellBrowser browser = (CellBrowser) tree;

    // Disable keyboard selection.
    browser.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    assertEquals(KeyboardSelectionPolicy.DISABLED,
        browser.getKeyboardSelectionPolicy());

    // Verify that keyboard selection is enabled in the lists.
    BrowserCellList<?> list = browser.treeNodes.get(0).getDisplay();
    assertEquals(KeyboardSelectionPolicy.ENABLED,
        list.getKeyboardSelectionPolicy());
    assertTrue(list.isKeyboardNavigationSuppressed());
  }

  @Override
  protected <T> CellBrowser createAbstractCellTree(TreeViewModel model,
      T rootValue) {
    CellBrowser browser = new CellBrowser(model, rootValue);
    browser.setHeight("500px");
    return browser;
  }
}
