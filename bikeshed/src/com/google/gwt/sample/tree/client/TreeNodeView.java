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
package com.google.gwt.sample.tree.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.sample.tree.shared.TreeNodeModel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * A tree view.
 * 
 * @param <T> the data type of each tree node.
 */
public abstract class TreeNodeView<T,C> extends Composite {
  
  private TreeItem treeItem;
  private ListRegistration listReg;

  @SuppressWarnings("unused")
  public TreeNodeView(T value, final TreeNodeModel<C> childModel, Cell<C> cell) {
    this.treeItem = new TreeItem(value.toString()) {
      @Override
      public void setState(boolean open, boolean fireEvents) {
        super.setState(open, fireEvents);
        if (open) {
          listReg = childModel.getListModel().addListHandler(new ListHandler<C>() {
            public void onDataChanged(ListEvent<C> event) {
              // TODO - handle event start and length
              treeItem.removeItems();

              for (C value : event.getValues()) {
                TreeNodeView<C,?> childView = createChildView(value);
                treeItem.addItem(childView.treeItem);
              }
            }

            public void onSizeChanged(SizeChangeEvent event) {
              int size = event.getSize();
              treeItem.removeItems();
              if (size > 0) {
                treeItem.addItem(""); // placeholder
              }
            }
          });
          listReg.setRangeOfInterest(0, 100);
        } else {
          if (listReg != null) {
            listReg.removeHandler();
            listReg = null;
          }
        }
      }
    };
    if (childModel != null) {
      treeItem.addItem("loading...");
    }
  }

  TreeItem getTreeItem() {
    return treeItem;
  }

  protected abstract TreeNodeView<C, ?> createChildView(C nodeData);
}
