/*
 * WorkbenchTabPanel.java
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

package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.theme.ModuleTabLayoutPanel;
import org.rstudio.core.client.theme.WindowFrame;

import java.util.ArrayList;

class WorkbenchTabPanel
      extends Composite 
      implements RequiresResize,
                 ProvidesResize,
                 HasSelectionHandlers<Integer>,
                 HasEnsureVisibleHandlers
{
   public WorkbenchTabPanel(WindowFrame owner)
   {
      tabPanel_ = new ModuleTabLayoutPanel(owner);
      tabPanel_.setSize("100%", "100%");
      tabPanel_.addStyleDependentName("Workbench");
      initWidget(tabPanel_);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      tabPanel_.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>(){
         public void onBeforeSelection(BeforeSelectionEvent<Integer> event)
         {
            if (clearing_)
               return;

            WorkbenchTab tab = tabs_.get(event.getItem().intValue());
            tab.onBeforeSelected();
         }
      });
      tabPanel_.addSelectionHandler(new SelectionHandler<Integer>() {
         public void onSelection(SelectionEvent<Integer> event)
         {
            if (clearing_)
               return;

            WorkbenchTab pane = tabs_.get(event.getSelectedItem().intValue());
            pane.onSelected();
         }
      });

      int selectedIndex = tabPanel_.getSelectedIndex();
      if (selectedIndex >= 0)
      {
         WorkbenchTab tab = tabs_.get(selectedIndex);
         tab.onBeforeSelected();
         tab.onSelected();
      }
   }

   public void setTabs(ArrayList<WorkbenchTab> tabs)
   {
      tabPanel_.clear();
      tabs_.clear();

      for (WorkbenchTab tab : tabs)
         add(tab);
   }

   private void add(WorkbenchTab tab)
   {
      tabs_.add(tab);
      final Widget widget = tab.toWidget();
      tabPanel_.add(widget, tab.getTitle());
      
      tab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            // First ensure that we ourselves are visible
            fireEvent(new EnsureVisibleEvent());

            tabPanel_.selectTab(widget);
         }
      });
   }

   public void selectTab(int tabIndex)
   {
      // deal with migrating from n+1 to n tabs 
      int safeIndex = Math.min(tabIndex, tabs_.size() - 1);
      
      tabPanel_.selectTab(safeIndex);
   }
   
   public void selectTab(WorkbenchTab pane)
   {
      int index = tabs_.indexOf(pane);
      if (index != -1)
         selectTab(index);
      else
      {
         String title = pane.getTitle();
         for (int i = 0; i < tabs_.size(); i++)
         {
            WorkbenchTab tab = tabs_.get(i);
            if (tab.getTitle().equals(title))
            {
               selectTab(i);
               return;
            }
         }
      }
   }

   public int getSelectedIndex()
   {
      return tabPanel_.getSelectedIndex();
   }
   
   public HandlerRegistration addSelectionHandler(
         SelectionHandler<Integer> integerSelectionHandler)
   {
      return tabPanel_.addSelectionHandler(integerSelectionHandler);
   }

   public void onResize()
   {
      Widget w = getWidget();
      if (w instanceof RequiresResize)
         ((RequiresResize)w).onResize();
   }

   public HandlerRegistration addEnsureVisibleHandler(
         EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void clear()
   {
      clearing_ = true;
      tabPanel_.clear();
      tabs_.clear();
      clearing_ = false;
   }

   private ModuleTabLayoutPanel tabPanel_;
   private ArrayList<WorkbenchTab> tabs_ = new ArrayList<WorkbenchTab>();
   private boolean clearing_ = false;
}
