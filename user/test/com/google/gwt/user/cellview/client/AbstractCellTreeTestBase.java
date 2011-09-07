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

import com.google.gwt.user.cellview.client.AbstractHasDataTestBase.IndexCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Base tests for subclasses of {@link AbstractCellTree}.
 */
public abstract class AbstractCellTreeTestBase extends GWTTestCase {

  /**
   * The root value.
   */
  private static final Object ROOT_VALUE = new Object();

  /**
   * A mock {@link TreeViewModel} used for testing. Each child ads a character
   * to the parent string. The longest string in the tree is 4 characters.
   */
  protected class MockTreeViewModel implements TreeViewModel {

    private static final int MAX_DEPTH = 4;

    /**
     * The cell used to render all nodes in the tree.
     */
    private final Cell<String> cell = new TextCell();

    /**
     * The root data provider.
     */
    private final ListDataProvider<String> rootDataProvider = createDataProvider("");

    /**
     * The selection models at each level of the tree.
     */
    private final List<MultiSelectionModel<String>> selectionModels = new ArrayList<MultiSelectionModel<String>>();

    public MockTreeViewModel() {
      for (int i = 0; i < MAX_DEPTH; i++) {
        selectionModels.add(new MultiSelectionModel<String>());
      }
    }

    public <T> NodeInfo<?> getNodeInfo(T value) {
      if (value == ROOT_VALUE) {
        return new DefaultNodeInfo<String>(rootDataProvider, cell,
            selectionModels.get(0), null);
      } else if (value instanceof String) {
        String prefix = (String) value;
        int depth = prefix.length();
        if (depth >= MAX_DEPTH) {
          throw new IllegalStateException("Prefix should never exceed "
              + MAX_DEPTH + " characters.");
        }
        return new DefaultNodeInfo<String>(createDataProvider(prefix), cell,
            selectionModels.get(depth), null);
      }
      throw new IllegalArgumentException("Unrecognized value type");
    }

    public boolean isLeaf(Object value) {
      if (value == ROOT_VALUE) {
        return false;
      } else if (value instanceof String) {
        int depth = ((String) value).length();
        if (depth > MAX_DEPTH) {
          throw new IllegalStateException(
              "value should never exceed five characters.");
        }
        return depth == MAX_DEPTH;
      }
      throw new IllegalArgumentException("Unrecognized value type");
    }

    public ListDataProvider<String> getRootDataProvider() {
      return rootDataProvider;
    }

    /**
     * Get the {@link MultiSelectionModel} for the nodes at the specified depth.
     * 
     * @param depth the depth of the node
     * @return the {@link MultiSelectionModel} at that depth
     */
    public MultiSelectionModel<String> getSelectionModel(int depth) {
      return selectionModels.get(depth);
    }

    /**
     * Create a data provider that extends the prefix by one letter.
     * 
     * @param prefix the prefix string
     * @return a data provider
     */
    private ListDataProvider<String> createDataProvider(String prefix) {
      ListDataProvider<String> provider = new ListDataProvider<String>();
      List<String> list = provider.getList();
      for (int i = 0; i < 10; i++) {
        list.add(prefix + ((char) ('a' + i)));
      }
      provider.flush();
      return provider;
    }
  }

  /**
   * A mock {@link CloseHandler} used for testing.
   */
  private class MockCloseHandler implements CloseHandler<TreeNode> {

    private CloseEvent<TreeNode> lastEvent;

    public CloseEvent<TreeNode> getLastEventAndClear() {
      CloseEvent<TreeNode> toRet = lastEvent;
      lastEvent = null;
      return toRet;
    }

    public void onClose(CloseEvent<TreeNode> event) {
      assertNull(lastEvent);
      this.lastEvent = event;
    }
  }

  /**
   * A mock {@link OpenHandler} used for testing.
   */
  private class MockOpenHandler implements OpenHandler<TreeNode> {

    private OpenEvent<TreeNode> lastEvent;

    public OpenEvent<TreeNode> getLastEventAndClear() {
      OpenEvent<TreeNode> toRet = lastEvent;
      lastEvent = null;
      return toRet;
    }

    public void onOpen(OpenEvent<TreeNode> event) {
      assertNull(lastEvent);
      this.lastEvent = event;
    }
  }

  /**
   * The model that backs the tree.
   */
  protected MockTreeViewModel model;

  /**
   * The current tree being tested.
   */
  protected AbstractCellTree tree;

  /**
   * If true, the tree only supports opening a single path.
   */
  private final boolean singlePathOnly;

