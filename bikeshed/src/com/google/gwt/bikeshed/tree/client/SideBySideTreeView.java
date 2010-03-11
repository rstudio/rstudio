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
package com.google.gwt.bikeshed.tree.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * A view of a tree.
 */
public class SideBySideTreeView extends TreeView {

  protected int columnHeight = 200;

  protected int columnWidth = 100;

  /**
   * Construct a new {@link TreeView}.
   *
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   * @param columnWidth
   * @param columnHeight
   */
  public <T> SideBySideTreeView(TreeViewModel viewModel, T rootValue,
      int columnWidth, int columnHeight) {
    super(viewModel);

    this.columnWidth = columnWidth;
    this.columnHeight = columnHeight;

    Element rootElement = Document.get().createDivElement();
    rootElement.setClassName("gwt-sstree");
    Style style = rootElement.getStyle();
    style.setPosition(Position.RELATIVE);
    style.setWidth(columnWidth, Unit.PX);
    style.setHeight(columnHeight, Unit.PX);
    setElement(rootElement);

    // Add event handlers.
    sinkEvents(Event.ONCLICK | Event.ONMOUSEDOWN | Event.ONMOUSEUP);

    // Associate a view with the item.
    TreeNodeView<T> root = new SideBySideTreeNodeView<T>(this, null, null,
        rootElement, rootValue, 0, "gwt-sstree", columnWidth, columnHeight);
    setRootNode(root);
    root.setState(true);
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    int eventType = DOM.eventGetType(event);
    switch (eventType) {
      case Event.ONMOUSEUP:
        Element currentTarget = event.getCurrentEventTarget().cast();
        if (currentTarget == getElement()) {
          Element target = event.getEventTarget().cast();
          String id = target.getId();
          boolean inCell = target.getClassName().equals("gwt-sstree-cell");
          while (id == null || !id.startsWith("gwt-sstree")) {
            target = target.getParentElement();
            if (target == null) {
              return;
            }

            id = target.getId();
            inCell |= target.getClassName().equals("gwt-sstree-cell");
          }

          if (id.startsWith("gwt-sstree-")) {
            id = id.substring(11);
            String[] path = id.split("-");

            TreeNodeView<?> nodeView = getRootTreeNodeView();
            for (String s : path) {
              nodeView = nodeView.getChildTreeNodeView(Integer.parseInt(s));
            }
            if (inCell) {
              nodeView.fireEventToCell(event);
            } else {
              nodeView.setState(!nodeView.getState());
            }
          }
        }
        break;
    }
  }
}
