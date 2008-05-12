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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
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
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwTreeDescription();

    String cwTreeDynamicLabel();

    String cwTreeItem();

    String cwTreeName();

    String cwTreeStaticLabel();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwTree(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwTreeDescription();
  }

  @Override
  public String getName() {
    return constants.cwTreeName();
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
      TreeItem item = dynamicTree.addItem(constants.cwTreeItem() + " " + i);

      // Temporarily add an item so we can expand this node
      item.addItem("");
    }

    // Add a listener that automatically generates some children
    dynamicTree.addTreeListener(new TreeListener() {
      public void onTreeItemSelected(TreeItem item) {
      }

      public void onTreeItemStateChanged(TreeItem item) {
        if (item.getState() && item.getChildCount() == 1) {
          // Close the item immediately
          item.setState(false, false);

          // Add a random number of children to the item
          String itemText = item.getText();
          int numChildren = Random.nextInt(5) + 2;
          for (int i = 0; i < numChildren; i++) {
            TreeItem child = item.addItem(itemText + "." + i);
            child.addItem("");
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
    Tree staticTree = new Tree();

    // Add some of Beethoven's music
    TreeItem c1 = staticTree.addItem("Beethoven");
    TreeItem c1g1 = c1.addItem("Concertos");
    c1g1.addItem("No. 1 - C");
    c1g1.addItem("No. 2 - B-Flat Major");
    c1g1.addItem("No. 3 - C Minor");
    c1g1.addItem("No. 4 - G Major");
    c1g1.addItem("No. 5 - E-Flat Major");
    TreeItem c1g2 = c1.addItem("Quartets");
    c1g2.addItem("Six String Quartets");
    c1g2.addItem("Three String Quartets");
    c1g2.addItem("Grosse Fugue for String Quartets");
    TreeItem c1g3 = c1.addItem("Sonatas");
    c1g3.addItem("Sonata in A Minor");
    c1g3.addItem("Sonata in F Major");
    TreeItem c1g4 = c1.addItem("Symphonies");
    c1g4.addItem("No. 2 - D Major");
    c1g4.addItem("No. 2 - D Major");
    c1g4.addItem("No. 3 - E-Flat Major");
    c1g4.addItem("No. 4 - B-Flat Major");
    c1g4.addItem("No. 5 - C Minor");
    c1g4.addItem("No. 6 - F Major");
    c1g4.addItem("No. 7 - A Major");
    c1g4.addItem("No. 8 - F Major");
    c1g4.addItem("No. 9 - D Minor");

    // Add some of Brahms's music
    TreeItem c2 = staticTree.addItem("Brahms");
    TreeItem c2g1 = c2.addItem("Concertos");
    c2g1.addItem("Violin Concerto");
    c2g1.addItem("Double Concerto - A Minor");
    c2g1.addItem("Piano Concerto No. 1 - D Minor");
    c2g1.addItem("Piano Concerto No. 2 - B-Flat Major");
    TreeItem c2g2 = c2.addItem("Quartets");
    c2g2.addItem("Piano Quartet No. 1 - G Minor");
    c2g2.addItem("Piano Quartet No. 2 - A Major");
    c2g2.addItem("Piano Quartet No. 3 - C Minor");
    c2g2.addItem("String Quartet No. 3 - B-Flat Minor");
    TreeItem c2g3 = c2.addItem("Sonatas");
    c2g3.addItem("Two Sonatas for Clarinet - F Minor");
    c2g3.addItem("Two Sonatas for Clarinet - E-Flat Major");
    TreeItem c2g4 = c2.addItem("Symphonies");
    c2g4.addItem("No. 1 - C Minor");
    c2g4.addItem("No. 2 - D Minor");
    c2g4.addItem("No. 3 - F Major");
    c2g4.addItem("No. 4 - E Minor");

    // Add some of Mozart's music
    TreeItem c3 = staticTree.addItem("Mozart");
    TreeItem c3g1 = c3.addItem("Concertos");
    c3g1.addItem("Piano Concerto No. 12");
    c3g1.addItem("Piano Concerto No. 17");
    c3g1.addItem("Clarinet Concerto");
    c3g1.addItem("Violin Concerto No. 5");
    c3g1.addItem("Violin Concerto No. 4");

    // Return the tree
    return staticTree;
  }
}
