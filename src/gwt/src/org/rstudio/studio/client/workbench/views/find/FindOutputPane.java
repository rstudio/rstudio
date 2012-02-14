/*
 * FindOutputPane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.find;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class FindOutputPane extends WorkbenchPane
      implements FindOutputPresenter.Display, HasSelectionHandlers<CodeNavigationTarget>
{
   public FindOutputPane()
   {
      super("Find Results");
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      LayoutPanel panel = new LayoutPanel();
      panel.getElement().getStyle().setBackgroundColor("#F4F6F7");

      context_ = new FindResultContext();

      FindOutputCellTreeResources resources = GWT.create(
                                             FindOutputCellTreeResources.class);

      treeViewModel_ = new FindTreeViewModel(context_,
                                             resources.cellTreeStyle().lineNumber());
      cellTree_ = new CellTree(treeViewModel_, (Object)null,
                               resources);
      cellTree_.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.BOUND_TO_SELECTION);

      treeViewModel_.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            Object o = treeViewModel_.getSelectionModel().getSelectedObject();
            SelectionEvent.fire(FindOutputPane.this, toCodeNavigationTarget(o));
         }
      });

      panel.add(cellTree_);
      final int PAD = 6;
      panel.setWidgetTopBottom(cellTree_, 30, Style.Unit.PX, PAD, Style.Unit.PX);
      panel.setWidgetLeftRight(cellTree_,
                               PAD,
                               Style.Unit.PX,
                               PAD,
                               Style.Unit.PX);

      return panel;
   }

   private CodeNavigationTarget toCodeNavigationTarget(Object o)
   {
      if (o == null)
         return null;
      if (o instanceof FindResultContext.File)
      {
         String path = ((FindResultContext.File)o).getPath();
         return new CodeNavigationTarget(path, null);
      }
      else if (o instanceof FindResultContext.Match)
      {
         FindResultContext.Match match = (FindResultContext.Match)o;
         String path = match.getParent().getPath();
         return new CodeNavigationTarget(
               path, FilePosition.create(match.getLine(), 1));
      }
      else
      {
         assert false;
         return null;
      }
   }

   @Override
   public void addMatch(String path, int line, int column, String value)
   {
      context_.getFile(path).addMatch(line, column, value);
   }

   @Override
   public void clearMatches()
   {
      context_.reset();
   }

   @Override
   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   @Override
   public HandlerRegistration addSelectionHandler(SelectionHandler<CodeNavigationTarget> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   private CellTree cellTree_;
   private FindTreeViewModel treeViewModel_;
   private FindResultContext context_;
}
