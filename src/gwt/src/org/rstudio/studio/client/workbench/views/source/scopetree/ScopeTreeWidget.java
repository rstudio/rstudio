/*
 * ScopeTreeWidget.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.scopetree;

import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class ScopeTreeWidget extends Composite
{
   public ScopeTreeWidget(JsArray<Scope> scopeTree)
   {
      scopeTree_ = scopeTree;
      containerPanel_ = new FlowPanel();
      tree_ = new Tree();
      
      containerPanel_.add(tree_);
      
      init();
      initWidget(containerPanel_);
   }
   
   private void init()
   {
      for (int i = 0; i < scopeTree_.length(); i++)
         addItem(scopeTree_.get(i));
   }
   
   private void addItem(Scope node)
   {
      TreeItem item = new TreeItem();
      item.setText(node.getLabel());
      tree_.addItem(item);
   }
   
   private final JsArray<Scope> scopeTree_;
   private final FlowPanel containerPanel_;
   private final Tree tree_;
}
