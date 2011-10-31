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
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
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

    private final SelectionModel<Object> selectionModel;

    public MixedTreeViewModel() {
      this(null);
    }

    public MixedTreeViewModel(SelectionModel<Object> selectionModel) {
      this.selectionModel = selectionModel;
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
      if (value == null) {
        // Get the children of root, which are Integers.
        ListDataProvider<Number> provider = new ListDataProvider<Number>();
        for (int i = 0; i < 10; i++) {
          provider.getList().add(new Integer(i));
        }
        return new DefaultNodeInfo<Number>(provider, new NumberCell(), selectionModel, null);
      } else if (value instanceof Integer && !isLeaf(value)) {
        // Get the children of odd Integers, which are Strings.
        ListDataProvider<String> provider = new ListDataProvider<String>();
        for (int i = 0; i < 10; i++) {
          char c = (char) ('a' + i);
          provider.getList().add("" + c);
        }
        return new DefaultNodeInfo<String>(provider, new TextCell(), selectionModel, null);
      }
      throw new IllegalArgumentException("Unexpected value: " + value);
    }

    @Override
    public boolean isLeaf(Object value) {
      if (value == null) {
        // Root value is null.
        return false;
      } else if (value instanceof Integer) {
        // Odd integers are leaf nodes
        return ((Integer) value % 2) != 0;
      } else if (value instanceof String) {
        return true;
      }
      return false;
    }
  }

  public CellBrowserTest() {
    super(true);
  }

  /**
   * Test that the CellBrowser correctly updates a shared SelectionModel if it
   * is bound to selection.
   */
  public void testBoundToSharedSelectionModel() {
    SingleSelectionModel<Object> selectionModel = new SingleSelectionModel<Object>();
    CellBrowser browser = new CellBrowser(new MixedTreeViewModel(selectionModel), null);
    browser.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);

    // Select a leaf.
    browser.treeNodes.get(0).getDisplay().getPresenter().setKeyboardSelectedRow(1, true, false);
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected(1));

    // Select another leaf.
    browser.treeNodes.get(0).getDisplay().getPresenter().setKeyboardSelectedRow(3, true, false);
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected(3));

    // Select a non-leaf.
    browser.treeNodes.get(0).getDisplay().getPresenter().setKeyboardSelectedRow(2, true, false);
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected(2));

    // Select a leaf in the child list.
    browser.treeNodes.get(1).getDisplay().getPresenter().setKeyboardSelectedRow(5, true, false);
    browser.treeNodes.get(1).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected("f"));

    // Verify that flushing the selection model doesn't change the selection
    // back to the parent list.
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected("f"));

    // Verify that redrawing the parent list doesn't change the selection back
    // to the parent list.
    browser.treeNodes.get(0).getDisplay().redraw();
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected("f"));

    // Select a non-leaf in the parent.
    browser.treeNodes.get(0).getDisplay().getPresenter().setKeyboardSelectedRow(4, true, false);
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected(4));

    // isSelected() triggers the final SelectionChangeEvent. Verify the end
    // state is still correct.
    browser.treeNodes.get(0).getDisplay().getPresenter().flush();
    assertTrue(selectionModel.isSelected(4));
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

  /**
   * Test that even when keyboard selection is bound to the selection model, we
   * do not automatically select items in child lists until the child list is
   * actually touched.
   */
  public void testSetKeyboardSelectionPolicyBound() {
    CellBrowser browser = (CellBrowser) tree;

    // Bind keyboard selection to the selection model.
    browser.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
    assertEquals(KeyboardSelectionPolicy.BOUND_TO_SELECTION, browser.getKeyboardSelectionPolicy());

    // Select an item at depth 0. Nothing should be selected at depth 1.
    BrowserCellList<?> list0 = browser.treeNodes.get(0).getDisplay();
    list0.getPresenter().setKeyboardSelectedRow(1, false, false);
    list0.getPresenter().flush();
    browser.treeNodes.get(1).getDisplay().getPresenter().flush();
    assertEquals(1, model.getSelectionModel(0).getSelectedSet().size());
    assertEquals(0, model.getSelectionModel(1).getSelectedSet().size());

    // Select an item at depth 1. Nothing should be selected at depth 2.
    BrowserCellList<?> list1 = browser.treeNodes.get(1).getDisplay();
    list1.getPresenter().setKeyboardSelectedRow(2, false, false);
    list1.getPresenter().flush();
    browser.treeNodes.get(2).getDisplay().getPresenter().flush();
    assertEquals(1, model.getSelectionModel(0).getSelectedSet().size());
    assertEquals(1, model.getSelectionModel(1).getSelectedSet().size());
    assertEquals(0, model.getSelectionModel(2).getSelectedSet().size());
  }

  public void testSetKeyboardSelectionPolicyDisabled() {
    CellBrowser browser = (CellBrowser) tree;

    // Disable keyboard selection.
    browser.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    assertEquals(KeyboardSelectionPolicy.DISABLED, browser.getKeyboardSelectionPolicy());

    // Verify that keyboard selection is enabled in the lists.
    BrowserCellList<?> list = browser.treeNodes.get(0).getDisplay();
    assertEquals(KeyboardSelectionPolicy.ENABLED, list.getKeyboardSelectionPolicy());
    assertTrue(list.isKeyboardNavigationSuppressed());
  }

  @Override
  protected <T> CellBrowser createAbstractCellTree(TreeViewModel model, T rootValue) {
    CellBrowser browser = new CellBrowser(model, rootValue);
    browser.setHeight("500px");
    return browser;
  }
}
