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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.EventReporter;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple tree used to quickly exercise tree behavior.
 */
public class VisualsForTreeEvents extends AbstractIssue {

  @Override
  public Widget createIssue() {
    VerticalPanel p = new VerticalPanel();
    Tree t = VisualsForTree.createTree();
    p.add(t);
    EventReporter<Object, TreeItem> handler = new EventReporter<Object, TreeItem>(
        p);
    t.addKeyboardListener(handler);
    t.addMouseListener(handler);

    t.addSelectionHandler(new SelectionHandler<TreeItem>() {
      public void onSelection(SelectionEvent<TreeItem> event) {
        Window.setTitle("select " + prettyPrint(event.getSelectedItem()));
      }
    });

    t.addCloseHandler(new CloseHandler<TreeItem>() {

      public void onClose(CloseEvent<TreeItem> event) {
        Window.setTitle("close " + prettyPrint(event.getTarget()));
      }

    });

    t.addOpenHandler(new OpenHandler<TreeItem>() {

      public void onOpen(OpenEvent<TreeItem> event) {
        Window.setTitle("open " + prettyPrint(event.getTarget()));
      }

    });

    return p;
  }

  @Override
  public String getInstructions() {
    return "Open each node, ensure you see the right events in the window title";
  }

  @Override
  public String getSummary() {
    return "Event Tree tests";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private String prettyPrint(TreeItem item) {
    return item.getText();
  }

}