  /**
   * Construct a new {@link AbstractCellTreeTestBase}.
   * 
   * @param singlePathOnly true if the tree only supports a single open path
   */
  public AbstractCellTreeTestBase(boolean singlePathOnly) {
    this.singlePathOnly = singlePathOnly;
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.cellview.CellView";
  }

  /**
   * Issue 6677: Deleting the last element on a CellTree causes NPE in IE.
   */
  public void testDeleteLastNode() {
    // Remove all but the last tree node from the model.
    TreeNode root = tree.getRootTreeNode();
    for (int i = 0; i < 9; i++) {
      model.rootDataProvider.getList().remove(0);
    }
    model.rootDataProvider.flush();
    assertEquals(1, root.getChildCount());

    // Remove the last tree node.
    model.rootDataProvider.getList().remove(0);
    model.rootDataProvider.flush();
    assertEquals(0, root.getChildCount());
  }

  public void testGetRootNode() {
    TreeNode root = tree.getRootTreeNode();
    assertEquals(10, root.getChildCount());
    assertEquals(0, root.getIndex());
    assertNull(root.getParent());
    assertEquals(ROOT_VALUE, root.getValue());
    testTreeNode(root, null, 0, ROOT_VALUE, false);
  }

  public void testIsLeaf() {
    assertFalse(tree.isLeaf(ROOT_VALUE));
    assertFalse(tree.isLeaf("a"));
    assertFalse(tree.isLeaf("ab"));
    assertFalse(tree.isLeaf("ab"));
    assertFalse(tree.isLeaf("abc"));
    assertTrue(tree.isLeaf("abcd"));
  }

  /**
   * Test that the correct values are sent to the Cell to be rendered.
   */
  public void testRenderWithKeyProvider() {
    // Create a cell that verifies the render args.
    final List<String> rendered = new ArrayList<String>();
    final IndexCell<String> cell = new IndexCell<String>() {
      @Override
      public void render(Context context, String data, SafeHtmlBuilder sb) {
        super.render(context, data, sb);
        int call = rendered.size();
        rendered.add(data);
        assertTrue("render() called more than thrice", rendered.size() < 4);

        assertEquals(call + "value", data);
        Object key = context.getKey();
        assertTrue(key instanceof Integer);
        assertEquals(call, key);
      }
    };

    // Create a model with only one level, and three values at that level.
    TreeViewModel model = new TreeViewModel() {
      public NodeInfo<?> getNodeInfo(Object value) {
        // The key provider returns the first char as an integer.
        ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
          public Object getKey(String item) {
            return Integer.parseInt(item.substring(0, 1));
          }
        };
        ListDataProvider<String> dataProvider = new ListDataProvider<String>(
            keyProvider);
        dataProvider.getList().add("0value");
        dataProvider.getList().add("1value");
        dataProvider.getList().add("2value");
        return new DefaultNodeInfo<String>(dataProvider, cell);
      }

      public boolean isLeaf(Object value) {
        return value != null;
      }
    };

