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
package com.google.gwt.sample.showcase.client.content.lists;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".gwt-Tree")
public class CwTree extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String[] cwTreeBeethovenWorkConcertos();

    String[] cwTreeBeethovenWorkQuartets();

    String[] cwTreeBeethovenWorkSonatas();

    String[] cwTreeBeethovenWorkSymphonies();

    String[] cwTreeBrahmsWorkConcertos();

    String[] cwTreeBrahmsWorkQuartets();

    String[] cwTreeBrahmsWorkSonatas();

    String[] cwTreeBrahmsWorkSymphonies();

    String[] cwTreeComposers();

    String cwTreeConcertos();

    String cwTreeDescription();

    String cwTreeDynamicLabel();

    String cwTreeItem();

    String[] cwTreeMozartWorkConcertos();

    String cwTreeName();

    String cwTreeQuartets();

    String cwTreeSonatas();

    String cwTreeStaticLabel();

    String cwTreeSymphonies();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwTree(CwConstants constants) {
    super(constants.cwTreeName(), constants.cwTreeDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a static tree and a container to hold it
    Tree staticTree = createStaticTree();
    staticTree.setAnimationEnabled(true);
    staticTree.ensureDebugId("cwTree-staticTree");
    ScrollPanel staticTreeWrapper = new ScrollPanel(staticTree);
    staticTreeWrapper.ensureDebugId("cwTree-staticTree-Wrapper");
    staticTreeWrapper.setSize("300px", "300px");

    // Wrap the static tree in a DecoratorPanel
    DecoratorPanel staticDecorator = new DecoratorPanel();
    staticDecorator.setWidget(staticTreeWrapper);

    // Create a dynamically generated tree and a container to hold it
    final Tree dynamicTree = createDynamicTree();
    dynamicTree.ensureDebugId("cwTree-dynamicTree");
    ScrollPanel dynamicTreeWrapper = new ScrollPanel(dynamicTree);
    dynamicTreeWrapper.ensureDebugId("cwTree-dynamicTree-Wrapper");
    dynamicTreeWrapper.setSize("300px", "300px");

    // Wrap the dynamic tree in a DecoratorPanel
    DecoratorPanel dynamicDecorator = new DecoratorPanel();
    dynamicDecorator.setWidget(dynamicTreeWrapper);

    // Combine trees onto the page
    Grid grid = new Grid(2, 3);
    grid.setCellPadding(2);
    grid.getRowFormatter().setVerticalAlign(1, HasVerticalAlignment.ALIGN_TOP);
    grid.setHTML(0, 0, constants.cwTreeStaticLabel());
    grid.setHTML(0, 1, "&nbsp;&nbsp;&nbsp;");
    grid.setHTML(0, 2, constants.cwTreeDynamicLabel());
    grid.setWidget(1, 0, staticDecorator);
    grid.setHTML(1, 1, "&nbsp;&nbsp;&nbsp;");
    grid.setWidget(1, 2, dynamicDecorator);

    // Wrap the trees in DecoratorPanels
    return grid;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwTree.class, new RunAsyncCallback() {

      @Override
      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      @Override
      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  /**
   * Add a new section of music created by a specific composer.
   *
   * @param parent the parent {@link TreeItem} where the section will be added
   * @param label the label of the new section of music
   * @param composerWorks an array of works created by the composer
   */
  @ShowcaseSource
  private void addMusicSection(
      TreeItem parent, String label, String[] composerWorks) {
    TreeItem section = parent.addTextItem(label);
    for (String work : composerWorks) {
      section.addTextItem(work);
    }
  }

  /**
   * Create a dynamic tree that will add a random number of children to each
   * node as it is clicked.
   *
   * @return the new tree
   */
  @ShowcaseSource
  private Tree createDynamicTree() {
    // Create a new tree
    Tree dynamicTree = new Tree();

    // Add some default tree items
    for (int i = 0; i < 5; i++) {
      TreeItem item = dynamicTree.addTextItem(constants.cwTreeItem() + " " + i);

      // Temporarily add an item so we can expand this node
      item.addTextItem("");
    }

    // Add a handler that automatically generates some children
    dynamicTree.addOpenHandler(new OpenHandler<TreeItem>() {
      @Override
      public void onOpen(OpenEvent<TreeItem> event) {
        TreeItem item = event.getTarget();
        if (item.getChildCount() == 1) {
          // Close the item immediately
          item.setState(false, false);

          // Add a random number of children to the item
          String itemText = item.getText();
          int numChildren = Random.nextInt(5) + 2;
          for (int i = 0; i < numChildren; i++) {
            TreeItem child = item.addTextItem(itemText + "." + i);
            child.addTextItem("");
          }

          // Remove the temporary item when we finish loading
          item.getChild(0).remove();

          // Reopen the item
          item.setState(true, false);
        }
      }
    });

    // Return the tree
    return dynamicTree;
  }

  /**
   * Create a static tree with some data in it.
   *
   * @return the new tree
   */
  @ShowcaseSource
  private Tree createStaticTree() {
    // Create the tree
    String[] composers = constants.cwTreeComposers();
    String concertosLabel = constants.cwTreeConcertos();
    String quartetsLabel = constants.cwTreeQuartets();
    String sonatasLabel = constants.cwTreeSonatas();
    String symphoniesLabel = constants.cwTreeSymphonies();
    Tree staticTree = new Tree();

    // Add some of Beethoven's music
    TreeItem beethovenItem = staticTree.addTextItem(composers[0]);
    addMusicSection(beethovenItem, concertosLabel,
        constants.cwTreeBeethovenWorkConcertos());
    addMusicSection(
        beethovenItem, quartetsLabel, constants.cwTreeBeethovenWorkQuartets());
    addMusicSection(
        beethovenItem, sonatasLabel, constants.cwTreeBeethovenWorkSonatas());
    addMusicSection(beethovenItem, symphoniesLabel,
        constants.cwTreeBeethovenWorkSymphonies());

    // Add some of Brahms's music
    TreeItem brahmsItem = staticTree.addTextItem(composers[1]);
    addMusicSection(
        brahmsItem, concertosLabel, constants.cwTreeBrahmsWorkConcertos());
    addMusicSection(
        brahmsItem, quartetsLabel, constants.cwTreeBrahmsWorkQuartets());
    addMusicSection(
        brahmsItem, sonatasLabel, constants.cwTreeBrahmsWorkSonatas());
    addMusicSection(
        brahmsItem, symphoniesLabel, constants.cwTreeBrahmsWorkSymphonies());

    // Add some of Mozart's music
    TreeItem mozartItem = staticTree.addTextItem(composers[2]);
    addMusicSection(
        mozartItem, concertosLabel, constants.cwTreeMozartWorkConcertos());

    // Return the tree
    return staticTree;
  }
}
