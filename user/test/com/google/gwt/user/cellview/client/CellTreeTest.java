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
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  public void testAriaSelectedAndExpanded() {
    CellTree cellTree = (CellTree) tree;
    TreeNode root = cellTree.getRootTreeNode();

    TreeNode newNode = root.setChildOpen(1, true);
    cellTree.rootNode.getChildNode(1).setSelected(true);
    model.getRootDataProvider().refresh();
    model.getRootDataProvider().flush();
    root.setChildOpen(1, true);
    CellTreeNodeView<?> newNodeImpl = cellTree.rootNode.getChildNode(1);
    assertEquals("true", newNodeImpl.getElement().getAttribute("aria-selected"));
    // Check aria-expanded on open
    assertEquals("true", newNodeImpl.getElement().getAttribute("aria-expanded"));

    // Check aria-expanded on close
    root.setChildOpen(1, false);
    newNodeImpl = cellTree.rootNode.getChildNode(1);
    assertEquals("false", newNodeImpl.getElement().getAttribute("aria-expanded"));

    cellTree.rootNode.getChildNode(1).setSelected(false);
    model.getRootDataProvider().refresh();
    model.getRootDataProvider().flush();
    root.setChildOpen(1, true);
    newNodeImpl = cellTree.rootNode.getChildNode(1);
    assertEquals("false", newNodeImpl.getElement().getAttribute("aria-selected"));
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

  public void testAriaAttributes() {
    CellTree cellTree = (CellTree) tree;
    TreeNode rootNode = cellTree.getRootTreeNode();

    // Open a child node.
    TreeNode aNode = rootNode.setChildOpen(0, true);

    // Check role="tree"
    assertEquals("tree", cellTree.rootNode.getElement().getAttribute("role"));

    // Check 1st level
    CellTreeNodeView<?> aView = cellTree.rootNode.getChildNode(0);
    CellTreeNodeView<?> bView = cellTree.rootNode.getChildNode(1);

    // Check treeitem role, level, posinset, and setsize
    assertEquals("treeitem", aView.getElement().getAttribute("role"));
    assertEquals("1", aView.getElement().getAttribute("aria-level"));
    assertEquals("1", aView.getElement().getAttribute("aria-posinset"));
    assertEquals("10", aView.getElement().getAttribute("aria-setsize"));

    assertEquals("treeitem", bView.getElement().getAttribute("role"));
    assertEquals("1", bView.getElement().getAttribute("aria-level"));
    assertEquals("2", bView.getElement().getAttribute("aria-posinset"));
    assertEquals("10", bView.getElement().getAttribute("aria-setsize"));

    // Check 2nd level
    assertTrue(aNode.getChildCount() != 0);
    assertEquals(aNode, aView.getTreeNode());
    assertTrue(aView.getChildCount() != 0);
    CellTreeNodeView<?> aViewChild = aView.getChildNode(0);

    // Check treeitem role, level, posinset, and setsize
    assertEquals("treeitem", aViewChild.getElement().getAttribute("role"));
    assertEquals("2", aViewChild.getElement().getAttribute("aria-level"));
    assertEquals("1", aViewChild.getElement().getAttribute("aria-posinset"));
    assertEquals("10", aViewChild.getElement().getAttribute("aria-setsize"));

    // Check aria-expanded
    assertEquals("true", aView.getElement().getAttribute("aria-expanded"));
    assertEquals("false", aViewChild.getElement().getAttribute("aria-expanded"));
    while (!aNode.isChildLeaf(0)) {
      aNode = aNode.setChildOpen(0, true);
      aView = aView.getChildNode(0);
    }
    assertEquals(aNode, aView.getTreeNode());
    assertEquals("", aView.getChildNode(0).getElement().getAttribute("aria-expanded"));

    // Change default size and check aria-setsize and aria-posinset
    cellTree.setDefaultNodeSize(5);
    TreeNode cNode = rootNode.setChildOpen(3, true);
    CellTreeNodeView<?> cView = cellTree.rootNode.getChildNode(3);
    assertEquals(5, cNode.getChildCount());

    CellTreeNodeView<?> cViewChildFirst = cView.getChildNode(0);
    assertEquals("1", cViewChildFirst.getElement().getAttribute("aria-posinset"));
    assertEquals("5", cViewChildFirst.getElement().getAttribute("aria-setsize"));

    CellTreeNodeView<?> cViewChildLast = cView.getChildNode(cView.getChildCount() - 1);
    assertEquals("5", cViewChildLast.getElement().getAttribute("aria-posinset"));
    assertEquals("5", cViewChildLast.getElement().getAttribute("aria-setsize"));
  }

  public void testExplicitKeyboardSelection() {
    CellTree cellTree = (CellTree) tree;
    TreeNode root = cellTree.getRootTreeNode();

    final String cellTreeKeyboardSelectedItemStyleName =
        cellTree.getStyle().cellTreeKeyboardSelectedItem();

    // Navigate through tree to level 4 nodes.
    TreeNode l1Node = root.setChildOpen(0, true); // a
    CellTreeNodeView<?> l1View = cellTree.rootNode.getChildNode(0);
    TreeNode l2Node = l1Node.setChildOpen(0, true); // aa
    CellTreeNodeView<?> l2View = l1View.getChildNode(0);
    TreeNode l3Node = l2Node.setChildOpen(0, true); // aaa
    CellTreeNodeView<?> l3View = l2View.getChildNode(0);

    ((CellTreeNodeView.TreeNodeImpl) l3Node).flush();
    CellTreeNodeView<?> l4View = l3View.getChildNode(0);
    // l4Node is leaf, cannot get a reference via setChildOpen except via non-public nodeView
    TreeNode l4Node = l4View.getTreeNode(); // aaaa

    // Set keyboard selected node to a subtree node, l3Node = first child of l2Node
    cellTree.setKeyboardSelectedTreeNode(l2Node, 0, true);
    assertEquals(l3View, cellTree.getKeyboardSelectedNode());
    assertEquals(l3Node, cellTree.getKeyboardSelectedTreeNode());
    assertTrue(CellTreeNodeView.getSelectionElement(l3View.getElement()).getClassName()
        .indexOf(cellTreeKeyboardSelectedItemStyleName) >= 0);

    // Set keyboard selected node to a leaf node.
    cellTree.setKeyboardSelectedTreeNode(l3Node, 0, true);
    assertEquals(l4View, cellTree.getKeyboardSelectedNode());
    assertEquals(l4Node, cellTree.getKeyboardSelectedTreeNode());
    assertTrue(CellTreeNodeView.getSelectionElement(l4View.getElement()).getClassName()
        .indexOf(cellTreeKeyboardSelectedItemStyleName) >= 0);

    l1Node.setChildOpen(0, false); // close l2node
    ((CellTreeNodeView.TreeNodeImpl) l1Node).flush();

    // Try to select a leaf node in closed subtree
    try {
      cellTree.setKeyboardSelectedTreeNode(l3Node, 0, true);
      fail("should have thrown");
    } catch (IllegalStateException e) {
      assertEquals("TreeNode no longer exists.", e.getMessage());
    }

    // Try to select a subtree node in closed subtree
    try {
      cellTree.setKeyboardSelectedTreeNode(l2Node, 0, true);
      fail("should have thrown");
    } catch (IllegalStateException e) {
      assertEquals("TreeNode no longer exists.", e.getMessage());
    }

    // Still ok to select closed subtree node
    cellTree.setKeyboardSelectedTreeNode(l1Node, 0, true);

    // Create another tree of same structure
    CellTree anotherTree = createAbstractCellTree(model, root.getValue());
    l1Node = anotherTree.getRootTreeNode().setChildOpen(0, true); // a
    ((CellTreeNodeView.TreeNodeImpl) l1Node).flush();

    // Now l1Node refers to a subtree node in anotherTree
    // Select in the same cell tree is ok
    anotherTree.setKeyboardSelectedTreeNode(l1Node, 0, true);

    // Select in a different tree will throw exception.
    try {
      cellTree.setKeyboardSelectedTreeNode(l1Node, 0, true);
      fail("should have thrown");
    } catch (IllegalArgumentException e) {
      assertEquals("The tree node does not belong to the tree.", e.getMessage());
    }
  }

  public void testKeyboardNavigationForUpAndDownKeys() {
    CellTree cellTree = (CellTree) tree;
    TreeNode root = cellTree.getRootTreeNode();

    // Open nodes: a and aj
    TreeNode l1Node = root.setChildOpen(0, true); // a
    assertEquals("a", l1Node.getValue());
    TreeNode l2Node = l1Node.setChildOpen(9, true); // aj
    assertEquals("aj", l2Node.getValue());
    // To force tree structure changes (internal flush()).
    assertEquals(10, l2Node.getChildCount());

    // CellTree structure with node 'a' and 'aj' opened.
    List<String> expectedNavigationPath = Arrays.asList("a",
        "aa", "ab", "ac", "ad", "ae", "af", "ag", "ah", "ai", "aj",
        "aja", "ajb", "ajc", "ajd", "aje", "ajf", "ajg", "ajh", "aji", "ajj",
        "b", "c", "d", "e", "f", "g", "h", "i", "j");

    // Default KeyboardSelected is at "a"
    assertEquals("a", cellTree.getKeyboardSelectedTreeNode().getValue());

    int steps = expectedNavigationPath.size() - 1;
    assertEquals(expectedNavigationPath, repeatKeyInTree(cellTree, KeyCodes.KEY_DOWN, steps));

    Collections.reverse(expectedNavigationPath);
    assertEquals(expectedNavigationPath, repeatKeyInTree(cellTree, KeyCodes.KEY_UP, steps));
  }

  /**
   * Repeats a keystroke in the CellTree that contains string values.
   * @return the nodes selected before starting and after each keystroke.
   */
  private List<String> repeatKeyInTree(CellTree cellTree, int keyCode, int keyCount) {
    List<String> values = new ArrayList<String>();
    values.add((String) cellTree.getKeyboardSelectedTreeNode().getValue());
    for (int i = 0; i < keyCount; i++) {
      cellTree.handleKeyNavigation(keyCode);
      values.add((String) cellTree.getKeyboardSelectedTreeNode().getValue());
    }
    return values;
  }

  @Override
  protected <T> CellTree createAbstractCellTree(TreeViewModel model, T rootValue) {
    return new CellTree(model, rootValue);
  }
}