    // Create a tree.
    createAbstractCellTree(model, null);
    delayTestFinish(5000);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        assertEquals("Cell#render() should be called exactly thrice", 3,
            rendered.size());
        cell.assertLastRenderIndex(2);
        finishTest();
      }
    });
  }

  /**
   * Test that opening a sibling node works.
   */
  public void testOpenSiblingNode() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Open a node.
    TreeNode b = root.setChildOpen(1, true);
    assertEquals(b, openHandler.getLastEventAndClear().getTarget());

    // Open a sibling node.
    TreeNode d = root.setChildOpen(3, true);
    if (singlePathOnly) {
      assertFalse(root.isChildOpen(1));
      assertEquals(b, closeHandler.getLastEventAndClear().getTarget());
    } else {
      assertTrue(root.isChildOpen(1));
      assertNull(closeHandler.getLastEventAndClear());
    }
    assertEquals(d, openHandler.getLastEventAndClear().getTarget());
    assertTrue(root.isChildOpen(3));
  }

  /**
   * Test a {@link TreeNode} at the leaf. We access the leaf nodes with the
   * {@link TreeNode} that is the parent of the leaf nodes.
   */
  public void testTreeNodeAtLeaf() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Walk to a parent of leaf nodes.
    TreeNode b = root.setChildOpen(1, true);
    assertEquals(b, openHandler.getLastEventAndClear().getTarget());
    TreeNode bc = b.setChildOpen(2, true);
    assertEquals(bc, openHandler.getLastEventAndClear().getTarget());
    TreeNode bce = bc.setChildOpen(4, true);
    assertEquals(bce, openHandler.getLastEventAndClear().getTarget());

    // Try to open the leaf.
    assertNull(bce.setChildOpen(0, true));
    assertNull(openHandler.getLastEventAndClear());
    assertNull(openHandler.getLastEventAndClear());

    // Test the values associated with the node.
    testTreeNode(bce, bc, 4, "bce", true);
  }

  /**
   * Test a {@link TreeNode} in the middle of the tree.
   */
  public void testTreeNodeAtMiddle() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Walk to a parent of leaf nodes.
    TreeNode b = root.setChildOpen(1, true);
    assertEquals(b, openHandler.getLastEventAndClear().getTarget());
    TreeNode bc = b.setChildOpen(2, true);
    assertEquals(bc, openHandler.getLastEventAndClear().getTarget());

    // Test the values associated with the node.
    testTreeNode(bc, b, 2, "bc", false);
  }

  /**
   * Test that closing a branch closes all open nodes recursively.
   */
  public void testTreeNodeCloseBranch() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Walk down a branch.
    TreeNode b = root.setChildOpen(1, true);
    assertEquals(b, openHandler.getLastEventAndClear().getTarget());
    TreeNode bc = b.setChildOpen(2, true);
    assertEquals(bc, openHandler.getLastEventAndClear().getTarget());
    TreeNode bce = bc.setChildOpen(4, true);
    assertEquals(bce, openHandler.getLastEventAndClear().getTarget());

    // Close the node at the top of the branch.
    assertNull(root.setChildOpen(1, false));
    assertFalse(root.isChildOpen(1));
    assertTrue(b.isDestroyed());
    assertTrue(bc.isDestroyed());
    assertTrue(bce.isDestroyed());
    assertNull(openHandler.getLastEventAndClear());
    assertEquals(b, closeHandler.getLastEventAndClear().getTarget());
  }

  public void testTreeNodeCloseChild() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler() {
      @Override
      public void onClose(CloseEvent<TreeNode> event) {
        super.onClose(event);

        // The node should be destroyed when the close event is fired.
        TreeNode node = event.getTarget();
        assertTrue(node.isDestroyed());
      }
    };
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Open a node.
    TreeNode child = root.setChildOpen(2, true);
    assertEquals(child, openHandler.getLastEventAndClear().getTarget());
    assertNull(closeHandler.getLastEventAndClear());
    assertTrue(root.isChildOpen(2));
    assertFalse(child.isDestroyed());
    assertEquals("c", child.getValue());
    assertEquals(2, child.getIndex());
    assertEquals(root, child.getParent());

    // Close the child.
    assertNull(root.setChildOpen(2, false));
    assertNull(openHandler.getLastEventAndClear());
    assertEquals(child, closeHandler.getLastEventAndClear().getTarget());
    assertFalse(root.isChildOpen(2));
    assertFalse(root.isDestroyed());
    assertTrue(child.isDestroyed());
  }

  public void testTreeNodeCloseChildAlreadyClosed() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Open a node.
    TreeNode child = root.setChildOpen(2, true);
    assertEquals(child, openHandler.getLastEventAndClear().getTarget());
    assertNull(closeHandler.getLastEventAndClear());
    assertTrue(root.isChildOpen(2));
    assertFalse(child.isDestroyed());
    assertEquals("c", child.getValue());
    assertEquals(2, child.getIndex());
    assertEquals(root, child.getParent());

    // Close the child.
    assertNull(root.setChildOpen(2, false));
    assertNull(openHandler.getLastEventAndClear());
    assertEquals(child, closeHandler.getLastEventAndClear().getTarget());
    assertFalse(root.isChildOpen(2));
    assertFalse(root.isDestroyed());
    assertTrue(child.isDestroyed());

    // Close the child again.
    assertNull(root.setChildOpen(2, false));
    assertNull(openHandler.getLastEventAndClear());
    assertNull(closeHandler.getLastEventAndClear());
    assertFalse(root.isChildOpen(2));
    assertFalse(root.isDestroyed());
    assertTrue(child.isDestroyed());
  }

  /**
   * Test that a tree node is destroyed if its associated data is lost when new
   * data is provided to the node.
   */
  public void testTreeNodeDataLost() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Get a node.
    TreeNode b = root.setChildOpen(1, true);
    assertEquals(b, openHandler.getLastEventAndClear().getTarget());

    // Replace the data without the old node.
    List<String> list = new ArrayList<String>();
    list.add("x");
    list.add("y");
    list.add("z");
    model.rootDataProvider.setList(list);

    // Verify the node is destroyed.
    assertTrue(b.isDestroyed());

    // True to open a new node.
    assertNotNull(root.setChildOpen(0, true));
  }

  /**
   * Test that a tree node continues to exist when new data is pushed to the
   * node.
   */
  public void testTreeNodeDataReplaced() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Get a node.
    TreeNode b = root.setChildOpen(1, true);
    assertEquals(b, openHandler.getLastEventAndClear().getTarget());

    // Replace the data and include the old node at a different location.
    List<String> list = new ArrayList<String>();
    list.add("x");
    list.add("y");
    list.add("b");
    list.add("z");
    model.rootDataProvider.setList(list);

    // Verify the node still exists.
    assertFalse(root.isChildOpen(1));
    assertTrue(root.isChildOpen(2));
    testTreeNode(b, root, 2, "b", false);
  }

  public void testTreeNodeIsDestroyed() {
    TreeNode root = tree.getRootTreeNode();

    // Open a node.
    TreeNode c = root.setChildOpen(2, true);
    assertFalse(c.isDestroyed());

    // Close the node.
    assertNull(root.setChildOpen(2, false));
    assertFalse(root.isDestroyed());
    assertTrue(c.isDestroyed());

    // Verify we can still get the value.
    assertEquals("c", c.getValue());

    try {
      c.getChildCount();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
    try {
      c.getChildValue(0);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
    try {
      c.getIndex();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
    try {
      c.getParent();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
    try {
      c.isChildLeaf(0);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
    try {
      c.setChildOpen(0, true);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
    try {
      c.setChildOpen(0, true, true);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected;
    }
  }

  /**
   * Try to open a child that is already open.
   */
  public void testTreeNodeOpenChildAlreadyOpen() {
    MockOpenHandler openHandler = new MockOpenHandler();
    MockCloseHandler closeHandler = new MockCloseHandler();
    tree.addOpenHandler(openHandler);
    tree.addCloseHandler(closeHandler);
    TreeNode root = tree.getRootTreeNode();

    // Open a node.
    TreeNode child = root.setChildOpen(2, true);
    assertEquals(child, openHandler.getLastEventAndClear().getTarget());
    assertNull(closeHandler.getLastEventAndClear());
    assertTrue(root.isChildOpen(2));
    assertFalse(child.isDestroyed());
    assertEquals("c", child.getValue());
    assertEquals(2, child.getIndex());
    assertEquals(root, child.getParent());

    // Open the same node.
    assertEquals(child, root.setChildOpen(2, true));
    assertNull(openHandler.getLastEventAndClear());
    assertNull(closeHandler.getLastEventAndClear());
    assertTrue(root.isChildOpen(2));
    assertFalse(child.isDestroyed());
  }

  /**
   * Create an {@link AbstractCellTree} to test.
   * 
   * @param <T> the data type of the root value
   * @param model the {@link TreeViewModel} that backs the tree
   * @param rootValue the root value
   * @return a new {@link AbstractCellTree}
   */
  protected abstract <T> AbstractCellTree createAbstractCellTree(
      TreeViewModel model, T rootValue);

  @Override
  protected void gwtSetUp() throws Exception {
    model = new MockTreeViewModel();
    tree = createAbstractCellTree(model, ROOT_VALUE);
    RootPanel.get().add(tree);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    RootPanel.get().remove(tree);
  }

  /**
   * Test the state of a {@link TreeNode}.
   * 
   * @param node the node to test
   * @param parent the expected parent
   * @param index the expected index within the parent
   * @param value the expected value
   * @param isChildLeaf true if the node only contains leaf nodes
   */
  private void testTreeNode(TreeNode node, TreeNode parent, int index,
      Object value, boolean isChildLeaf) {
    assertEquals(10, node.getChildCount());
    assertEquals(index, node.getIndex());
    assertEquals(parent, node.getParent());
    assertEquals(value, node.getValue());

    // Test child values.
    String prefix = (value == ROOT_VALUE) ? "" : value.toString();
    assertEquals(prefix + "a", node.getChildValue(0));
    assertEquals(prefix + "j", node.getChildValue(9));
    for (int i = 0; i < 10; i++) {
      assertEquals(isChildLeaf, node.isChildLeaf(i));
      assertFalse(node.isChildOpen(i));
    }

    // Test children out of range.
    try {
      node.getChildValue(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.getChildValue(10);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.isChildLeaf(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.isChildLeaf(10);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.isChildOpen(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.isChildOpen(10);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.setChildOpen(-1, true);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
    try {
      node.setChildOpen(10, true);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }
}
