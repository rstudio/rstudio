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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CellTree}.
 */
public class CellTreeTest extends AbstractCellTreeTestBase {

  public CellTreeTest() {
    super(false);
  }

  public void testRefreshEmptyNode() {
    // An empty data provider.
    final ListDataProvider<String> provider = new ListDataProvider<String>();
    TreeViewModel model = new TreeViewModel() {
      @Override
      public NodeInfo<?> getNodeInfo(Object value) {
        TextCell cell = new TextCell();
        return new DefaultNodeInfo<String>(provider, cell);
      }

      @Override
      public boolean isLeaf(Object value) {
        return false;
      }
    };

    // Create the tree.
    CellTree tree = createAbstractCellTree(model, null);
    tree.rootNode.listView.presenter.flush();

    // Refresh the empty list.
    provider.refresh();
    provider.flush();
    tree.rootNode.listView.presenter.flush();
  }

  /**
   * Test that CellTree handles rendering the same content, but with a different
   * underlying value.
   */
  public void testRenderSameContent() {
    final AbstractCell<Integer> intCell = new AbstractCell<Integer>() {
      @Override
      public void render(Context context, Integer value, SafeHtmlBuilder sb) {
        sb.append(value % 10); // Render the units digit only.
      }
    };

    // Create a data provider for the root node.
    final ListDataProvider<Integer> root = new ListDataProvider<Integer>();
    for (int i = 0; i < 9; i++) {
      root.getList().add(i);
    }

    TreeViewModel model = new TreeViewModel() {
      @Override
      public NodeInfo<?> getNodeInfo(Object value) {
        if (value == null) {
          // Return the root node.
          return new DefaultNodeInfo<Integer>(root, intCell);
        } else {
          // Return a child node.
          return new DefaultNodeInfo<String>(new ListDataProvider<String>(), new TextCell());
        }
      }

      @Override
      public boolean isLeaf(Object value) {
        return false;
      }
    };

    CellTree tree = createAbstractCellTree(model, null);
    RootPanel.get().add(tree);
    tree.rootNode.listView.presenter.flush();

    // Open the first child.
    TreeNode rootNode = tree.getRootTreeNode();
    assertEquals(1, rootNode.getChildValue(1));
    TreeNode child1 = rootNode.setChildOpen(1, true);
    assertFalse(child1.isDestroyed());
    assertTrue(rootNode.isChildOpen(1));

    // Replace all values in the list.
    List<Integer> oldData = root.getList();
    List<Integer> newData = new ArrayList<Integer>();
    for (int l : oldData) {
      newData.add(l + 100); // renders the same as the current value.
    }
    root.setList(newData);
    root.flush();
    tree.rootNode.listView.presenter.flush();

    // Child1 is closed and destroyed.
    assertFalse(rootNode.isChildOpen(1));
    assertTrue(child1.isDestroyed());

    RootPanel.get().remove(tree);
  }

  /**
   * Test that replacing a subset of children updates both the TreeNode value
   * and the underlying DOM correctly.
   */
  public void testReplaceChildren() {
    CellTree cellTree = (CellTree) tree;
    TreeNode root = cellTree.getRootTreeNode();

    // Open a couple of child nodes.
    TreeNode a = root.setChildOpen(0, true);
    TreeNode b = root.setChildOpen(1, true);
    assertEquals("a", a.getValue());
    assertEquals("ab", a.getChildValue(1));
    assertEquals("b", b.getValue());
    assertEquals("bc", b.getChildValue(2));

    // Replace "b" with a "new" value.
    model.getRootDataProvider().getList().set(1, "new");
    model.getRootDataProvider().flush();
    assertFalse(a.isDestroyed());
    assertTrue(b.isDestroyed());
    TreeNode newNode = root.setChildOpen(1, true);
    assertEquals("a", a.getValue());
    assertEquals("ab", a.getChildValue(1));
    assertEquals("new", newNode.getValue());
    assertEquals("newc", newNode.getChildValue(2));

    // Check the underlying DOM values.
    CellTreeNodeView<?> aImpl = cellTree.rootNode.getChildNode(0);
    CellTreeNodeView<?> newNodeImpl = cellTree.rootNode.getChildNode(1);
    assertEquals("a", aImpl.getCellParent().getInnerText());
    assertEquals(10, aImpl.ensureChildContainer().getChildCount());
    assertEquals("new", newNodeImpl.getCellParent().getInnerText());
  }

  public void testSetDefaultNodeSize() {
    CellTree cellTree = (CellTree) tree;
    TreeNode root = cellTree.getRootTreeNode();
    assertEquals(10, root.getChildCount());

    TreeNode b = root.setChildOpen(1, true);
    assertEquals(10, b.getChildCount());

    // Change the default size.
    cellTree.setDefaultNodeSize(5);
    assertEquals(5, cellTree.getDefaultNodeSize());
    assertEquals(10, b.getChildCount());
    TreeNode d = root.setChildOpen(3, true);
    assertEquals(5, d.getChildCount());
  }

  @Override
  protected <T> CellTree createAbstractCellTree(TreeViewModel model, T rootValue) {
    return new CellTree(model, rootValue);
  }
}
